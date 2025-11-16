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
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Représente un message multi-exemplaires dans le buffer.
     * Un seul slot par message, avec un compteur d'exemplaires.
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
     * Lock équitable protégeant toutes les données partagées du buffer.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Condition "buffer non plein" (il reste au moins un slot libre).
     */
    private final Condition notFull = lock.newCondition();

    /**
     * Condition "buffer non vide" (au moins un slot présent).
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
            Slot slot = new Slot(m, n, lock.newCondition());

            // Insérer le slot dans le buffer
            buf[in] = slot;
            in = (in + 1) % buf.length;
            count++;
            totalProduced += n;

            // Réveiller les consommateurs : un nouveau message est disponible
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
            // Attendre qu'il y ait au moins un slot (message disponible)
            while (count == 0) {
                notEmpty.await();
            }

            // On regarde le slot en tête de file (mais on ne l'enlève pas encore)
            Slot slot = buf[out];

            // Ce consommateur prend un exemplaire
            slot.taken++;
            boolean last = (slot.taken == slot.copies);

            if (last) {
                // Ce consommateur est le dernier :
                // - on enlève le slot du buffer
                // - on libère un slot pour les producteurs
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
                // Quand il se réveille, last a déjà fait le nettoyage.
            }

            // Tous les exemplaires de ce message ont été consommés,
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
