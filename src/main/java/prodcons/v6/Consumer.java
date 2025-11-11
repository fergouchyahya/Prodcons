package prodcons.v6;

public class Consumer extends Thread {
    private final IProdConsBuffer buffer;
    private final int consTimeMs;

    public Consumer(int cid, IProdConsBuffer buffer, int consTimeMs) {
        super("C-" + cid);
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;

    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Message m = buffer.get();
                Thread.sleep(consTimeMs);

            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
