package prodcons.v1;


public class Consumer extends Thread {
    private final IProdConsBuffer buffer;
    private final int consTimeMs;

    public Consumer(IProdConsBuffer buffer, int consTimeMs) {
        this.buffer = buffer;
        this.consTimeMs = consTimeMs;
    }

    @Override
    public void run() {
        while (true) { // v1 : pas de terminaison auto
            try {
                Message m = buffer.get();
                // System.out.println(getName() + " got " + m + " nmsg=" + buffer.nmsg());
                Log.info("%s got %-6s  (nmsg=%d, tot=%d)", getName(), m, buffer.nmsg(), buffer.totmsg());
                Thread.sleep(consTimeMs);
            } catch (InterruptedException e) {
                Log.info("%s interrupted", getName());
                return;
            }
        }
    }
}
