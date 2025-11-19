package prodcons.v2;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consommateur pour la version v2.
 *
 * Ce thread consomme des messages dans le buffer partagé jusqu'à être
 * interrompu.
 * Chaque message consommé incrémente un compteur global "consumed", partagé
 * entre
 * tous les consommateurs. Ce compteur permet au thread de test de savoir quand
 * l'ensemble des messages produits ont été effectivement consommés.
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

                // Si le buffer est fermé et vide, get() retourne null -> fin
                if (m == null) {
                    break;
                }

                // Signale qu'un message de plus a été consommé
                consumed.incrementAndGet();

                // Simule le temps de traitement
                Thread.sleep(consTimeMs);

            } catch (InterruptedException e) {
                // Interruption utilisée comme signal de terminaison propre
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}


