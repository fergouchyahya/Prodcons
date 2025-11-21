package prodcons.v1;

/**
 * Consommateur simple pour la version v1.
 * Il boucle indéfiniment :
 * - lit un message depuis le buffer partagé,
 * - logge l'opération,
 * - attend consTimeMs millisecondes.
 * La terminaison se fait uniquement en interrompant le thread
 * depuis l'extérieur.
 */
public class Consumer extends Thread {

    /**
     * Référence vers le buffer partagé à partir duquel ce consommateur retire les
     * messages.
     */
    private final IProdConsBuffer buffer;

    /**
     * Temps de "traitement" simulé après chaque consommation (en millisecondes).
     * Permet de forcer des commutations de contexte et d'observer la concurrence.
     */
    private final int consTimeMs;

    /**
     * @param buffer     buffer partagé
     * @param consTimeMs temps de pause après chaque consommation (ms)
     */
    public Consumer(IProdConsBuffer buffer, int consTimeMs) {
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
    }

    @Override
    public void run() {
        // v1 : pas de terminaison auto -> boucle infinie, arrêt par interruption.
        while (true) {
            try {
                Message m = buffer.get();

                // Log détaillé : nom du thread, message, état du buffer
                Log.info("%s got %-6s  (nmsg=%d, tot=%d)",
                        getName(), m, buffer.nmsg(), buffer.totmsg());

                Thread.sleep(consTimeMs);
            } catch (InterruptedException e) {
                // Terminaison propre du consommateur sur interruption
                Log.info("%s interrupted", getName());
                return;
            }
        }
    }
}
