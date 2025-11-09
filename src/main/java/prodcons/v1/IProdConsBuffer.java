package prodcons.v1;

public interface IProdConsBuffer {
    void put(Message m) throws InterruptedException;

    Message get() throws InterruptedException;

    int nmsg(); // nombre actuellement dans le buffer

    int totmsg(); // nombre total produits depuis le début

}
