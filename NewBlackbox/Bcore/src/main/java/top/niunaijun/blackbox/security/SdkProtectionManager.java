package top.niunaijun.blackbox.security;

import android.content.Context;

import top.niunaijun.blackbox.utils.Slog;

public class SdkProtectionManager {
    private static final String TAG = "SdkProtectionManager";

    private static final String[] SECURITY_SDK_LIBS = {
            "libanogs.so", "libanort.so", "libTBlueData.so", "libBugly.so",
            "libmsaoaidsec.so", "libtup.so", "libtsssdk.so", "libtersafe.so",
            "libstaysafe.so", "libsecuritysdk.so", "libsgmain.so", "libsgsecuritybody.so",
            "libmobisec.so", "libxguardian.so", "libantibot.so", "libprotect.so", "libsafe.so"
    };

    private static final String[] CONTAINER_PATHS = {
            "/blackbox/", "/BlackBox/", "/virtual/", "/parallel/", "/dual/",
            "/clone/", "/sandbox/", "/niunaijun/", "/bcore/", "/vbox/"
    };

    private static SdkProtectionManager sInstance;
    private boolean mEnabled;

    public static synchronized SdkProtectionManager getInstance() {
        if (sInstance == null) sInstance = new SdkProtectionManager();
        return sInstance;
    }

    public void initialize(Context context) {
        Slog.i(TAG, "Initialized");
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        Slog.i(TAG, "setEnabled=" + enabled);
        if (enabled) {
            mediateLibraryLoading();
            ensureSignalCompatibility();
            virtualizePaths();
        }
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void onGameLaunch(String packageName) {
        Slog.i(TAG, "onGameLaunch: " + packageName);
        if (mEnabled) {
            mediateLibraryLoading();
        }
    }

    public boolean isSecuritySdk(String libName) {
        if (libName == null) return false;
        for (String item : SECURITY_SDK_LIBS) {
            if (libName.contains(item)) return true;
        }
        return false;
    }

    public boolean containsContainerPath(String path) {
        if (path == null) return false;
        for (String item : CONTAINER_PATHS) {
            if (path.contains(item)) return true;
        }
        return false;
    }

    private void mediateLibraryLoading() {
        try {
            mediateLibraryLoadingNative();
        } catch (UnsatisfiedLinkError e) {
            Slog.w(TAG, "Native mediate unavailable");
        }
    }

    private void ensureSignalCompatibility() {
        try {
            ensureSignalCompatibilityNative();
        } catch (UnsatisfiedLinkError e) {
            Slog.w(TAG, "Native signal compatibility unavailable");
        }
    }

    private void virtualizePaths() {
        try {
            virtualizePathsNative();
        } catch (UnsatisfiedLinkError e) {
            Slog.w(TAG, "Native path virtualization unavailable");
        }
    }

    private native void mediateLibraryLoadingNative();
    private native void ensureSignalCompatibilityNative();
    private native void virtualizePathsNative();
}
