package prodcons.v6;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.GroupLayout.Group;

public class ProdConsBuffer implements IProdConsBuffer {
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private final Deque<Group> q = new ArrayDeque<>();
    private final int capacity; // en GROUPES
    private int countGroups = 0, totGroups = 0;

    final class Group {
        final Message m;
        final int n; // exemplaires à consommer
        int taken = 0; // exemplaires déjà pris
        boolean done = false; // vrai quand taken == n
        final Condition allTaken; // barrière : réveille producteur + consommateurs

        Group(Message m, int n, Condition allTaken) {
            this.m = m;
            this.n = n;
            this.allTaken = allTaken;
        }
    }

    public ProdConsBuffer(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void put(Message m, int n) throws InterruptedException {
        if (n <= 0)
            throw new IllegalArgumentException("n <= 0");
        lock.lock();
        try {
            while (countGroups == capacity)
                notFull.await();
            Group g = new Group(m, n, lock.newCondition());
            q.addLast(g);
            countGroups++;
            totGroups++;
            notEmpty.signal();
            while (!g.done)
                g.allTaken.await(); // barrière producteur
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Message get() throws InterruptedException {
        lock.lock();
        try {
            while (countGroups == 0)
                notEmpty.await();
            Group g = q.peekFirst(); // FIFO par groupe

            g.taken++;
            if (g.taken == g.n) { // dernier consommateur
                g.done = true;
                q.removeFirst();
                countGroups--;
                g.allTaken.signalAll(); // libère producteur + consommateurs de ce groupe
                notFull.signal(); // libère un producteur bloqué sur place buffer
            } else {
                while (!g.done)
                    g.allTaken.await(); // barrière consommateurs
            }
            return g.m;
        } finally {
            lock.unlock();
        }
    }

    // stats
    public int nmsg() {
        lock.lock();
        try {
            return countGroups;
        } finally {
            lock.unlock();
        }
    }

    public int totmsg() {
        lock.lock();
        try {
            return totGroups;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(Message m) throws InterruptedException {
        put(m,1);
    }

    @Override
    public Message[] get(int k) throws InterruptedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }
}
