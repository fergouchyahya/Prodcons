package prodcons.v1;

/**
 * Implémentation du tampon borné Prod/Cons (solution directe avec wait/notify).
 *
 * 
 * Propriétés :
 * 
 * Buffer circulaire de capacité fixe (taille fournie au constructeur).
 * Synchronisation via le moniteur Java (méthodes synchronized +
 * wait/notifyAll).
 * Les variables implicites du TD :
 * 
 * nfull ≈ count : nombre de cases occupées
 * nempty ≈ buf.length - count : nombre de cases libres
 * 
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Tableau circulaire qui contient les messages.
     * Index logiques : in (prochaine case d'écriture), out
     * (prochaine case de lecture).
     */
    public final Message[] buf;

    /**
     * Nombre de messages actuellement dans le buffer (0 <= count <= buf.length).
     * C'est l'équivalent de nfull dans le TD.
     */
    private int count = 0;

    /**
     * Index de la prochaine case à lire (consommation).
     * Avance modulo buf.length.
     */
    private int out = 0;

    /**
     * Index de la prochaine case à écrire (production).
     * Avance modulo buf.length.
     */
    private int in = 0;

    /**
     * Nombre total de messages produits depuis le démarrage.
     * Sert pour les stats et les tests, indépendamment du contenu actuel du buffer.
     */
    private int totalProduced = 0;

    /**
     * Construit un tampon de capacité donnée.
     *
     * @param capacity taille maximale du buffer (doit être ≥ 0)
     */
    public ProdConsBuffer(int capacity) {
        if (capacity < 0)
            throw new IllegalArgumentException("capacity<0");
        this.buf = new Message[capacity];
    }

    /**
     * Production d'un message.
     * Garde : tant que le buffer est plein (count == buf.length),
     * le thread appelant attend (wait) qu'une place se libère.
     */
    @Override
    public synchronized void put(Message m) throws InterruptedException {
        // Garde : attendre qu'il y ait au moins une case vide (nempty > 0)
        while (count == buf.length) {
            wait(); // buffer plein => le producteur se bloque
        }

        // Action : écrire le message dans la case "in" et avancer l'index
        buf[in] = m;
        in = (in + 1) % buf.length;
        count++;
        totalProduced++;

        // Réveil de tous les threads en attente : un consommateur peut maintenant lire.
        notifyAll();
    }

    /**
     * Consommation d'un message.
     * Garde : tant que le buffer est vide (count == 0),
     * le thread appelant attend (wait) qu'un message arrive.
     */
    @Override
    public synchronized Message get() throws InterruptedException {
        // Garde : attendre qu'il y ait au moins un message à lire (nfull > 0)
        while (count == 0) {
            wait(); // buffer vide => le consommateur se bloque
        }

        // Action : lire le message de la case "out" et avancer l'index
        Message m = buf[out];
        buf[out] = null; // pas obligatoire
        out = (out + 1) % buf.length;
        count--;

        // Réveil de tous les threads en attente : un producteur peut maintenant écrire.
        notifyAll();

        return m;
    }

    /**
     * @return nombre de messages actuellement présents dans le buffer
     */
    @Override
    public synchronized int nmsg() {
        return count;
    }

    /**
     * @return nombre total de messages produits depuis le début (statistique
     *         monotone)
     */
    @Override
    public synchronized int totmsg() {
        return totalProduced;
    }
}
