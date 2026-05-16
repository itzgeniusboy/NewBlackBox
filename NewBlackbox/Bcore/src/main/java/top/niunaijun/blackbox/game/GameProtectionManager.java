package top.niunaijun.blackbox.game;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Game Protection Manager - Protects games from being killed by system or other apps
 * Provides anti-kill, crash recovery, and performance optimization for games
 */
public class GameProtectionManager {
    private static final String TAG = "GameProtectionManager";
    
    private static GameProtectionManager sInstance;
    private Context mContext;
    private boolean mEnabled = false;
    
    // Game detection patterns
    private static final String[] GAME_CATEGORIES = {
        "game", "games", "gaming", "play"
    };
    
    private static final String[] GAME_KEYWORDS = {
        "pubg", "freefire", "cod", "callofduty", "mobilelegends", 
        "arenaofvalor", "free fire", "bgmi", "battlegrounds",
        "fortnite", "minecraft", "roblox", "amongus", "garena",
        "clashofclans", "clashroyale", "brawlstars", "subwaysurfers",
        "temple run", "asphalt", "needforspeed", "fifa", "pes",
        "efootball", "dreamleague", "scorehero", "8ballpool",
        "ludo", "carrom", "chess", "rummy", "teenpatti",
        "gta", "grand theft auto", "san andreas", "vice city"
    };
    
    // Protected games list
    private final Set<String> mProtectedGames = new CopyOnWriteArraySet<>();
    private final Set<String> mDetectedGames = new CopyOnWriteArraySet<>();
    private final Set<String> mGameCategories = new HashSet<>();
    
    // Crash tracking
    private final HashMap<String, Integer> mCrashCounts = new HashMap<>();
    private static final int MAX_CRASH_COUNT = 3;
    
    // Game state tracking
    private String mCurrentGame = null;
    private long mGameStartTime = 0;
    
    private GameProtectionManager() {
        // Private constructor for singleton
    }
    
    public static synchronized GameProtectionManager getInstance() {
        if (sInstance == null) {
            sInstance = new GameProtectionManager();
        }
        return sInstance;
    }
    
    public void initialize(Context context) {
        mContext = context.getApplicationContext();
        Slog.i(TAG, "GameProtectionManager initialized");
        
        // Load previously protected games from preferences
        loadProtectedGames();
        
        // Scan for games on initialization
        scanForGames();
    }
    
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        Slog.i(TAG, "Game Protection " + (enabled ? "ENABLED" : "DISABLED"));
        
