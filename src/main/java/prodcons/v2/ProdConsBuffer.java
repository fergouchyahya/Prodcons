package prodcons.v2;

public class ProdConsBuffer implements IProdConsBuffer {
    private final Message[] buf;
    private int in = 0, out = 0, count = 0;
    private int totalProduced = 0;

    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
    }

    @Override
    public synchronized void put(Message m) throws InterruptedException {
        while (count == buf.length)
            wait();
        buf[in] = m;
        in = (in + 1) % buf.length;
        count++;
        totalProduced++;
        notifyAll();
    }

    @Override
    public synchronized Message get() throws InterruptedException {
        while (count == 0)
            wait();
        Message m = buf[out];
        buf[out] = null;
        out = (out + 1) % buf.length;
        count--;
        notifyAll();
        return m;
    }

    @Override
    public synchronized int nmsg() {
        return count;
    }

    @Override
    public synchronized int totmsg() {
        return totalProduced;
    }
}
