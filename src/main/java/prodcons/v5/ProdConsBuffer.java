package prodcons.v5;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProdConsBuffer implements IProdConsBuffer {
    private final Message[] buf;
    private int in = 0, out = 0, count = 0;
    private int totalProduced = 0;

    private final ReentrantLock lock = new ReentrantLock(true); // équitable
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final int expectedTotal;

    public ProdConsBuffer(int capacity, int expectedTotal) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        if (expectedTotal < 0)
            throw new IllegalArgumentException("expectedTotal < 0");
        this.buf = new Message[capacity];
        this.expectedTotal = expectedTotal;
    }

    private boolean finished() {
        return totalProduced >= expectedTotal; // « fermé » dès que tout a été produit
    }

    @Override
    public void put(Message m) throws InterruptedException {
        lock.lock();
        try {
            while (count == buf.length)
                notFull.await();
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;
            notEmpty.signalAll(); // réveille les consommateurs
            if (finished())
                notEmpty.signalAll(); // s'assure de lever les derniers wait
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0 && !finished())
                notEmpty.await();
            if (count == 0 && finished())
                return null; // fin propre
            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;
            notFull.signal();
            return m;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message[] get(int k) throws InterruptedException {
        if (k <= 0)
            throw new IllegalArgumentException("k <= 0");
        lock.lock();
        try {
            ArrayList<Message> batch = new ArrayList<>(k);
            while (batch.size() < k) {
                while (count == 0 && !finished())
                    notEmpty.await();
                if (count == 0 && finished())
                    break; // rendre partiel/0
                while (count > 0 && batch.size() < k) { // drainer FIFO
                    Message m = buf[out];
                    buf[out] = null;
                    out = (out + 1) % buf.length;
                    count--;
                    batch.add(m);
                }
                notFull.signalAll();
                if (batch.size() < k) {
                    if (finished())
                        break;
                    notEmpty.await();
                }
            }
            return batch.toArray(new Message[0]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int nmsg() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int totmsg() {
        lock.lock();
        try {
            return totalProduced;
        } finally {
            lock.unlock();
        }
    }
}
