package prodcons.v3;

import java.util.concurrent.Semaphore;

/**
 * Tampon borné pour la version v3 utilisant des sémaphores.
 *
 * Synchronisation :
 * - empty : nombre de cases libres restantes dans le buffer
 * - full : nombre de messages disponibles pour consommation
 * - mutex : exclusion mutuelle autour des variables partagées (in, out, count,
 * totalProduced)
 *
 * Objectif : reproduire la solution classique Producteur/Consommateur avec
 * sémaphores,
 * tout en favorisant le parallélisme. Les producteurs et consommateurs ne sont
 * bloqués
 * que lorsque le buffer est réellement plein ou vide, et la section critique
 * protégée
 * par mutex est minimale.
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Tableau de stockage des messages, utilisé comme buffer circulaire.
     */
    private final Message[] buf;

    /**
     * Index de la prochaine écriture dans le buffer.
     */
    private int in = 0;

    /**
     * Index de la prochaine lecture dans le buffer.
     */
    private int out = 0;

    /**
     * Nombre de messages actuellement présents dans le buffer.
     * Contraintes : 0 <= count <= buf.length
     */
    private int count = 0;

    /**
     * Nombre total de messages produits depuis le démarrage.
     * Ce compteur ne diminue jamais.
     */
    private int totalProduced = 0;

    // Sémaphores

    /**
     * Sémaphore représentant le nombre de cases libres dans le buffer.
     * Initialisé à la capacité du buffer.
     * Les producteurs font empty.acquire() avant d'insérer.
     */
    private final Semaphore empty; // nb de cases libres

    /**
     * Sémaphore représentant le nombre de messages disponibles.
     * Initialisé à 0.
     * Les consommateurs font full.acquire() avant de lire.
     */
    private final Semaphore full; // nb de messages prêts

    /**
     * Sémaphore binaire pour l'exclusion mutuelle sur les variables partagées.
     * Sert à protéger in, out, count et totalProduced.
     */
    private final Semaphore mutex; // exclusion mutuelle

    /**
     * Construit un buffer avec une capacité donnée.
     *
     * @param capacity taille maximale du buffer (strictement positive)
     */
    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
        this.empty = new Semaphore(capacity, true); // au début toutes les cases sont libres
        this.full = new Semaphore(0, true); // au début aucun message disponible
        this.mutex = new Semaphore(1, true); // mutex binaire, juste pour la section critique
    }

    /**
     * Insère un message dans le buffer.
     * Bloque si le buffer est plein (empty == 0).
     */
    @Override
    public void put(Message m) throws InterruptedException {
        // Attendre une case libre
        empty.acquire();

        // Entrer en section critique
        mutex.acquire();
        try {
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;
        } finally {
            // Sortir de la section critique
            mutex.release();
        }

        // Signaler qu'un message de plus est disponible
        full.release();
    }

    /**
     * Retire et renvoie un message du buffer.
     * Bloque si le buffer est vide (full == 0).
     */
    @Override
    public Message get() throws InterruptedException {
        // Attendre qu'un message soit disponible
        full.acquire();

        // Entrer en section critique
        mutex.acquire();
        try {
            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;
            return m;
        } finally {
            // Sortir de la section critique et libérer une case libre
            mutex.release();
            empty.release();
        }
    }

    /**
     * Lecture du nombre de messages actuellement stockés.
     * Utilise le même mutex que put/get pour garantir la cohérence.
     */
    @Override
    public int nmsg() {
        mutex.acquireUninterruptibly();
        try {
            return count;
        } finally {
            mutex.release();
        }
    }

    /**
     * Lecture du nombre total de messages produits depuis le début.
     */
    @Override
    public int totmsg() {
        mutex.acquireUninterruptibly();
        try {
            return totalProduced;
        } finally {
            mutex.release();
        }
    }
}
