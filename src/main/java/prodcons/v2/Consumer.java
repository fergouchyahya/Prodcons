package prodcons.v2;

import java.util.concurrent.atomic.AtomicInteger;

public class Consumer extends Thread {
    private final IProdConsBuffer buffer;
    private final int consTimeMs;
    private final AtomicInteger consumed; // compteur global partagé

    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs, AtomicInteger consumed) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
        this.consumed = consumed;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Message m = buffer.get();
                consumed.incrementAndGet(); // 1 message consommé
                Thread.sleep(consTimeMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
