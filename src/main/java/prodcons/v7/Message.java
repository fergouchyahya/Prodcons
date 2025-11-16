package prodcons.v7;

/**
 * Message contenant une tâche à exécuter.
 *
 * Le message implémente Runnable :
 * - la méthode run() correspond à la tâche à exécuter par un consommateur.
 */
public class Message implements Runnable {

    /**
     * Identifiant logique du message (pour les logs).
     */
    public final int id;

    /**
     * Identifiant du producteur ayant créé ce message (id du thread).
     */
    public final long producerTid;

    /**
     * Tâche à exécuter lorsque le consommateur appelle run().
     */
    private final Runnable task;

    public Message(int id, long producerTid, Runnable task) {
        this.id = id;
        this.producerTid = producerTid;
        this.task = task;
    }

    @Override
    public void run() {
        // Délègue l'exécution à la tâche interne
        task.run();
    }

    @Override
    public String toString() {
        return "Task#" + id + "(P" + producerTid + ")";
    }
}
