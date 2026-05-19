package top.niunaijun.blackbox.core;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import top.niunaijun.blackbox.utils.Slog;

/**
 * Centralized compatibility helper for authentication-related ecosystems
 * (Google Play Services / GSF / Play Store).
 */
public final class AuthCompatibilityCore {
    private static final String TAG = "AuthCompatibilityCore";

    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";

    private AuthCompatibilityCore() {
    }

    public static boolean isAuthSensitivePackage(String packageName) {
        if (packageName == null) {
            return false;
        }
        return packageName.equals(GMS_PKG)
                || packageName.equals(GSF_PKG)
                || packageName.equals(VENDING_PKG)
                || packageName.startsWith("com.google.android.gms.");
    }

    public static boolean isAuthSensitiveIntent(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (intent.getComponent() != null && isAuthSensitivePackage(intent.getComponent().getPackageName())) {
            return true;
        }
        final String action = intent.getAction();
        if (action == null) {
            return false;
        }
        return action.contains("gms")
                || action.contains("measurement")
                || action.contains("signin");
    }

    public static PackageInfo createCompatPackageInfo(String packageName) {
        if (VENDING_PKG.equals(packageName)) {
            return createPlayStorePackageInfo();
        }
        if (GMS_PKG.equals(packageName) || GSF_PKG.equals(packageName)) {
            return createGooglePlayServicesPackageInfo(packageName);
        }
        return null;
    }

    private static PackageInfo createPlayStorePackageInfo() {
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = VENDING_PKG;
        packageInfo.versionName = "33.8.16-21";
        packageInfo.versionCode = 83381621;
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = VENDING_PKG;
        appInfo.name = "Google Play Store";
        appInfo.flags = ApplicationInfo.FLAG_SYSTEM;
        appInfo.uid = 10001;
        packageInfo.applicationInfo = appInfo;
        Slog.d(TAG, "Providing compatibility PackageInfo for Play Store");
        return packageInfo;
    }

    private static PackageInfo createGooglePlayServicesPackageInfo(String packageName) {
        final PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = packageName;
        packageInfo.versionCode = 250505301;
        packageInfo.versionName = "25.05.53";
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.packageName = packageName;
        appInfo.enabled = true;
        packageInfo.applicationInfo = appInfo;
        Slog.d(TAG, "Providing compatibility PackageInfo for " + packageName);
        return packageInfo;
    }

}
