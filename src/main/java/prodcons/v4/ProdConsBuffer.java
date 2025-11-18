package prodcons.v4;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tampon borné pour la version v4 utilisant ReentrantLock et Condition.
 *
 * Objectif :
 * - Reprendre la logique producteur/consommateur avec un buffer circulaire,
 * - Utiliser explicitement les sections critiques conditionnelles de Java
 * (ReentrantLock + Condition).
 * Le lock est créé en mode équitable afin de limiter la famine entre threads.
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Buffer circulaire de stockage des messages.
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
     * On a toujours 0 <= count <= buf.length.
     */
    private int count = 0;

    /**
     * Nombre total de messages produits depuis le début.
     * Ce compteur ne diminue jamais et sert essentiellement aux stats/tests.
     */
    private int totalProduced = 0;

    private int producersRemaining = 0;
    private boolean closed = false;

    // Synchronisation via ReentrantLock et conditions associées

    /**
     * Lock principal protégeant l'accès aux variables partagées
     * (buf, in, out, count, totalProduced).
     * Le constructeur équitable (true) réduit le risque de famine.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Condition "buffer pas plein".
     * Les producteurs attendent dessus lorsque le buffer est plein.
     */
    private final Condition notFull = lock.newCondition();

    /**
     * Condition "buffer pas vide".
     * Les consommateurs attendent dessus lorsque le buffer est vide.
     */
    private final Condition notEmpty = lock.newCondition();

    /**
     * Construit un buffer v4 avec une capacité donnée.
     *
     * @param capacity taille maximale du buffer (strictement positive)
     */
    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Message[capacity];
    }

    @Override
    public void setProducersCount(int n) {
        if (n < 0)
            throw new IllegalArgumentException("n < 0");
        lock.lock();
        try {
            this.producersRemaining = n;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void producerDone() {
        lock.lock();
        try {
            if (producersRemaining > 0) {
                producersRemaining--;
                if (producersRemaining == 0) {
                    closed = true;
                    // réveiller tous les consommateurs en attente
                    notEmpty.signalAll();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isClosed() {
        lock.lock();
        try {
            return closed;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Insère un message dans le buffer.
     * Bloque tant que le buffer est plein.
     */
    @Override
    public void put(Message m) throws InterruptedException {
        lock.lock();
        try {
            // Tant que le buffer est plein, on attend sur la condition "notFull".
            while (count == buf.length) {
                notFull.await(); // attendre de la place
            }

            // Insertion du message dans la case "in"
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;

            // Au moins un message est désormais disponible pour un consommateur.
            // signal() réveille un seul consommateur en attente.
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retire et renvoie un message du buffer.
     * Bloque tant que le buffer est vide.
     */
    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0 && !closed) {
                notEmpty.await();
            }

            if (count == 0 && closed) {
                return null;
            }

            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;

            notFull.signal();
            return m;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Renvoie le nombre de messages actuellement dans le buffer.
     * Accès protégé par le même lock que put/get.
     */
    @Override
    public int nmsg() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Renvoie le nombre total de messages produits depuis le début.
     */
    @Override
    public int totmsg() {
        lock.lock();
        try {
            return totalProduced;
        } finally {
            lock.unlock();
        }
    }
}
