package prodcons.v7;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tampon borné pour messages contenant des tâches.
 *
 * Implémentation classique producteur-consommateur :
 * - buffer circulaire
 * - ReentrantLock équitable
 * - conditions notFull / notEmpty
 */
public class ProdConsBuffer implements IProdConsBuffer {

    private final Message[] buf;
    private int in = 0;
    private int out = 0;
    private int count = 0;
    private int totalProduced = 0;

    private int producersRemaining = 0;
    private boolean closed = false;

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
    }

    @Override
    public void setProducersCount(int n) {
        if (n < 0)
            throw new IllegalArgumentException("n < 0");
        lock.lock();
        try {
            this.producersRemaining = n;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void producerDone() {
        lock.lock();
        try {
            if (producersRemaining > 0) {
                producersRemaining--;
                if (producersRemaining == 0) {
                    closed = true;
                    notEmpty.signalAll();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        lock.lock();
        try {
            return closed;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Message m) throws InterruptedException {
        lock.lock();
        try {
            while (count == buf.length) {
                notFull.await();
            }
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0 && !closed) {
                notEmpty.await();
            }

            if (count == 0 && closed) {
                return null;
            }

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

/*
 * Ancienne version (conservée en commentaire)
 * package prodcons.v7;
 * 
 * import java.util.concurrent.locks.Condition;
 * import java.util.concurrent.locks.ReentrantLock;
 * 
 * /**
 * Tampon borné pour messages contenant des tâches.
 *
 * Implémentation classique producteur-consommateur :
 * - buffer circulaire
 * - ReentrantLock équitable
 * - conditions notFull / notEmpty
 */
/*
 * public class ProdConsBuffer implements IProdConsBuffer {
 * 
 * private final Message[] buf;
 * private int in = 0;
 * private int out = 0;
 * private int count = 0;
 * private int totalProduced = 0;
 * 
 * private final ReentrantLock lock = new ReentrantLock(true);
 * private final Condition notFull = lock.newCondition();
 * private final Condition notEmpty = lock.newCondition();
 * 
 * public ProdConsBuffer(int capacity) {
 * if (capacity <= 0)
 * throw new IllegalArgumentException("capacity <= 0");
 * this.buf = new Message[capacity];
 * }
 * 
 * @Override
 * public void put(Message m) throws InterruptedException {
 * lock.lock();
 * try {
 * while (count == buf.length) {
 * notFull.await();
 * }
 * buf[in] = m;
 * in = (in + 1) % buf.length;
 * count++;
 * totalProduced++;
 * notEmpty.signal();
 * } finally {
 * lock.unlock();
 * }
 * }
 * 
 * @Override
 * public Message get() throws InterruptedException {
 * lock.lock();
 * try {
 * while (count == 0) {
 * notEmpty.await();
 * }
 * Message m = buf[out];
 * buf[out] = null;
 * out = (out + 1) % buf.length;
 * count--;
 * notFull.signal();
 * return m;
 * } finally {
 * lock.unlock();
 * }
 * }
 * 
 * @Override
 * public int nmsg() {
 * lock.lock();
 * try {
 * return count;
 * } finally {
 * lock.unlock();
 * }
 * }
 * 
 * @Override
 * public int totmsg() {
 * lock.lock();
 * try {
 * return totalProduced;
 * } finally {
 * lock.unlock();
 * }
 * }
 * }
 */
