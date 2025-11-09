package prodcons.v5;

import java.util.concurrent.atomic.AtomicInteger;

public class Consumer extends Thread {
    private final IProdConsBuffer buffer;
    private final int consTimeMs;
    private final int k;
    private final AtomicInteger consumed;

    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs, int k, AtomicInteger consumed) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
        this.k = k;
        this.consumed = consumed;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Message[] batch = ((ProdConsBuffer) buffer).get(k);
                if (batch.length == 0) {
                    // fin de production et tampon vidé
                    return;
                }
                consumed.addAndGet(batch.length);
                Thread.sleep(consTimeMs);
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
