package prodcons.v5;

public interface IProdConsBuffer {
    /** Put the message m in the prodcons buffer (FIFO). */
    void put(Message m) throws InterruptedException;

    /**
     * Retrieve one message (FIFO). Peut renvoyer null si fin de prod et tampon
     * vide.
     */
    Message get() throws InterruptedException;

    /**
     * Retrieve k consecutive messages (FIFO). Peut renvoyer un lot partiel si fin
     * de prod.
     */
    Message[] get(int k) throws InterruptedException;

    /** Nombre actuellement dans le buffer. */
    int nmsg();

    /** Nombre total produits depuis le début. */
    int totmsg();

}
