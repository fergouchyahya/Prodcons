package prodcons.v5;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producteur pour la version v5.
 *
 * Chaque producteur possède un quota fixe de messages à produire.
 * Il produit exactement "quota" messages, espacés de prodTimeMs millisecondes,
 * et les insère dans le buffer partagé.
 *
 * Les identifiants des messages sont générés à partir d'un compteur global GEN,
 * ce qui garantit des IDs uniques sur l'ensemble des producteurs.
 *
 * Contrairement à v3/v4, il n'y a pas de producerDone() : la terminaison
 * globale est gérée par expectedTotal dans le buffer, qui connaît la somme
 * des quotas de tous les producteurs.
 */
public class Producer extends Thread {

    /**
     * Générateur atomique global d'identifiants de messages.
     */
    private static final AtomicInteger GEN = new AtomicInteger(0);

    /**
     * Buffer partagé vers lequel ce producteur envoie ses messages.
     */
    private final IProdConsBuffer buffer;

    /**
     * Nombre de messages que ce producteur doit produire.
     */
    private final int quota;

    /**
     * Durée de "production" d'un message, en millisecondes,
     * utilisée pour forcer un peu de concurrence.
     */
    private final int prodTimeMs;

    /**
     * Construit un producteur avec un quota donné.
     *
     * @param pid        identifiant logique pour le nom du thread
     * @param buffer     tampon partagé
     * @param quota      nombre de messages à produire
     * @param prodTimeMs temps de production simulé entre deux messages
     */
    public Producer(int pid, IProdConsBuffer buffer, int quota, int prodTimeMs) {
        super("P-" + pid);
        this.buffer = buffer;
        this.quota = quota;
        this.prodTimeMs = prodTimeMs;
    }

    @Override
    public void run() {
        for (int i = 0; i < quota; i++) {
            try {
                // Simule un temps de production
                Thread.sleep(prodTimeMs);

                // Génère un identifiant unique pour le message
                int id = GEN.incrementAndGet();

                // Insère le message dans le buffer
                buffer.put(new Message(id, getId()));

            } catch (InterruptedException e) {
                // Interruption = arrêt anticipé, on ne force pas la fin du quota.
                // Le buffer reste cohérent car totalProduced ne sera pas égal
                // à expectedTotal si on ne produit pas tout.
                return;
            }
        }
        // Fin normale après avoir produit tout le quota
        Log.info("%s finished producing quota=%d", getName(), quota);
    }
}
