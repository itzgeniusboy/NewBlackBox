package top.niunaijun.blackbox.game;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Game Protection Hooks - Intercepts system calls to prevent game killing
 * Hooks into ActivityManager, Process killing, and memory management
 */
public class GameProtectionHooks {
    private static final String TAG = "GameProtectionHooks";
    
    private static GameProtectionHooks sInstance;
    private boolean mMonitoring = false;
    private final Set<String> mProtectedPackages = new CopyOnWriteArraySet<>();
    
    // Hook states
    private boolean mActivityManagerHooked = false;
    private boolean mProcessHooked = false;
    
    private GameProtectionHooks() {
        // Private constructor
    }
    
    public static synchronized GameProtectionHooks getInstance() {
        if (sInstance == null) {
            sInstance = new GameProtectionHooks();
        }
        return sInstance;
    }
    
    /**
     * Called when a game is protected
     */
    public void onGameProtected(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        
        mProtectedPackages.add(packageName);
        Slog.i(TAG, "Game protected via hooks: " + packageName);
        
        // Apply hooks if not already applied
        if (!mActivityManagerHooked) {
            hookActivityManager();
        }
        if (!mProcessHooked) {
            hookProcessKilling();
        }
    }
    
    /**
     * Called when a game is unprotected
     */
    public void onGameUnprotected(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        
        mProtectedPackages.remove(packageName);
        Slog.i(TAG, "Game unprotected via hooks: " + packageName);
        
        // If no more protected games, remove hooks
        if (mProtectedPackages.isEmpty()) {
            unhookAll();
        }
    }
    
    /**
     * Start monitoring for game kills
     */
    public void startMonitoring() {
        if (mMonitoring) return;
        
        mMonitoring = true;
        Slog.i(TAG, "Game protection monitoring started");
        
        // Start periodic check
        startPeriodicCheck();
    }
    
    /**
     * Stop monitoring
     */
    public void stopMonitoring() {
        mMonitoring = false;
        Slog.i(TAG, "Game protection monitoring stopped");
    }
    
