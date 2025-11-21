package prodcons.v7;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producteur de tâches.
 *
 * Chaque producteur génère un certain nombre de messages (quota).
 * Chaque message contient une tâche (Runnable) qui sera exécutée par un
 * consommateur.
 */
public class Producer extends Thread {

    private static final AtomicInteger GEN = new AtomicInteger(0);

    private final ProdConsBuffer buffer;
    private final int quota;
    private final int prodTimeMs;
    private final int pid;

    public Producer(int pid, ProdConsBuffer buffer, int quota, int prodTimeMs) {
        super("P-" + pid);
        this.pid=pid;
        this.buffer = buffer;
        this.quota = quota;
        this.prodTimeMs = prodTimeMs;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < quota; i++) {
                Thread.sleep(prodTimeMs);

                int id = GEN.incrementAndGet();
                long tid = getId();

                // La tâche : ici, simple affichage avec l'identité du thread consommateur
                Runnable task = () -> {
                    String threadName = Thread.currentThread().getName();
                    System.out.printf("[TASK] %s exécute la tâche %d produite par P-%d%n",
                            threadName, id, pid);
                };

                Message m = new Message(id, tid, task);
                buffer.put(m);

                System.out.printf("[PROD] %s a déposé %s (nmsg=%d, tot=%d)%n",
                        getName(), m, buffer.nmsg(), buffer.totmsg());
            }
        } catch (InterruptedException e) {
            System.out.printf("[PROD] %s interrompu%n", getName());
            Thread.currentThread().interrupt();
        } finally {
            try {
                buffer.producerDone();
            } catch (Throwable t) {
            }
        }
    }
}
