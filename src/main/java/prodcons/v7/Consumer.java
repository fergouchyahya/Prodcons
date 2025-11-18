package prodcons.v7;

/**
 * Consommateur de tâches.
 *
 * Boucle principale :
 * - récupère un Message depuis le buffer,
 * - exécute la tâche associée (m.run()).
 */
public class Consumer extends Thread {

    private final IProdConsBuffer buffer;
    private final int consTimeMs;

    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message m = buffer.get();
                if (m == null)
                    break;
                System.out.printf("[CONS] %s a reçu %s, exécution de la tâche...%n",
                        getName(), m);

                // Exécute la tâche associée
                m.run();

                Thread.sleep(consTimeMs);
            }
        } catch (InterruptedException e) {
            System.out.printf("[CONS] %s interrompu%n", getName());
            Thread.currentThread().interrupt();
        }
    }
}

/*
 * Ancienne version (conservée en commentaire)
 * package prodcons.v7;
 * 
 * /**
 * Consommateur de tâches.
 *
 * Boucle principale :
 * - récupère un Message depuis le buffer,
 * - exécute la tâche associée (m.run()).
 */
/*
 * public class Consumer extends Thread {
 * 
 * private final IProdConsBuffer buffer;
 * private final int consTimeMs;
 * 
 * public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs) {
 * super("C-" + cid);
 * this.buffer = buffer;
 * this.consTimeMs = consTimeMs;
 * }
 * 
 * @Override
 * public void run() {
 * try {
 * while (!isInterrupted()) {
 * Message m = buffer.get();
 * System.out.printf("[CONS] %s a reçu %s, exécution de la tâche...%n",
 * getName(), m);
 * 
 * // Exécute la tâche associée
 * m.run();
 * 
 * Thread.sleep(consTimeMs);
 * }
 * } catch (InterruptedException e) {
 * System.out.printf("[CONS] %s interrompu%n", getName());
 * }
 * }
 * }
 */