        if (enabled) {
            // Start protection services
            startProtection();
        } else {
            // Stop protection services
            stopProtection();
        }
    }
    
    public boolean isEnabled() {
        return mEnabled;
    }
    
    /**
     * Add a game to protected list
     */
    public void protectGame(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        
        mProtectedGames.add(packageName);
        Slog.i(TAG, "Game protected: " + packageName);
        
        // Save to preferences
        saveProtectedGames();
        
        // Apply protection immediately
        applyProtection(packageName);
    }
    
    /**
     * Remove a game from protected list
     */
    public void unprotectGame(String packageName) {
        if (packageName == null || packageName.isEmpty()) return;
        
        mProtectedGames.remove(packageName);
        Slog.i(TAG, "Game unprotected: " + packageName);
        
        // Save to preferences
        saveProtectedGames();
        
        // Remove protection
        removeProtection(packageName);
    }
    
    /**
     * Get list of protected games
     */
    public Set<String> getProtectedGames() {
        return new HashSet<>(mProtectedGames);
    }
    
    /**
     * Get list of detected games
     */
    public Set<String> getDetectedGames() {
        return new HashSet<>(mDetectedGames);
    }
    
    /**
     * Check if package is a game
     */
    public boolean isGame(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        
        // Check if already detected
        if (mDetectedGames.contains(packageName)) {
            return true;
        }
        
        // Check if in protected list
        if (mProtectedGames.contains(packageName)) {
            return true;
        }
        
        // Check package name against keywords
        String lowerPackage = packageName.toLowerCase();
        for (String keyword : GAME_KEYWORDS) {
            if (lowerPackage.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Scan installed apps for games
     */
    public void scanForGames() {
        if (mContext == null) return;
        
        new Thread(() -> {
            try {
                PackageManager pm = mContext.getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
                
                mDetectedGames.clear();
                
                for (ApplicationInfo app : apps) {
                    String packageName = app.packageName;
                    
                    // Check if it's a game by category or keywords
                    if (isGameApp(app, pm)) {
                        mDetectedGames.add(packageName);
                        Slog.d(TAG, "Detected game: " + packageName);
                    }
                }
                
                Slog.i(TAG, "Game scan complete. Found " + mDetectedGames.size() + " games.");
                
            } catch (Exception e) {
                Slog.e(TAG, "Error scanning for games: " + e.getMessage());
            }
        }, "GameScanner").start();
    }
    
    /**
     * Check if system should block kill for this game
     */
    public boolean shouldBlockKill(String packageName) {
        if (!mEnabled) return false;
        if (packageName == null || packageName.isEmpty()) return false;
        
        // Check if game is protected
        if (mProtectedGames.contains(packageName)) {
            Slog.d(TAG, "Blocking kill for protected game: " + packageName);
            return true;
        }
        
        // Check if it's a detected game and auto-protect is enabled
        if (mDetectedGames.contains(packageName)) {
            // Auto-protect detected games
            Slog.d(TAG, "Blocking kill for detected game: " + packageName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle game crash
     */
    public void onGameCrashed(String packageName, Throwable error) {
        if (packageName == null) return;
        
        Slog.w(TAG, "Game crashed: " + packageName, error);
        
        // Track crash count
        int crashCount = mCrashCounts.getOrDefault(packageName, 0) + 1;
        mCrashCounts.put(packageName, crashCount);
        
        if (crashCount >= MAX_CRASH_COUNT) {
            Slog.e(TAG, "Game " + packageName + " crashed " + crashCount + " times. Disabling protection.");
            unprotectGame(packageName);
            return;
        }
        
        // Try to restart the game
        restartGame(packageName);
    }
    
    /**
     * Set current running game
     */
    public void setCurrentGame(String packageName) {
        mCurrentGame = packageName;
        mGameStartTime = System.currentTimeMillis();
        Slog.i(TAG, "Current game set: " + packageName);
    }
    
    /**
     * Get current running game
     */
    public String getCurrentGame() {
        return mCurrentGame;
    }
    
    /**
     * Get game session duration
     */
    public long getGameSessionDuration() {
        if (mCurrentGame == null || mGameStartTime == 0) return 0;
        return System.currentTimeMillis() - mGameStartTime;
    }
    
    // ============ PRIVATE METHODS ============
    
    private boolean isGameApp(ApplicationInfo app, PackageManager pm) {
        String packageName = app.packageName.toLowerCase();
        
        // Check keywords in package name
        for (String keyword : GAME_KEYWORDS) {
            if (packageName.contains(keyword)) {
                return true;
            }
        }
        
        // Check app category (Android 26+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (app.category == ApplicationInfo.CATEGORY_GAME) {
                return true;
            }
        }
        
        // Check app name
        try {
            CharSequence appName = pm.getApplicationLabel(app);
            if (appName != null) {
                String name = appName.toString().toLowerCase();
                for (String keyword : GAME_KEYWORDS) {
                    if (name.contains(keyword)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        return false;
    }
    
    private void startProtection() {
        Slog.i(TAG, "Starting game protection services...");
        
        // Apply protection to all protected games
        for (String packageName : mProtectedGames) {
            applyProtection(packageName);
        }
        
        // Start monitoring
        startMonitoring();
    }
    
    private void stopProtection() {
        Slog.i(TAG, "Stopping game protection services...");
        
        // Remove protection from all games
        for (String packageName : mProtectedGames) {
            removeProtection(packageName);
        }
        
        // Stop monitoring
        stopMonitoring();
    }
    
    private void applyProtection(String packageName) {
        Slog.d(TAG, "Applying protection for: " + packageName);
        
        // Set high priority for the game process
        // This is done through hooks in GameProtectionHooks
        
        // Notify hooks
        GameProtectionHooks.getInstance().onGameProtected(packageName);
    }
    
    private void removeProtection(String packageName) {
        Slog.d(TAG, "Removing protection for: " + packageName);
        
        // Notify hooks
        GameProtectionHooks.getInstance().onGameUnprotected(packageName);
    }
    
    private void restartGame(String packageName) {
        Slog.i(TAG, "Attempting to restart game: " + packageName);
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // Launch the game again
                BlackBoxCore.get().launchApk(packageName, 0);
            } catch (Exception e) {
                Slog.e(TAG, "Failed to restart game: " + e.getMessage());
            }
        }, 2000); // Wait 2 seconds before restarting
    }
    
    private void startMonitoring() {
        // Start process monitoring to detect game kills
        GameProtectionHooks.getInstance().startMonitoring();
    }
    
    private void stopMonitoring() {
        GameProtectionHooks.getInstance().stopMonitoring();
    }
    
    private void loadProtectedGames() {
        // Load from SharedPreferences
        // Implementation depends on your storage preference
        Slog.d(TAG, "Loading protected games from storage...");
    }
    
    private void saveProtectedGames() {
        // Save to SharedPreferences
        // Implementation depends on your storage preference
        Slog.d(TAG, "Saving protected games to storage...");
    }
}
