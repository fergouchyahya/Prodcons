package prodcons.v3;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producteur pour la version v3.
 *
 * Ici, chaque producteur reçoit un quota fixe de messages à produire.
 * Il produit exactement "quota" messages, en espaçant chaque production
 * par une pause de prodTimeMs millisecondes.
 *
 * Les identifiants de messages sont générés via un compteur global GEN
 * partagé entre tous les producteurs, ce qui garantit des IDs uniques.
 *
 * À la fin de sa production (normale ou interrompue), le producteur
 * appelle IProdConsBuffer#producerDone() dans un bloc finally
 * pour s'assurer que le buffer est correctement informé.
 */
public class Producer extends Thread {

    /**
     * Générateur global d'identifiants de messages.
     * Incrémente de manière atomique à chaque production.
     */
    private static final AtomicInteger GEN = new AtomicInteger(0);

    /**
     * Buffer partagé dans lequel ce producteur insère ses messages.
     */
    private final IProdConsBuffer buffer;

    /**
     * Nombre de messages que ce producteur doit produire.
     */
    private final int quota; // quota fixé à l’avance

    /**
     * Temps de "production" d'un message, en millisecondes.
     */
    private final int prodTimeMs;

    /**
     * Construit un producteur qui produira exactement "quota" messages.
     *
     * @param pid        identifiant logique du producteur (pour le nom du thread)
     * @param buffer     buffer partagé
     * @param quota      nombre de messages à produire
     * @param prodTimeMs délai entre deux productions
     */
    public Producer(int pid, IProdConsBuffer buffer, int quota, int prodTimeMs) {
        super("P-" + pid);
        this.buffer = buffer;
        this.quota = quota;
        this.prodTimeMs = prodTimeMs;
    }

    @Override
    public void run() {
        try {
            // Boucle principale de production : le producteur essaie
            // de produire exactement "quota" messages.
            for (int i = 0; i < quota; i++) {
                try {
                    // Simule le temps de production
                    Thread.sleep(prodTimeMs);

                    // Génère un nouvel identifiant de message
                    int id = GEN.incrementAndGet();

                    // Crée le message et l'insère dans le buffer
                    Message m = new Message(id, getId());
                    buffer.put(m);

                } catch (InterruptedException e) {
                    // Si le producteur est interrompu pendant sleep() ou put(),
                    // on log, on restaure le flag d'interruption, puis on sort.
                    Log.info("%s interrupted", getName());
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            // Fin normale après avoir produit tout le quota
            Log.info("%s finished producing quota=%d", getName(), quota);
        } finally {
            // Quel que soit le scénario (fin normale ou interruption),
            // on informe le buffer qu'un producteur de moins reste actif.
            try {
                buffer.producerDone();
            } catch (Throwable t) {
                // On ignore toute exception ici pour éviter de casser
                // la terminaison globale de l'application.
            }
        }
    }
}
