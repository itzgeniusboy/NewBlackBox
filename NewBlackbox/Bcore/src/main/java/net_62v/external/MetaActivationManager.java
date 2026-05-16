package net_62v.external;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.lsposed.lsparanoid.Obfuscate;

import android.MetaCore.PermissionManager;
import android.MetaCore.RemoteManager;
import android.MetaCore.nk;

@Obfuscate
public final class MetaActivationManager {

    private static final String TAG = "MetaActivationManager";
    private static final int POLL_ATTEMPTS = 10;
    private static final long POLL_DELAY_MS = 500;

    public interface ActivationCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionDenied(String reason);
    }

    private MetaActivationManager() {}

    // ----------------------------------------------------------------------
    // Permission handling
    // ----------------------------------------------------------------------

    public static void checkPermissions(Activity activity, final PermissionCallback callback) {
        PermissionManager.checkAndRequestAllPermissions(activity, new PermissionManager.PermissionCallback() {
            @Override
            public void onAllPermissionsGranted() {
                callback.onPermissionsGranted();
            }

            @Override
            public void onPermissionFailed(String reason) {
                callback.onPermissionDenied(reason);
            }

            @Override
            public void onActivationResult(boolean success, String message) {}
        });
    }

    public static void handleRequestPermissionsResult(Activity activity, int requestCode,
                                                      String[] permissions, int[] grantResults,
                                                      final PermissionCallback callback) {
        PermissionManager.handleRequestPermissionsResult(activity, requestCode, permissions, grantResults,
                new PermissionManager.PermissionCallback() {
                    @Override
                    public void onAllPermissionsGranted() {
                        callback.onPermissionsGranted();
                    }

                    @Override
                    public void onPermissionFailed(String reason) {
                        callback.onPermissionDenied(reason);
                    }

                    @Override
                    public void onActivationResult(boolean success, String message) {}
                });
    }

    public static void handleActivityResult(Activity activity, int requestCode, int resultCode,
                                            Intent data, final PermissionCallback callback) {
        PermissionManager.handleActivityResult(activity, requestCode, resultCode, data,
                new PermissionManager.PermissionCallback() {
                    @Override
                    public void onAllPermissionsGranted() {
                        callback.onPermissionsGranted();
                    }

                    @Override
                    public void onPermissionFailed(String reason) {
                        callback.onPermissionDenied(reason);
                    }

                    @Override
                    public void onActivationResult(boolean success, String message) {}
                });
    }

    // ----------------------------------------------------------------------
    // Activation methods (with and without callback)
    // ----------------------------------------------------------------------

    /**
     * Activate SDK with a callback to receive the result.
     * The callback is always invoked on the main thread.
     */
    public static void activateSdk(String userKey, final ActivationCallback callback) {
        if (userKey == null || userKey.trim().isEmpty()) {
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure("User key is empty"));
            }
            return;
        }

        RemoteManager.getInstance().activateSdk(userKey);

        if (callback == null) return;

        // Poll for completion using RemoteManager's message
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            int attempts = 0;
            @Override
            public void run() {
                String msg = RemoteManager.getInstance().getServerMessage();
                boolean isSuccess = msg != null && (msg.contains("successful") || msg.contains("Activated"));
                boolean isFailure = msg != null && (msg.contains("fail") || msg.contains("error") || msg.contains("invalid"));

                if (isSuccess) {
                    callback.onSuccess(msg);
                } else if (isFailure) {
                    callback.onFailure(msg);
                } else {
                    attempts++;
                    if (attempts < POLL_ATTEMPTS) {
                        handler.postDelayed(this, POLL_DELAY_MS);
                    } else {
                        callback.onFailure("Activation timeout");
                    }
                }
            }
        }, POLL_DELAY_MS);
    }

    /**
     * Activate SDK without a callback (fire and forget).
     * This overload exists for backward compatibility.
     */
    public static void activateSdk(String userKey) {
        activateSdk(userKey, null);
    }

    // ----------------------------------------------------------------------
    // Status queries
    // ----------------------------------------------------------------------

    public static boolean isActivated() {
        return RemoteManager.getInstance().getActivatedSdk();
    }

    public static String getServerMessage() {
        return RemoteManager.getInstance().getServerMessage();
    }

    public static String getLicenseMessage() {
        return nk.getServerMessage();
    }
}