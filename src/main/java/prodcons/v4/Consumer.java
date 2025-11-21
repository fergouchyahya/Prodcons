package prodcons.v4;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consommateur pour la version v4.
 *
 * Ce thread consomme des messages dans le buffer partagé jusqu'à :
 * - recevoir un signal de fin (get() renvoie null lorsque le buffer est
 * fermé et vide),
 * - ou être interrompu.
 *
 * Chaque message consommé incrémente un compteur global "consumed", partagé
 * entre tous les consommateurs. Ce compteur permet au thread de test de savoir
 * quand l'ensemble des messages produits ont été effectivement consommés.
 */
public class Consumer extends Thread {

    /**
     * Buffer partagé à partir duquel ce consommateur retire les messages.
     */
    private final IProdConsBuffer buffer;

    /**
     * Temps de "traitement" après chaque consommation, en millisecondes.
     * Permet de simuler un temps de travail et de favoriser la concurrence.
     */
    private final int consTimeMs;

    /**
     * Compteur global du nombre total de messages consommés.
     * Ce compteur est partagé par tous les consommateurs.
     */
    private final AtomicInteger consumed; // compteur global partagé

    /**
     * Construit un consommateur.
     *
     * @param cid        identifiant logique du consommateur (utilisé dans le nom du
     *                   thread)
     * @param buffer     buffer partagé
     * @param consTimeMs temps de pause après chaque message consommé
     * @param consumed   compteur global de messages consommés
     */
    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs, AtomicInteger consumed) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
        this.consumed = consumed;
    }

    @Override
    public void run() {
        while (true) {
            try {
                // Récupère un message depuis le buffer (bloquant si vide)
                Message m = buffer.get();

                
                // - si le buffer est fermé et définitivement vide,
                // get() renvoie null pour indiquer au consommateur d’arrêter.
                if (m == null)
                    break;

                // Incrémente le compteur global de messages consommés
                consumed.incrementAndGet();

                // Simule le temps de traitement du message
                Thread.sleep(consTimeMs);

            } catch (InterruptedException e) {
                // Interruption "propre" : on restaure le flag d’interruption
                // puis on termine le thread.
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
