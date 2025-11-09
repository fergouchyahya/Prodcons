package prodcons.v4;

import java.util.concurrent.atomic.AtomicInteger;

public class Producer extends Thread {
    private static final AtomicInteger GEN = new AtomicInteger(0);
    private final IProdConsBuffer buffer;
    private final int quota;
    private final int prodTimeMs;

    public Producer(int pid, IProdConsBuffer buffer, int quota, int prodTimeMs) {
        super("P-" + pid);
        this.buffer = buffer;
        this.quota = quota;
        this.prodTimeMs = prodTimeMs;
    }

    @Override
    public void run() {
        for (int i = 0; i < quota; i++) {
            try {
                Thread.sleep(prodTimeMs);
                int id = GEN.incrementAndGet();
                buffer.put(new Message(id, getId()));
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
