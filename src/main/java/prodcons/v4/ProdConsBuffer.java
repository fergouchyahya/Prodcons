package prodcons.v4;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ProdConsBuffer implements IProdConsBuffer {
    private final Message[] buf;
    private int in = 0, out = 0, count = 0;
    private int totalProduced = 0;

    // Lock équitable pour limiter la famine
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
    }

    @Override
    public void put(Message m) throws InterruptedException {
        lock.lock();
        try {
            while (count == buf.length) {
                notFull.await(); // attendre de la place
            }
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;
            // il y a au moins 1 message dispo
            notEmpty.signal(); // un seul consommateur suffit
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // attendre un message
            }
            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;
            // il y a au moins 1 place libre
            notFull.signal(); // réveiller un producteur
            return m;
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
