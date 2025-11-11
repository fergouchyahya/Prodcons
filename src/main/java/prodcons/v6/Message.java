package prodcons.v6;

public class Message {
    final public int id;
    final public long producerTid;

    public Message(int id, long producerTid) {
        this.id = id;
        this.producerTid = producerTid;
    }

    @Override
    public String toString() {
        return "M#" + id + "(P" + producerTid + ")";
    }

}