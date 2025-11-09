package prodcons.v1;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer extends Thread {
    private static final AtomicInteger GEN = new AtomicInteger(0);
    private final IProdConsBuffer buffer;
    private final int minProd;
    private final int maxProd;
    private final int prodTimeMs;

    public Producer(IProdConsBuffer buffer, int minProd, int maxProd, int prodTimeMs) {
        this.buffer = buffer;
        this.minProd = minProd;
        this.maxProd = maxProd;
        this.prodTimeMs = prodTimeMs;
        setName("Producer -" + getId());
    }

    @Override
    public void run() {
        int n = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);
        for (int i = 0; i < n; i++) {
            try {
                Thread.sleep(prodTimeMs); // temps de production
                int id = GEN.incrementAndGet();
                Message m = new Message(id, getId());
                buffer.put(m);
                Log.info("%s put %-6s  (nmsg=%d, tot=%d)", getName(), m, buffer.nmsg(), buffer.totmsg());
                // System.out.println(getName() + " put " + id + " nmsg=" + buffer.nmsg());
            } catch (InterruptedException e) {
                Log.info("%s interrupted", getName());
                return;
            }
        }
        Log.info("%s finished producing", getName());
    }
}
