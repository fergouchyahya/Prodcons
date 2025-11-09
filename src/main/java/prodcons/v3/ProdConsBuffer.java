package prodcons.v3;

import java.util.concurrent.Semaphore;

public class ProdConsBuffer implements IProdConsBuffer {
    private final Message[] buf;
    private int in = 0, out = 0, count = 0;
    private int totalProduced = 0;

    // Sémaphores
    private final Semaphore empty; // nb de cases libres
    private final Semaphore full; // nb de messages prêts
    private final Semaphore mutex; // exclusion mutuelle

    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
        this.empty = new Semaphore(capacity, true);
        this.full = new Semaphore(0, true);
        this.mutex = new Semaphore(1, true);
    }

    @Override
    public void put(Message m) throws InterruptedException {
        empty.acquire(); // attendre une case libre
        mutex.acquire(); // entrer en section critique
        try {
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;
        } finally {
            mutex.release();
        }
        full.release(); // signaler un message dispo
    }

    @Override
    public Message get() throws InterruptedException {
        full.acquire(); // attendre un message
        mutex.acquire(); // entrer en section critique
        try {
            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;
            return m;
        } finally {
            mutex.release();
            empty.release(); // libérer une case
        }
    }

    @Override
    public int nmsg() { // lecture
        try {
            mutex.acquire();
            return count;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return count;
        } finally {
            mutex.release();
        }
    }

    @Override
    public int totmsg() {
        try {
            mutex.acquire();
            return totalProduced;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return totalProduced;
        } finally {
            mutex.release();
        }
    }
}
