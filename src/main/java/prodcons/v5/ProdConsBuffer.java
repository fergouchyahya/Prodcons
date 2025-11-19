package prodcons.v5;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tampon ProdCons pour la version v5.
 *
 * Fonctionnalités :
 * - buffer circulaire borné, synchronisé avec ReentrantLock + Conditions,
 * - support de la consommation unitaire (get()) et par lot (get(k)),
 * - gestion de la fin de production via expectedTotal, pour que les
 * consommateurs puissent détecter qu'il n'y aura plus jamais de nouveaux
 * messages.
 *
 * expectedTotal représente le nombre total de messages qui seront produits
 * (somme des quotas de tous les producteurs).
 * Quand totalProduced atteint expectedTotal, le buffer est considéré comme
 * "fermé" (plus de production à venir).
 */
public class ProdConsBuffer implements IProdConsBuffer {

    /**
     * Tableau de stockage des messages, utilisé comme buffer circulaire.
     */
    private final Message[] buf;

    /**
     * Index de la prochaine écriture.
     */
    private int in = 0;

    /**
     * Index de la prochaine lecture.
     */
    private int out = 0;

    /**
     * Nombre de messages actuellement dans le buffer (0 <= count <= buf.length).
     */
    private int count = 0;

    /**
     * Nombre total de messages produits depuis le début.
     * Quand totalProduced == expectedTotal, plus aucun message ne sera produit.
     */
    private int totalProduced = 0;

    /**
     * Lock équitable pour protéger l'accès au buffer et aux compteurs.
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Condition signalant "buffer non plein".
     * Les producteurs attendent dessus quand count == buf.length.
     */
    private final Condition notFull = lock.newCondition();

    /**
     * Condition signalant "buffer non vide".
     * Les consommateurs attendent dessus quand count == 0 et que la production
     * n'est pas encore terminée.
     */
    private final Condition notEmpty = lock.newCondition();

    /**
     * Nombre total de messages que l'on s'attend à produire au cours de
     * l'exécution.
     * Ce nombre est connu à l'avance (somme des quotas des producteurs).
     */
    private final int expectedTotal;

    /**
     * Construit un buffer de capacité donnée, en sachant à l'avance combien
     * de messages seront produits au total.
     *
     * @param capacity      taille maximale du buffer
     * @param expectedTotal nombre total de messages attendus
     */
    public ProdConsBuffer(int capacity, int expectedTotal) {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity <= 0");
        if (expectedTotal < 0)
            throw new IllegalArgumentException("expectedTotal < 0");
        this.buf = new Message[capacity];
        this.expectedTotal = expectedTotal;
    }

    /**
     * Indique si la production est terminée (tous les messages ont été produits).
     * On ne s'occupe ici que de la production, pas du fait que le buffer soit vidé.
     */
    private boolean finished() {
        return totalProduced >= expectedTotal;
    }

    @Override
    public void put(Message m) throws InterruptedException {
        lock.lock();
        try {
            // Attente tant que le buffer est plein
            while (count == buf.length)
                notFull.await();

            // Insertion du message dans le buffer circulaire
            buf[in] = m;
            in = (in + 1) % buf.length;
            count++;
            totalProduced++;

            // Réveil des consommateurs : il y a au moins un message disponible
            notEmpty.signalAll();

            // Si l'on vient de finir la production (totalProduced == expectedTotal),
            // on s'assure que tous les consommateurs en attente soient réveillés
            // pour qu'ils puissent constater la fin.
            if (finished()) {
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            // Attendre un message tant que le buffer est vide et que la production n'est
            // pas finie
            while (count == 0 && !finished())
                notEmpty.await();

            // Si le buffer est vide et que la production est finie, plus rien à lire
            if (count == 0 && finished())
                return null;

            // Lecture d'un message
            Message m = buf[out];
            buf[out] = null;
            out = (out + 1) % buf.length;
            count--;

            // Une place libre de plus pour les producteurs
            notFull.signal();
            return m;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message[] get(int k) throws InterruptedException {
        if (k <= 0)
            throw new IllegalArgumentException("k <= 0");

        lock.lock();
        try {
            ArrayList<Message> batch = new ArrayList<>(k);

            // On essaie de remplir le lot jusqu'à k messages.
            while (batch.size() < k) {
                // Tant que le buffer est vide et que la production continue, on attend.
                while (count == 0 && !finished())
                    notEmpty.await();

                // Si le buffer est vide et que la production est finie, on ne pourra pas
                // obtenir plus de messages : on sort avec ce qu'on a (éventuellement 0).
                if (count == 0 && finished())
                    break;

                // Tant qu'il y a des messages dispo et qu'on n'a pas encore k éléments,
                // on vide le buffer en FIFO dans le lot.
                while (count > 0 && batch.size() < k) {
                    Message m = buf[out];
                    buf[out] = null;
                    out = (out + 1) % buf.length;
                    count--;
                    batch.add(m);
                }

                // On a libéré des cases, on peut réveiller les producteurs.
                notFull.signalAll();

                // Si le lot est encore incomplet, deux cas :
                // - si la production est finie → on sort, on rend le lot partiel
                // - sinon → on peut attendre l'arrivée de nouveaux messages
                if (batch.size() < k) {
                    if (finished())
                        break;
                    notEmpty.await();
                }
            }

            // Conversion de la liste en tableau compact
            return batch.toArray(new Message[0]);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int nmsg() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

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
