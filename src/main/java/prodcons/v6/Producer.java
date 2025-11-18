package prodcons.v6;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producteur pour la version v6.
 *
 * Chaque producteur :
 * - produit un certain nombre de messages (quota),
 * - pour chaque message, le dépose en nCopies exemplaires dans le buffer,
 * - reste bloqué sur put(m, nCopies) tant que tous les exemplaires n'ont pas
 * été consommés.
 */
public class Producer extends Thread {

    /**
     * Générateur atomique d'identifiants de messages.
     */
    private static final AtomicInteger GEN = new AtomicInteger(0);

    /**
     * Buffer partagé.
     */
    private final IProdConsBuffer buffer;

    /**
     * Nombre de messages logiques que ce producteur doit produire.
     */
    private final int quota;

    /**
     * Nombre d'exemplaires par message logique.
     */
    private final int nCopies;

    /**
     * Temps de "production" par message, en millisecondes.
     */
    private final int prodTimeMs;

    /**
     * Construit un producteur v6.
     *
     * @param pid        identifiant logique pour le nom du thread
     * @param buffer     tampon partagé
     * @param quota      nombre de messages logiques à produire
     * @param nCopies    nombre d'exemplaires par message
     * @param prodTimeMs temps de production simulé
     */
    public Producer(int pid,
            IProdConsBuffer buffer,
            int quota,
            int nCopies,
            int prodTimeMs) {
        super("P-" + pid);
        this.buffer = buffer;
        this.quota = quota;
        this.nCopies = nCopies;
        this.prodTimeMs = prodTimeMs;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < quota; i++) {
                try {
                    Thread.sleep(prodTimeMs);

                    int id = GEN.incrementAndGet();
                    Message m = new Message(id, getId());

                    Log.info("%s putting message %s in %d copies",
                            getName(), m, nCopies);

                    buffer.put(m, nCopies);

                    Log.info("%s finished synchronized production of %s (%d copies)",
                            getName(), m, nCopies);
                } catch (InterruptedException e) {
                    Log.info("%s interrupted", getName());
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            Log.info("%s finished producing quota=%d messages (each %d copies)",
                    getName(), quota, nCopies);
        } finally {
            try {
                buffer.producerDone();
            } catch (Throwable t) {
            }
        }
    }
}
