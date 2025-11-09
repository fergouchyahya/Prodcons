package prodcons.v1;

import java.util.Date;

public final class Log {
    public static volatile boolean ENABLED = true;  // coupe/allume les logs

    public static void info(String fmt, Object... args) {
        if (!ENABLED) return;
        synchronized (System.out) { // lignes non-entrelacées
            System.out.printf("[%tT] %s%n", new Date(), String.format(fmt, args));
        }
    }
    private Log() {}
}

