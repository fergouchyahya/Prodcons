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
 *
 * Le lock est créé en mode équitable afin de limiter la famine entre threads.
 * La terminaison est gérée par :
 * - un compteur de producteurs restants (producersRemaining),
 * - un drapeau "closed" indiquant que la production est terminée.
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

    /**
     * Nombre de producteurs n'ayant pas encore signalé leur fin.
     * Initialisé par setProducersCount(n), décrémenté par producerDone().
     */
    private int producersRemaining = 0;

    /**
     * Indique si la production est définitivement terminée.
     * Quand closed == true, aucun nouveau message ne sera inséré.
     */
    private boolean closed = false;

    // Synchronisation via ReentrantLock et conditions associées

    /**
     * Lock principal protégeant l'accès aux variables partagées
     * (buf, in, out, count, totalProduced, producersRemaining, closed).
     * Le constructeur équitable (true) réduit le risque de famine.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Condition "buffer pas plein".
     * Les producteurs attendent dessus lorsque le buffer est plein
     * (count == buf.length).
     */
    private final Condition notFull = lock.newCondition();

    /**
     * Condition "buffer pas vide".
     * Les consommateurs attendent dessus lorsque le buffer est vide
     * (count == 0).
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
                // Quand le dernier producteur a fini, on ferme le buffer
                if (producersRemaining == 0) {
                    closed = true;
                    // Réveiller tous les consommateurs potentiellement
                    // bloqués sur notEmpty pour qu'ils voient que closed == true.
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
     * Bloque tant que le buffer est vide et que la production n'est pas terminée.
     *
     * Cas particuliers :
     * - Tant que count == 0 et que closed == false, on attend sur notEmpty.
     * - Si on se réveille avec count == 0 et closed == true, cela signifie
     * qu'aucun nouveau message n'arrivera : on renvoie null pour signaler
     * la fin au consommateur.
     */
    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            // Attente conditionnelle : buffer vide mais production encore active
            while (count == 0 && !closed) {
                notEmpty.await();
            }

            // Si le buffer est vide ET fermé, plus rien à consommer
            if (count == 0 && closed) {
                return null;
            }

            // Cas normal : on retire un message du buffer circulaire
            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;

            // On a libéré une case : réveiller éventuellement un producteur
            notFull.signal();
            return m;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Renvoie le nombre de messages actuellement dans le buffer.
     * Accès protégé par le même lock que put/get pour garantir la cohérence.
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
