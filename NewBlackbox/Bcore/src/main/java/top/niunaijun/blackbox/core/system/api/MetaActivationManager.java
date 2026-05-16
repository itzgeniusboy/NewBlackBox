package top.niunaijun.blackbox.core.system.api;

import android.MetaCore.RemoteManager;
import org.lsposed.lsparanoid.Obfuscate;

/**
 * 
 */
@Obfuscate
public class MetaActivationManager {
    public static void activateSdk(String userkey) {
        try {
            RemoteManager.getInstance().activateSdk(userkey);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static String getServerMessage() {
        try {
            return RemoteManager.getInstance().getServerMessage();
        } catch (Throwable e) {
            e.printStackTrace();
            return "Error getting server message";
        }
    }

    public static boolean getActivatedStatus() {
        try {
            return RemoteManager.getInstance().getActivatedSdk();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
}