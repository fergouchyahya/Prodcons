package prodcons.v7;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TaskExecutor : accepte des tâches (Runnable) et exécute automatiquement
 * les tâches via un pool de threads consommateurs géré dynamiquement.
 *
 * Politique :
 * - Lorsqu'une tâche est déposée et qu'aucun consommateur inactif n'est
 * disponible, on crée un nouveau thread consommateur (si on n'a pas
 * atteint le maximum autorisé).
 * - Lorsqu'un consommateur est inactif pendant plus de 3 secondes, il se
 * termine automatiquement.
 */
public class TaskExecutor {

    private final ArrayBlockingQueue<Runnable> queue;
    private final int maxWorkers;

    // Protège la création/destruction de workers
    private final Object lock = new Object();

    // Nombre de workers actuellement démarrés
    private int currentWorkers = 0;

    // Nombre de workers qui sont actuellement en attente (inactifs)
    private final AtomicInteger idleWorkers = new AtomicInteger(0);

    // Id sequence pour les messages (utile pour logs si on utilise Message)
    private final AtomicInteger nextId = new AtomicInteger(1);

    public TaskExecutor(int capacity, int maxWorkers) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        if (maxWorkers <= 0)
            throw new IllegalArgumentException("maxWorkers <= 0");
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.maxWorkers = maxWorkers;
    }

    /**
     * Soumet une tâche à exécuter. Bloque si la file est pleine.
     */
    public void submit(Runnable task) throws InterruptedException {
        if (task == null)
            throw new NullPointerException("task == null");

        // On enveloppe la tâche dans un Message pour avoir un toString utile
        Runnable toEnqueue;
        if (task instanceof Message) {
            toEnqueue = task;
        } else {
            toEnqueue = new Message(nextId.getAndIncrement(), Thread.currentThread().getId(), task);
        }

        // Mettre dans la queue (bloquant si plein)
        queue.put(toEnqueue);

        // Si aucun worker inactif disponible, tenter d'en créer un (sous verrou)
        synchronized (lock) {
            if (idleWorkers.get() == 0 && currentWorkers < maxWorkers) {
                createWorker();
            }
        }
    }

    private void createWorker() {
        currentWorkers++;
        Thread t = new Thread(new Worker(), "TaskExecutor-worker-" + currentWorkers);
        t.setDaemon(false);
        t.start();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getWorkerCount() {
        synchronized (lock) {
            return currentWorkers;
        }
    }

    public int getIdleCount() {
        return idleWorkers.get();
    }

    private class Worker implements Runnable {
        @Override
        public void run() {
            boolean counted = true;
            try {
                while (true) {
                    idleWorkers.incrementAndGet();
                    Runnable task;
                    try {
                        task = queue.poll(3, TimeUnit.SECONDS);
                    } finally {
                        idleWorkers.decrementAndGet();
                    }

                    if (task == null) {
                        // timeout -> this worker dies
                        synchronized (lock) {
                            currentWorkers--;
                        }
                        counted = false;
                        break;
                    }

                    try {
                        task.run();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                // en cas d’exception, on s’assure de décrémenter aussi
                synchronized (lock) {
                    if (counted) {
                        currentWorkers--;
                    }
                }
            }
        }
    }

}
