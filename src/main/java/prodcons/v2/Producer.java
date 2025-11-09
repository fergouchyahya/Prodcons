package prodcons.v2;

import java.util.concurrent.atomic.AtomicInteger;

public class Producer extends Thread {
    private static final AtomicInteger GEN = new AtomicInteger(0);

    private final IProdConsBuffer buffer;
    private final int quota; // ← quota fixé à l’avance
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
                Message m = new Message(id, getId());
                buffer.put(m);
                Log.info("%s put %-6s  (nmsg=%d, tot=%d)", getName(), m, buffer.nmsg(), buffer.totmsg());

            } catch (InterruptedException e) {
                Log.info("%s interrupted", getName());
                return;
            }
        }
        Log.info("%s finished producing", getName());
    }
}
