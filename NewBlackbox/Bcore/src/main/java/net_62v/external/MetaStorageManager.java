package net_62v.external;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import org.lsposed.lsparanoid.Obfuscate;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.utils.FileUtils;


@Obfuscate
public class MetaStorageManager {
    private static final String TAG = "MetaStorageManager";

    public static File obtainAppExternalStorageDir() {
        File baseDir = BlackBoxCore.getContext().getExternalFilesDir(null);
        if (baseDir == null) {
            baseDir = BlackBoxCore.getContext().getFilesDir();
        }
        FileUtils.mkdirs(baseDir);
        return baseDir;
    }

    public static File getObbContainerPath(String packageName) {
        try {
            File base = obtainAppExternalStorageDir();
            File obbDir = new File(base, "Android/obb/" + packageName);
            FileUtils.mkdirs(obbDir);
            return obbDir;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get OBB container path for " + packageName, e);
            throw new RuntimeException(e);
        }
    }

    public static File getDataContainerPath(String packageName) {
        try {
            File base = obtainAppExternalStorageDir();
            File dataDir = new File(base, "Android/data/" + packageName);
            FileUtils.mkdirs(dataDir);
            return dataDir;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get data container path for " + packageName, e);
            throw new RuntimeException(e);
        }
    }
}