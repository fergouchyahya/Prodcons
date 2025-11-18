package prodcons.v2;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producteur pour la version v2.
 *
 * Ici, chaque producteur reçoit un quota fixe de messages à produire.
 * Il produit exactement "quota" messages, en espaçant chaque production
 * par une pause de prodTimeMs millisecondes.
 *
 * Les identifiants de messages sont générés via un compteur global GEN
 * partagé entre tous les producteurs, ce qui garantit des IDs uniques.
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
                    // Interruption reçue pendant la production :
                    Log.info("%s interrupted", getName());
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            // Fin normale après avoir produit tout le quota
            Log.info("%s finished producing quota=%d", getName(), quota);
        } finally {
            // Signal de terminaison pour le buffer (centralise la terminaison)
            try {
                buffer.producerDone();
            } catch (Throwable t) {
                // Ne pas interrompre la fin en cas d'erreur de signalisation
            }
        }
    }
}

/*
 * Ancienne version (conservée en commentaire)
 * package prodcons.v2;
 * 
 * import java.util.concurrent.atomic.AtomicInteger;
 * 
 * /**
 * Producteur pour la version v2.
 *
 * Ici, chaque producteur reçoit un quota fixe de messages à produire.
 * Il produit exactement "quota" messages, en espaçant chaque production
 * par une pause de prodTimeMs millisecondes.
 *
 * Les identifiants de messages sont générés via un compteur global GEN
 * partagé entre tous les producteurs, ce qui garantit des IDs uniques.
 */
/*
 * public class Producer extends Thread {
 * private static final AtomicInteger GEN = new AtomicInteger(0);
 * private final IProdConsBuffer buffer;
 * private final int quota;
 * private final int prodTimeMs;
 * 
 * public Producer(int pid, IProdConsBuffer buffer, int quota, int prodTimeMs) {
 * super("P-" + pid);
 * this.buffer = buffer;
 * this.quota = quota;
 * this.prodTimeMs = prodTimeMs;
 * }
 * 
 * @Override
 * public void run() {
 * for (int i = 0; i < quota; i++) {
 * try {
 * Thread.sleep(prodTimeMs);
 * int id = GEN.incrementAndGet();
 * Message m = new Message(id, getId());
 * buffer.put(m);
 * } catch (InterruptedException e) {
 * Log.info("%s interrupted", getName());
 * return;
 * }
 * }
 * Log.info("%s finished producing quota=%d", getName(), quota);
 * }
 * }
 */
