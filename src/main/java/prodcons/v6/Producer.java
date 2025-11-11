package prodcons.v6;

import java.util.concurrent.atomic.AtomicInteger;

public class Producer extends Thread {
    private static final AtomicInteger GEN = new AtomicInteger(0);

    private final IProdConsBuffer buffer;
    private final int quota; // nombre de GROUPES à produire
    private final int nCopies; // exemplaires par groupe (>= 1)
    private final int prodTimeMs; // temps de prod entre deux groupes

    public Producer(int pid, IProdConsBuffer buffer, int quota, int nCopies, int prodTimeMs) {
        super("P-" + pid);
        if (nCopies <= 0)
            throw new IllegalArgumentException("nCopies <= 0");
        this.buffer = buffer;
        this.quota = quota;
        this.nCopies = nCopies;
        this.prodTimeMs = prodTimeMs;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < quota; i++) {
                Thread.sleep(prodTimeMs);
                int id = GEN.incrementAndGet();
                long tid = Thread.currentThread().getId(); // évite toute API douteuse
                buffer.put(new Message(id, tid), nCopies); // v6: dépôt synchrone de n copies
            }
        } catch (InterruptedException ignored) {
            // sortie douce
        }
    }
}