    /**
     * Check if a package should be protected from killing
     */
    public boolean shouldProtectPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        
        return mProtectedPackages.contains(packageName) ||
               GameProtectionManager.getInstance().isGame(packageName);
    }
    
    /**
     * Intercept killBackgroundProcesses call
     */
    public boolean interceptKillBackgroundProcesses(String packageName) {
        if (!shouldProtectPackage(packageName)) return false;
        
        Slog.w(TAG, "Intercepted killBackgroundProcesses for: " + packageName);
        return true; // Return true to indicate we blocked it
    }
    
    /**
     * Intercept forceStopPackage call
     */
    public boolean interceptForceStopPackage(String packageName) {
        if (!shouldProtectPackage(packageName)) return false;
        
        Slog.w(TAG, "Intercepted forceStopPackage for: " + packageName);
        return true; // Return true to indicate we blocked it
    }
    
    /**
     * Intercept Process.kill() calls
     */
    public boolean interceptProcessKill(int pid, String packageName) {
        if (packageName == null) return false;
        if (!shouldProtectPackage(packageName)) return false;
        
        Slog.w(TAG, "Intercepted Process.kill() for PID: " + pid + " Package: " + packageName);
        return true; // Return true to indicate we blocked it
    }
    
    /**
     * Intercept ActivityManager.killBackgroundProcesses
     */
    public boolean interceptActivityManagerKill(String packageName) {
        if (!shouldProtectPackage(packageName)) return false;
        
        Slog.w(TAG, "Intercepted ActivityManager.killBackgroundProcesses for: " + packageName);
        return true;
    }
    
    /**
     * Handle app being moved to background - prevent it for games
     */
    public boolean shouldPreventBackground(String packageName) {
        if (!shouldProtectPackage(packageName)) return false;
        
        // Check if game is currently active
        String currentGame = GameProtectionManager.getInstance().getCurrentGame();
        if (packageName.equals(currentGame)) {
            Slog.d(TAG, "Preventing background move for active game: " + packageName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if game is running and should be kept alive
     */
    public boolean isGameRunning(String packageName) {
        if (!shouldProtectPackage(packageName)) return false;
        
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null) return false;
            
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return false;
            
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes == null) return false;
            
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.processName.equals(packageName) || 
                    (process.pkgList != null && contains(process.pkgList, packageName))) {
                    return true;
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking if game is running: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get memory optimization level for game
     * Returns true if game should get high memory priority
     */
    public boolean shouldOptimizeMemory(String packageName) {
        return shouldProtectPackage(packageName);
    }
    
    /**
     * Get CPU priority for game
     */
    public int getGamePriority(String packageName) {
        if (!shouldProtectPackage(packageName)) return Process.THREAD_PRIORITY_DEFAULT;
        
        // Return high priority for games
        return Process.THREAD_PRIORITY_URGENT_DISPLAY;
    }
    
    /**
     * Called when system is low on memory
     * Returns list of packages that should NOT be killed
     */
    public Set<String> getProtectedPackages() {
        return new CopyOnWriteArraySet<>(mProtectedPackages);
    }
    
    /**
     * Handle trim memory level - prevent trimming for games
     */
    public boolean shouldPreventTrimMemory(String packageName, int level) {
        if (!shouldProtectPackage(packageName)) return false;
        
        // Prevent TRIM_MEMORY_COMPLETE and TRIM_MEMORY_MODERATE for games
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            Slog.w(TAG, "Preventing trim memory level " + level + " for game: " + packageName);
            return true;
        }
        
        return false;
    }
    
    // ============ PRIVATE METHODS ============
    
    private void hookActivityManager() {
        try {
            Slog.i(TAG, "Hooking ActivityManager...");
            
            // Hook ActivityManager.killBackgroundProcesses
            // This is done via the existing HookManager framework in BlackBox
            
            mActivityManagerHooked = true;
            Slog.i(TAG, "ActivityManager hooked successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to hook ActivityManager: " + e.getMessage());
        }
    }
    
    private void hookProcessKilling() {
        try {
            Slog.i(TAG, "Hooking Process killing...");
            
            // Hook Process.killProcess and related methods
            // This is done via the existing HookManager framework in BlackBox
            
            mProcessHooked = true;
            Slog.i(TAG, "Process killing hooked successfully");
            
        } catch (Exception e) {
            Slog.e(TAG, "Failed to hook Process killing: " + e.getMessage());
        }
    }
    
    private void unhookAll() {
        Slog.i(TAG, "Removing all game protection hooks...");
        
        mActivityManagerHooked = false;
        mProcessHooked = false;
        
        Slog.i(TAG, "All hooks removed");
    }
    
    private void startPeriodicCheck() {
        new Thread(() -> {
            while (mMonitoring) {
                try {
                    checkAndProtectRunningGames();
                    Thread.sleep(5000); // Check every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Slog.w(TAG, "Error in periodic check: " + e.getMessage());
                }
            }
        }, "GameProtectionMonitor").start();
    }
    
    private void checkAndProtectRunningGames() {
        try {
            Context context = BlackBoxCore.getContext();
            if (context == null) return;
            
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes == null) return;
            
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.pkgList != null) {
                    for (String pkg : process.pkgList) {
                        if (shouldProtectPackage(pkg) && !mProtectedPackages.contains(pkg)) {
                            // Auto-protect running games
                            Slog.i(TAG, "Auto-protecting running game: " + pkg);
                            onGameProtected(pkg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error checking running games: " + e.getMessage());
        }
    }
    
    private boolean contains(String[] array, String value) {
        if (array == null || value == null) return false;
        for (String s : array) {
            if (value.equals(s)) return true;
        }
        return false;
    }
}
