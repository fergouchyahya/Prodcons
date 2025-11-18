package prodcons.v7;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test pour TaskExecutor : les producteurs déposent des tâches (un Runnable
 * par message) et le TaskExecutor gère automatiquement les workers.
 */
public class TestTaskExecutor {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (InputStream in = TestTaskExecutor.class.getResourceAsStream("/prodcons/options.xml")) {
            if (in == null)
                throw new IllegalStateException("prodcons/options.xml introuvable sur le classpath");
            props.loadFromXML(in);
        }

        int nProd = Integer.parseInt(props.getProperty("nProd"));
        int bufSz = Integer.parseInt(props.getProperty("bufSz"));
        int prodT = Integer.parseInt(props.getProperty("prodTime"));
        int consT = Integer.parseInt(props.getProperty("consTime"));
        int minProd = Integer.parseInt(props.getProperty("minProd"));
        int maxProd = Integer.parseInt(props.getProperty("maxProd"));
        int maxWorkers = Integer.parseInt(props.getProperty("nCons"));

        int[] quotas = new int[nProd];
        int totalMessages = 0;
        for (int i = 0; i < nProd; i++) {
            int q = ThreadLocalRandom.current().nextInt(minProd,
                    Math.max(minProd, Math.min(maxProd, minProd + 10)) + 1);
            // limiter pour tests rapides : au moins minProd, pas trop large
            quotas[i] = q;
            totalMessages += q;
        }

        System.out.println("==================================================");
        System.out.println("[TEST TaskExecutor] Simulation de dépôt de tâches");
        System.out.printf("  nProd   = %d%n", nProd);
        System.out.printf("  bufSz   = %d%n", bufSz);
        System.out.printf("  prodT   = %d ms%n", prodT);
        System.out.printf("  consT   = %d ms%n", consT);
        System.out.printf("  maxWorkers = %d%n", maxWorkers);
        System.out.printf("  total tasks (approx) = %d%n", totalMessages);
        System.out.println("==================================================");

        final TaskExecutor executor = new TaskExecutor(bufSz, maxWorkers);

        List<Thread> producers = new ArrayList<>();

        for (int i = 0; i < nProd; i++) {
            final int idx = i;
            final int quota = quotas[i];
            Thread p = new Thread(() -> {
                try {
                    for (int j = 0; j < quota; j++) {
                        final int tid = idx + 1;
                        final int msgId = j + 1;
                        Runnable task = () -> {
                            String name = Thread.currentThread().getName();
                            System.out.printf(
                                    "[RUN start] %s executes task from P-%d #%d (workers=%d, idle=%d, queue=%d)\n",
                                    name, tid, msgId, executor.getWorkerCount(), executor.getIdleCount(),
                                    executor.getQueueSize());
                            try {
                                Thread.sleep(consT);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            System.out.printf("[RUN end]   %s finished task from P-%d #%d\n", name, tid, msgId);
                        };
                        // on peut soumettre directement un Message si on veut
                        executor.submit(task);
                        Thread.sleep(prodT);
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + (i + 1));
            producers.add(p);
        }

        // démarrage aléatoire
        Collections.shuffle(producers, new Random());
        for (Thread p : producers)
            p.start();

        // attendre la fin des producteurs
        for (Thread p : producers)
            p.join();

        // attendre que les workers traitent les tâches et s'arrêtent s'ils deviennent
        // inactifs
        System.out.println("Tous les producteurs ont terminé. Attente pour vidage et terminaison des workers...");
        // laisser 4 secondes -> permet aux workers d'expirer (timeout 3s)
        Thread.sleep(4000);

        System.out.println("Etat final :");
        System.out.printf("  queue size = %d%n", executor.getQueueSize());
        System.out.printf("  workers    = %d (idle=%d)%n", executor.getWorkerCount(), executor.getIdleCount());
        System.out.println("Test terminé.");
    }
}
