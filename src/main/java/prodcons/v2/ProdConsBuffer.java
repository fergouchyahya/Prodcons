package prodcons.v2;

/**
 * Tampon borné pour la version v2 .
 *
 * Implémentation par moniteur Java (synchronized, wait, notifyAll) avec un
 * buffer circulaire.
 *
 * Variables principales :
 * - buf : tableau de messages jouant le rôle de buffer circulaire
 * - in : index de la prochaine position d'écriture
 * - out : index de la prochaine position de lecture
 * - count : nombre de messages actuellement dans le buffer
 * - totalProduced : nombre total de messages insérés depuis le début
 *
 * Les gardes sont :
 * - put : attendre tant que count == buf.length (buffer plein)
 * - get : attendre tant que count == 0 (buffer vide)
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Tableau de stockage des messages (buffer circulaire).
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
     * 0 <= count <= buf.length
     */
    private int count = 0;

    /**
     * Nombre total de messages produits depuis le démarrage.
     * Ce compteur est strictement croissant.
     */
    private int totalProduced = 0;

    /**
     * Nombre de producteurs restant à déclarer comme terminés.
     * Initialisé via setProducersCount(int) par le test avant démarrage.
     */
    private int producersRemaining = 0;

    /**
     * Indique que tous les producteurs ont fini (buffer fermé).
     */
    private boolean closed = false;

    /**
     * Construit un buffer borné avec une capacité donnée.
     *
     * @param capacity taille maximale du buffer (doit être > 0)
     */
    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
    }

    @Override
    public synchronized void setProducersCount(int n) {
        if (n < 0)
            throw new IllegalArgumentException("n < 0");
        this.producersRemaining = n;
    }

    @Override
    public synchronized void producerDone() {
        if (producersRemaining > 0) {
            producersRemaining--;
            if (producersRemaining == 0) {
                closed = true;
                // réveiller tous les consommateurs en attente
                notifyAll();
            }
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Insère un message dans le buffer.
     * Bloque tant que le buffer est plein.
     */
    @Override
    public synchronized void put(Message m) throws InterruptedException {
        while (count == buf.length)
            wait(); // buffer plein : le producteur attend

        buf[in] = m;
        in = (in + 1) % buf.length;
        count++;
        totalProduced++;

        // Réveille un ou plusieurs threads en attente (consommateurs typiquement).
        notifyAll();
    }

    /**
     * Retire et renvoie un message du buffer.
     * Bloque tant que le buffer est vide.
     */
    @Override
    public synchronized Message get() throws InterruptedException {
        while (count == 0 && !closed) {
            wait(); // buffer vide : attendre soit un message, soit la fermeture
        }

        if (count == 0 && closed) {
            // Aucun message restant et production terminée -> signal de fin
            return null;
        }

        Message m = buf[out];
        buf[out] = null; // nettoyage
        out = (out + 1) % buf.length;
        count--;

        // Réveille un ou plusieurs threads en attente (producteurs typiquement).
        notifyAll();
        return m;
    }

    /**
     * Nombre de messages actuellement dans le buffer.
     */
    @Override
    public synchronized int nmsg() {
        return count;
    }

    /**
     * Nombre total de messages produits depuis le début de l'exécution.
     */
    @Override
    public synchronized int totmsg() {
        return totalProduced;
    }
}

/*
 * Ancienne implémentation (conservée en commentaire)
 * package prodcons.v2;
 * 
 * /**
 * Tampon borné pour la version v2 .
 *
 * Implémentation par moniteur Java (synchronized, wait, notifyAll) avec un
 * buffer circulaire.
 *
 * Variables principales :
 * - buf : tableau de messages jouant le rôle de buffer circulaire
 * - in : index de la prochaine position d'écriture
 * - out : index de la prochaine position de lecture
 * - count : nombre de messages actuellement dans le buffer
 * - totalProduced : nombre total de messages insérés depuis le début
 *
 * Les gardes sont :
 * - put : attendre tant que count == buf.length (buffer plein)
 * - get : attendre tant que count == 0 (buffer vide)
 */
/*
 * public class ProdConsBuffer implements IProdConsBuffer {
 * 
 * private final Message[] buf;
 * private int in = 0;
 * private int out = 0;
 * private int count = 0;
 * private int totalProduced = 0;
 * 
 * public ProdConsBuffer(int capacity) {
 * if (capacity <= 0)
 * throw new IllegalArgumentException("capacity <= 0");
 * this.buf = new Message[capacity];
 * }
 * 
 * @Override
 * public synchronized void put(Message m) throws InterruptedException {
 * while (count == buf.length)
 * wait();
 * 
 * buf[in] = m;
 * in = (in + 1) % buf.length;
 * count++;
 * totalProduced++;
 * notifyAll();
 * }
 * 
 * @Override
 * public synchronized Message get() throws InterruptedException {
 * while (count == 0)
 * wait();
 * 
 * Message m = buf[out];
 * buf[out] = null;
 * out = (out + 1) % buf.length;
 * count--;
 * notifyAll();
 * return m;
 * }
 * 
 * @Override
 * public synchronized int nmsg() {
 * return count;
 * }
 * 
 * @Override
 * public synchronized int totmsg() {
 * return totalProduced;
 * }
 * }
 */
