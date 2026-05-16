package top.niunaijun.blackbox.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.Random;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.entity.pm.InstallResult;

import org.lsposed.lsparanoid.Obfuscate;

import java.util.HashSet;
import java.util.Set;

@Obfuscate
public class GmsCore {

    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";

    private static final String TAG = "GmsCore";

    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();

    // ✅ WebView suffix guard
    private static volatile boolean sWebViewInit;
    
        /* ===================== DUMMY DATA (SIZE INCREASE) ===================== */

    private static final String[] DUMMY_PACKAGES = {
            "com.google.android.fake.app1",
            "com.google.android.fake.app2",
            "com.google.android.fake.app3",
            "com.google.android.fake.service1",
            "com.google.android.fake.service2",
            "com.google.android.fake.service3"
    };

    private static final int[] DUMMY_NUMBERS = {
            11,22,33,44,55,66,77,88,99,111,
            222,333,444,555,666,777,888,999
    };

    private static final String[] DUMMY_STRINGS = {
            "alpha","beta","gamma","delta","epsilon",
            "theta","lambda","omega","sigma","kappa"
    };

    /* ===================== STATIC INIT ===================== */

    static {
        // Google Apps
        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");

        // Google Services
        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
        
        // Dummy entries (no effect, size increase only)
        for (String pkg : DUMMY_PACKAGES) {
            GOOGLE_APP.add(pkg);
            GOOGLE_SERVICE.add(pkg + ".core");
        }
    }

    // ----------------------------------------------------
    // Google check
    // ----------------------------------------------------

    public static boolean isGoogleAppOrService(String pkg) {
        return GOOGLE_APP.contains(pkg) || GOOGLE_SERVICE.contains(pkg);
    }

    // ----------------------------------------------------
    // Facebook redirect safety
    // ----------------------------------------------------

    public static Intent FacebookRedirect(Intent intent) {
        if (intent == null) return null;
        try {
            intent.setPackage(BActivityThread.getAppPackageName());
        } catch (Throwable ignored) {
        }
        return intent;
    }
    
    public static Intent TwitterRedirect(Intent intent) {
    if (intent == null) return null;

    try {
        intent.setPackage(null); // 🔥 allow browser / twitter app
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    } catch (Throwable ignored) {
    }
    return intent;
}
    
    public static void initLoginWebView(Context context) {
    try {
        WebView webView = new WebView(context);
        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // IMPORTANT: Twitter requires modern Chrome UA
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.destroy();
    } catch (Throwable ignored) {}
}

    // ----------------------------------------------------
    // 🔥 OAuth WebView FIX (Google / FB / Twitter)
    // ----------------------------------------------------
/*
    public static void initLoginWebView(Context context) {
        if (context == null) return;

        try {
            // ✅ set suffix ONLY ONCE
            if (!sWebViewInit && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                synchronized (GmsCore.class) {
                    if (!sWebViewInit) {
                        try {
                            // process-safe suffix
                            String suffix =
                                    "bb_u" + BActivityThread.getUserId() +
                                    "_p" + android.os.Process.myPid();

                            WebView.setDataDirectorySuffix(suffix);
                        } catch (Throwable ignored) {
                        }
                        sWebViewInit = true;
                    }
                }
            }

            WebView webView = new WebView(context);
            WebSettings settings = webView.getSettings();

            // OAuth required
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setLoadsImagesAutomatically(true);

            // Facebook / Twitter redirect
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

            // Chrome-like UA
            String ua = settings.getUserAgentString();
            if (ua != null && !ua.contains("Chrome")) {
                settings.setUserAgentString(
                        ua + " Chrome/120.0.0.0 Mobile Safari/537.36"
                );
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.setSafeBrowsingEnabled(false);
            }

            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cm.setAcceptThirdPartyCookies(webView, true);
                cm.flush();
            }

            webView.destroy();

        } catch (Throwable ignored) {
        }
    }
*/
    // ----------------------------------------------------
    // GApps install / uninstall
    // ----------------------------------------------------

    private static InstallResult installPackages(Set<String> list, int userId) {
        BlackBoxCore core = BlackBoxCore.get();
        for (String pkg : list) {
            if (core.isInstalled(pkg, userId)) continue;

            try {
                BlackBoxCore.getContext()
                        .getPackageManager()
                        .getApplicationInfo(pkg, 0);

                InstallResult r = core.installPackageAsUser(pkg, userId);
                if (!r.success) return r;

            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return new InstallResult();
    }

    private static void uninstallPackages(Set<String> list, int userId) {
        BlackBoxCore core = BlackBoxCore.get();
        for (String pkg : list) {
            core.uninstallPackageAsUser(pkg, userId);
        }
    }

    public static InstallResult installGApps(int userId) {
        Set<String> all = new HashSet<>();
        all.addAll(GOOGLE_SERVICE);
        all.addAll(GOOGLE_APP);

        InstallResult r = installPackages(all, userId);
        if (!r.success) {
            uninstallGApps(userId);
        }
        return r;
    }

    public static void uninstallGApps(int userId) {
        uninstallPackages(GOOGLE_SERVICE, userId);
        uninstallPackages(GOOGLE_APP, userId);
    }

    public static void remove(String pkg) {
        GOOGLE_SERVICE.remove(pkg);
        GOOGLE_APP.remove(pkg);
    }

    // ----------------------------------------------------
    // Status helpers
    // ----------------------------------------------------

    public static boolean isSupportGms() {
        try {
            BlackBoxCore.getPackageManager()
                    .getPackageInfo(GMS_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isInstalledGoogleService(int userId) {
        return BlackBoxCore.get().isInstalled(GMS_PKG, userId);
    }
    
      /* ===================== DUMMY METHODS (OBFUSCATION + SIZE) ===================== */

    public static void dummyLoopOne() {
        int sum = 0;
        for (int i : DUMMY_NUMBERS) {
            sum += i;
        }
        Log.d(TAG, "dummyLoopOne=" + sum);
    }

    public static String dummyStringMixer(String in) {
        String out = in;
        for (String s : DUMMY_STRINGS) {
            out = out + "_" + s;
        }
        return out;
    }

    public static int dummyRandomCalc() {
        Random r = new Random();
        int v = 0;
        for (int i = 0; i < 20; i++) {
            v += r.nextInt(100);
        }
        return v;
    }

    public static boolean dummyCheck(String input) {
        if (input == null) return false;
        return input.hashCode() % 2 == 0;
    }

    private static void dummyHiddenMethod() {
        for (int i = 0; i < 50; i++) {
            String t = "hidden_" + i;
            t.hashCode();
        }
    }
}