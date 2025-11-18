package prodcons.v6;

/**
 * Consommateur pour la version v6.
 *
 * Il consomme les messages un par un via get().
 * La méthode get() implémente la synchronisation multi-exemplaires :
 * chaque consommateur qui prend un exemplaire d'un message reste bloqué
 * jusqu'à ce que tous les exemplaires de ce message aient été consommés.
 *
 * Ce consommateur simule ensuite un temps de traitement.
 */
public class Consumer extends Thread {

    /**
     * Buffer partagé.
     */
    private final IProdConsBuffer buffer;

    /**
     * Temps de "traitement" après chaque message, en millisecondes.
     */
    private final int consTimeMs;

    /**
     * Construit un consommateur v6.
     *
     * @param cid        identifiant logique pour le nom du thread
     * @param buffer     tampon partagé
     * @param consTimeMs temps de traitement simulé
     */
    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Message m = buffer.get();
                if (m == null)
                    break;

                Log.info("%s got %s (multi-exemplaires sync OK)", getName(), m);

                Thread.sleep(consTimeMs);
            } catch (InterruptedException e) {
                Log.info("%s interrupted", getName());
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
