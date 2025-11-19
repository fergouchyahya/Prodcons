package prodcons.v6;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tampon ProdCons pour la version v6 (multi-exemplaires synchrones).
 *
 * Un message déposé en n exemplaires obéit aux règles suivantes :
 * - le message ne disparaît du buffer qu'après la consommation de n
 * exemplaires,
 * - le producteur qui a déposé ce message est bloqué jusqu'à ce que tous
 * les exemplaires soient consommés,
 * - chaque consommateur qui prend un exemplaire est aussi bloqué jusqu'à ce
 * que tous les exemplaires de ce message aient été consommés.
 *
 * Concrètement, on modélise chaque message par un "slot" avec :
 * - le message,
 * - le nombre total d'exemplaires (copies),
 * - le nombre déjà consommé (taken),
 * - une Condition servant de barrière pour ce message.
 *
 * Le buffer lui-même est un tableau circulaire de slots, protégé par un
 * ReentrantLock équitable, avec deux conditions globales :
 * - notFull : le buffer n'est pas plein (au moins un slot libre),
 * - notEmpty: le buffer n'est pas vide (au moins un slot présent).
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Représente un message multi-exemplaires dans le buffer.
     * Un seul slot par message, avec un compteur d'exemplaires.
     *
     * allConsumed est une condition spécifique à ce message, utilisée comme
     * barrière :
     * - le producteur attend dessus jusqu'à ce que taken == copies,
     * - les consommateurs (sauf le dernier) attendent aussi que taken == copies.
     */
    private static final class Slot {
        final Message msg;
        final int copies; // nombre total d'exemplaires
        int taken = 0; // nombre déjà consommé
        final Condition allConsumed; // barrière pour producteur et consommateurs

        Slot(Message msg, int copies, Condition allConsumed) {
            this.msg = msg;
            this.copies = copies;
            this.allConsumed = allConsumed;
        }
    }

    /**
     * Buffer circulaire de slots (un slot par message logique).
     */
    private final Slot[] buf;

    /**
     * Index d'insertion (prochaine position d'écriture de slot).
     */
    private int in = 0;

    /**
     * Index de lecture (slot en tête de file).
     */
    private int out = 0;

    /**
     * Nombre de slots actuellement présents dans le buffer.
     * 0 <= count <= buf.length
     */
    private int count = 0;

    /**
     * Nombre total d'exemplaires de messages produits depuis le début.
     * Chaque exemplaire compte, même si un seul slot représente n copies.
     */
    private int totalProduced = 0;

    /**
     * Nombre de producteurs qui n'ont pas encore signalé leur fin.
     * Initialisé via setProducersCount(), décrémenté par producerDone().
     */
    private int producersRemaining = 0;

    /**
     * Indique si la production est terminée (tous les producteurs ont
     * appelé producerDone()).
     * Quand closed == true, aucun nouveau slot ne sera inséré.
     */
    private volatile boolean closed = false;

    /**
     * Lock équitable protégeant toutes les données partagées du buffer.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Condition "buffer non plein" (il reste au moins un slot libre).
     * Les producteurs attendent dessus lorsque count == buf.length.
     */
    private final Condition notFull = lock.newCondition();

    /**
     * Condition "buffer non vide" (au moins un slot présent).
     * Les consommateurs attendent dessus lorsque count == 0 et que le
     * buffer n'est pas encore fermé.
     */
    private final Condition notEmpty = lock.newCondition();

    /**
     * Construit un buffer v6 avec une capacité donnée en nombre de slots.
     *
     * @param capacity nombre maximal de messages différents simultanément
     */
    public ProdConsBuffer(int capacity) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        this.buf = new Slot[capacity];
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
                    // Tous les producteurs ont fini : on ferme le buffer.
                    closed = true;
                    // Réveiller les consommateurs qui attendent alors que
                    // plus aucun slot ne pourra apparaître.
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

    @Override
    public void put(Message m, int n) throws InterruptedException {
        if (n <= 0)
            throw new IllegalArgumentException("n <= 0");

        lock.lock();
        try {
            // Attendre un slot libre si le buffer est plein
            while (count == buf.length) {
                notFull.await();
            }

            // Créer le slot logique pour ce message multi-exemplaires
            // avec une condition dédiée à ce message.
            Slot slot = new Slot(m, n, lock.newCondition());

            // Insérer le slot dans le buffer
            buf[in] = slot;
            in = (in + 1) % buf.length;
            count++;
            totalProduced += n;

            // Réveiller les consommateurs : un nouveau slot est disponible
            notEmpty.signalAll();

            // Barrière producteur :
            // tant que tous les exemplaires n'ont pas été consommés,
            // le producteur reste bloqué sur la condition du slot.
            while (slot.taken < slot.copies) {
                slot.allConsumed.await();
            }

            // Quand on sort de cette boucle, tous les exemplaires ont été consommés
            // et le slot a été retiré du buffer par le dernier consommateur.
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            // Tant que le buffer est vide et que la production n'est pas déclarée
            // terminée, on attend l'arrivée d'un slot.
            while (count == 0 && !closed) {
                notEmpty.await();
            }

            // Si le buffer est vide et que la production est terminée,
            // plus rien à consommer : on signale la fin par null.
            if (count == 0 && closed) {
                return null;
            }

            // On regarde le slot en tête de file (mais on ne l'enlève pas encore).
            Slot slot = buf[out];

            // Ce consommateur prend un exemplaire de ce message.
            slot.taken++;
            boolean last = (slot.taken == slot.copies);

            if (last) {
                // Ce consommateur est le dernier à prendre un exemplaire :
                // - on enlève le slot du buffer,
                // - on libère un slot pour les producteurs,
                // - on réveille tous ceux qui attendent la fin de consommation
                // de ce message (producteur + autres consommateurs).
                buf[out] = null;
                out = (out + 1) % buf.length;
                count--;

                notFull.signal(); // buffer moins plein
                slot.allConsumed.signalAll(); // réveiller producteur + autres consommateurs
            } else {
                // Ce n'est pas le dernier consommateur :
                // il doit attendre que tous les exemplaires soient consommés.
                while (slot.taken < slot.copies) {
                    slot.allConsumed.await();
                }
                // Quand il se réveille, le dernier consommateur a déjà fait
                // le nettoyage du slot et libéré les ressources.
            }

            // Tous les exemplaires de ce message ont été consommés :
            // ce consommateur peut poursuivre avec le message qu'il a récupéré.
            return slot.msg;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public int nmsg() {
        lock.lock();
        try {
            // Nombre de slots actuellement stockés dans le buffer.
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int totmsg() {
        lock.lock();
        try {
            // Nombre total d'exemplaires produits depuis le début.
            return totalProduced;
        } finally {
            lock.unlock();
        }
    }
}
