package prodcons.v1;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Producteur de la version v1.
 *
 * Cette classe représente un thread qui produit un nombre aléatoire de messages
 * compris entre minProd et maxProd. Le producteur simule un temps de production
 * entre chaque message, puis insère le message dans le buffer partagé.
 *
 * Le producteur ne termine pas en fonction d'un signal global. Il s'arrête
 * uniquement une fois qu'il a produit sa quantité prévue de messages ou quand
 * il est interrompu depuis l'extérieur.
 *
 * Les messages créés reçoivent un identifiant unique grâce à GEN, afin de
 * faciliter le suivi et la journalisation.
 */
public class Producer extends Thread {

    /**
     * Générateur atomique d'identifiants de messages.
     * Chaque appel à incrementAndGet fournit un nouvel ID unique, même si
     * plusieurs producteurs s'exécutent en concurrence.
     */
    private static final AtomicInteger GEN = new AtomicInteger(0);

    /**
     * Le buffer partagé dans lequel le producteur insère ses messages.
     */
    private final IProdConsBuffer buffer;

    /**
     * Nombre minimal de messages que ce producteur doit produire.
     */
    private final int minProd;

    /**
     * Nombre maximal de messages que ce producteur peut produire.
     */
    private final int maxProd;

    /**
     * Temps de "fabrication" d'un message, en millisecondes.
     * Ce délai force davantage de commutations de contexte entre threads.
     */
    private final int prodTimeMs;

    /**
     * Construit un producteur configuré avec une plage de production et un
     * délai entre chaque production. Le nom du thread est fixé ici afin de
     * faciliter la lecture des logs.
     */
    public Producer(IProdConsBuffer buffer, int minProd, int maxProd, int prodTimeMs) {
        this.buffer = buffer;
        this.minProd = minProd;
        this.maxProd = maxProd;
        this.prodTimeMs = prodTimeMs;
        setName("Producer-" + getId());
    }

    @Override
    public void run() {
        // Sélection aléatoire du nombre de messages à produire
        int n = ThreadLocalRandom.current().nextInt(minProd, maxProd + 1);

        for (int i = 0; i < n; i++) {
            try {
                // Simule le temps de production d'un message
                Thread.sleep(prodTimeMs);

                // Création d'un nouvel identifiant global
                int id = GEN.incrementAndGet();

                // Création du message portant son ID et celui du thread producteur
                Message m = new Message(id, getId());

                // Insertion du message dans le buffer
                buffer.put(m);

                // Journalisation de l'opération de production
                Log.info("%s put %-6s  (nmsg=%d, tot=%d)",
                        getName(), m, buffer.nmsg(), buffer.totmsg());

            } catch (InterruptedException e) {
                // Arrêt propre du producteur si on lui envoie une interruption
                Log.info("%s interrupted", getName());
                return;
            }
        }

        // Fin normale de la production pour ce thread
        Log.info("%s finished producing", getName());
    }
}
