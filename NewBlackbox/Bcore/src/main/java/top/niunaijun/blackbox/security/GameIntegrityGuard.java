package top.niunaijun.blackbox.security;

import top.niunaijun.blackbox.utils.Slog;

public class GameIntegrityGuard {
    private static final String TAG = "GameIntegrityGuard";
    private static GameIntegrityGuard sInstance;

    private volatile boolean mMonitoring;
    private Thread mMonitorThread;

    public static synchronized GameIntegrityGuard getInstance() {
        if (sInstance == null) sInstance = new GameIntegrityGuard();
        return sInstance;
    }

    public void startMonitoring(String packageName) {
        if (mMonitoring) return;
        mMonitoring = true;
        SdkProtectionManager.getInstance().onGameLaunch(packageName);
        mMonitorThread = new Thread(() -> {
            while (mMonitoring) {
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "GameIntegrityMonitor");
        mMonitorThread.start();
        Slog.i(TAG, "Monitoring started for " + packageName);
    }

    public void stopMonitoring() {
        mMonitoring = false;
        if (mMonitorThread != null) {
            mMonitorThread.interrupt();
            mMonitorThread = null;
        }
        Slog.i(TAG, "Monitoring stopped");
    }
}
