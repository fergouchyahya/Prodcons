package prodcons.v5;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consommateur pour la version v5.
 *
 * Particularité : ce consommateur ne lit pas un message à la fois,
 * mais des lots de k messages via get(k).
 *
 * La boucle principale continue tant que le thread n'est pas interrompu.
 * Lorsque get(k) renvoie un tableau de taille 0, cela signifie que :
 * - la production est terminée,
 * - le buffer a été entièrement vidé.
 * Dans ce cas, le consommateur se termine proprement.
 */
public class Consumer extends Thread {

    /**
     * Buffer partagé dans lequel le consommateur puise les messages.
     */
    private final IProdConsBuffer buffer;

    /**
     * Temps de "traitement" après chaque lot consommé, en millisecondes.
     */
    private final int consTimeMs;

    /**
     * Taille des lots de consommation (paramètre k de get(k)).
     */
    private final int k;

    /**
     * Compteur global du nombre total de messages consommés par tous
     * les consommateurs.
     */
    private final AtomicInteger consumed;

    /**
     * Construit un consommateur qui consommera par lots de k messages.
     *
     * @param cid        identifiant logique pour le nom du thread
     * @param buffer     tampon partagé
     * @param consTimeMs temps de "traitement" après chaque lot
     * @param k          taille cible des lots de messages
     * @param consumed   compteur global des messages consommés
     */
    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs, int k, AtomicInteger consumed) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
        this.k = k;
        this.consumed = consumed;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                // Récupère un lot de k messages (ou moins en fin de production)
                Message[] batch = buffer.get(k);

                if (batch.length == 0) {
                    // Fin de production et tampon vidé :
                    // get(k) signale la fin en renvoyant un lot vide.
                    return;
                }

                // Mise à jour du compteur global
                consumed.addAndGet(batch.length);

                // Simule du temps de traitement sur le lot
                Thread.sleep(consTimeMs);

            } catch (InterruptedException e) {
                // Interruption = terminaison propre
                return;
            }
        }
    }
}
