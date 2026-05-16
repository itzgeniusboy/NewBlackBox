package com.onecore.loader.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.onecore.loader.floating.FloatAim;
import com.onecore.loader.floating.FloatLogo;
import com.onecore.loader.floating.Overlay;
import com.onecore.loader.libhelper.DownloadZip;
import com.onecore.loader.utils.CrashHandler;
import com.onecore.loader.utils.Prefs;
import com.Jagdish.tastytoast.TastyToast;
import com.onecore.loader.BoxApplication;
import com.onecore.loader.libhelper.ApkEnv;
import com.onecore.loader.libhelper.FileCopyTask;
import com.onecore.loader.utils.Constants;
import com.onecore.loader.utils.FLog;
import java.io.File;
import java.io.InputStream;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import static com.onecore.loader.Config.GAME_LIST_PKG;
import com.onecore.loader.R;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class MainActivity extends Activity {

    public static MainActivity instance;
    private BlackBoxCore blackBoxCore;
    private InstallResult installResult;
    private SharedPreferences sharedPreferences;
    public static native String TimeExpired();
    public static native String FixCrash();
    public String CURRENT_PACKAGE;
    private TextView installIndia, btnStartGame;
    private View rootView;
    private Prefs prefs;
    private static final String PREF_THEME = "loader_theme";
    
    public static int gameType = 0;
    private boolean isGameLaunched = false;
    private String selectedGamePkg = "";
    private boolean isIndiaSelected = false;
    
    public static MainActivity get() {
        return instance;
    }
    
    public static void goMain(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        instance = this;
        blackBoxCore = BlackBoxCore.get();
        blackBoxCore.doCreate();
        countDownStart();
        GameJsonMods();
        sharedPreferences = getSharedPreferences(getPackageName(), Activity.MODE_PRIVATE);
        prefs = new Prefs(this);
        CheckFloatViewPermission();
        
        rootView = findViewById(R.id.main_root);

        View settingsButton = findViewById(R.id.btn_settings);
        settingsButton.setOnClickListener(v -> showThemePicker());

        selectedGamePkg = GAME_LIST_PKG[0];
        gameType = 5;
        isIndiaSelected = true;
        
        // Find Views
        installIndia = findViewById(R.id.installIndia);
        btnStartGame = findViewById(R.id.btn_start_game);
        applySelectedTheme();
        
        // Update Install Button State
        updateButtonState(0, installIndia);
        
        // Install button click listener
        installIndia.setOnClickListener(view -> handleInstallUninstall(0, installIndia));

        // Start Game button click listener
        btnStartGame.setOnClickListener(v -> {
            if (!ApkEnv.getInstance().isInstalled(selectedGamePkg)) {
                BoxApplication.get().showToastWithImage(Constants.GAME_NOT_INSTALL, TastyToast.ERROR);
                return;
            }

            do_Lib_And_Run(selectedGamePkg);
            startPatcher();
        });
        
        // Start download - DownloadZip will show its own animation and dialog
        // No need to show any toast here as DownloadZip handles it
        new DownloadZip(MainActivity.get()).startDownload(FixCrash(), new DownloadZip.DownloadCallback() {
            @Override
            public void onStart() {
                // DownloadZip shows its own animation
            }
            @Override
            public void onProgress(int progress) {
                // Progress is handled in DownloadZip animation
            }
            @Override
            public void onSuccess() {
                // Don't show toast - DownloadZip already shows success dialog
                // You can add any additional logic here if needed
            }
            @Override
            public void onError(String error) {
                // Don't show toast - DownloadZip already shows error dialog
                // You can add any additional logic here if needed
            }
        });
    }

    private void showThemePicker() {
        final String[] themeNames = new String[]{"Neon Blue", "Cyber Purple", "Emerald Tech", "Amber Elite"};
        int selected = prefs.getInt(PREF_THEME, 0);
        new AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setSingleChoiceItems(themeNames, selected, (dialog, which) -> {
                    prefs.setInt(PREF_THEME, which);
                    applySelectedTheme();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applySelectedTheme() {
        int themeIndex = prefs.getInt(PREF_THEME, 0);
        int accent;
        int accentSoft;
        switch (themeIndex) {
            case 1:
                accent = Color.parseColor("#9D4DFF");
                accentSoft = Color.parseColor("#CEB2FF");
                break;
            case 2:
                accent = Color.parseColor("#14E6A3");
                accentSoft = Color.parseColor("#9FF8DD");
                break;
            case 3:
                accent = Color.parseColor("#FFB347");
                accentSoft = Color.parseColor("#FFD79A");
                break;
            case 0:
            default:
                accent = Color.parseColor("#4DB8FF");
                accentSoft = Color.parseColor("#9AB4FF");
                break;
        }

        if (rootView != null) rootView.setBackgroundColor(Color.parseColor("#000000"));
        tintText(R.id.PremiumFileManager, accent);
        tintText(R.id.tv_d, accent);
        tintText(R.id.tv_h, accent);
        tintText(R.id.tv_m, accent);
        tintText(R.id.tv_s, accent);
        tintText(R.id.IndiaVersion, accentSoft);
        tintText(R.id.tv_welcome, accentSoft);
        tintText(R.id.tv_announcement, accent);
        tintText(R.id.tv_announcement2, accentSoft);
        tintButtonBackground(R.id.installIndia, accent);
        tintButtonBackground(R.id.btn_start_game, accent);
    }

    private void tintText(int id, int color) {
        TextView view = findViewById(id);
        if (view != null) view.setTextColor(color);
    }

    private void tintButtonBackground(int id, int color) {
        View view = findViewById(id);
        if (view == null || view.getBackground() == null) return;
        view.getBackground().mutate().setTint(color);
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(Color.parseColor("#EAF7FF"));
        }
    }
    
    public void do_Lib_And_Run(String packageName) {
        CURRENT_PACKAGE = packageName;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            File loaderFile = new File(getFilesDir(), "loader/libbgmi.so");
            if (!loaderFile.exists()) {
                BoxApplication.get().showToastWithImage("Loader missing: files/loader/libbgmi.so (wait for Saved.zip extraction)", TastyToast.ERROR);
                return;
            }

            boolean loaderReady = ApkEnv.getInstance().tryAddLoader(packageName);
            if (!loaderReady) {
                BoxApplication.get().showToastWithImage("Loader setup failed, check logs", TastyToast.ERROR);
                return;
            }
            ApkEnv.getInstance().LaunchApplication(packageName);
        });
    }
    
    private void handleInstallUninstall(final int gameIndex, final TextView installButton) {
        final String packageName = GAME_LIST_PKG[gameIndex];
        final FileCopyTask fileCopyTask = new FileCopyTask(MainActivity.get());

        boolean isInstalled = getInstallationStatus(packageName);
        android.util.Log.d("OBBCopy", "handleInstallUninstall: pkg=" + packageName + ", isInstalled=" + isInstalled);

        if (isInstalled) {
            ApkEnv.getInstance().unInstallApp(packageName);
            installButton.setText("INSTALL");
            saveInstallationStatus(packageName, false);
            BoxApplication.get().showToastWithImage(Constants.UNINSTALL_SUCCESS, TastyToast.SUCCESS);
        } else {
            // FileCopyTask will show its own animation and dialog
            if (fileCopyTask.isObbCopied(packageName)) {
                if (ApkEnv.getInstance().installByPackage(packageName)) {
                    installButton.setText("UNINSTALL");
                    saveInstallationStatus(packageName, true);
                    BoxApplication.get().showToastWithImage(Constants.INSTALL_SUCCESS, TastyToast.SUCCESS);
                } else {
                    BoxApplication.get().showToastWithImage(Constants.MSG_ERROR, TastyToast.WARNING);
                }
            } else {
                fileCopyTask.copyObbFolderAsync(packageName, new FileCopyTask.CopyCallback() {
                    @Override
                    public void onCopyCompleted(boolean copySuccess) {
                        if (copySuccess) {
                            if (ApkEnv.getInstance().installByPackage(packageName)) {
                                installButton.setText("UNINSTALL");
                                saveInstallationStatus(packageName, true);
                                BoxApplication.get().showToastWithImage(Constants.INSTALL_SUCCESS, TastyToast.SUCCESS);
                            } else {
                                BoxApplication.get().showToastWithImage(Constants.MSG_ERROR, TastyToast.WARNING);
                            }
                        } else {
                            BoxApplication.get().showToastWithImage(Constants.COPY_FAILED, TastyToast.ERROR);
                        }
                    }
                });
            }
        }
    }
    
    private void saveInstallationStatus(String packageName, boolean installed) {
        SharedPreferences preferences = MainActivity.get().getSharedPreferences("install_status", Context.MODE_PRIVATE);
        preferences.edit().putBoolean(packageName, installed).apply();
    }

    private boolean getInstallationStatus(String packageName) {
        SharedPreferences preferences = MainActivity.get().getSharedPreferences("install_status", Context.MODE_PRIVATE);
        return preferences.getBoolean(packageName, false);
    }
    
    private void updateButtonState(int gameIndex, TextView installButton) {
        String packageName = GAME_LIST_PKG[gameIndex];
        boolean installed = getInstallationStatus(packageName);
        if(installed) {
            installButton.setText("UNINSTALL");
        } else {
            installButton.setText("INSTALL");
        }
    }
    
    private void countDownStart() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expiryDate = dateFormat.parse(TimeExpired());
                    long now = System.currentTimeMillis();
                    long distance = expiryDate.getTime() - now;
                    long days = distance / (24 * 60 * 60 * 1000);
                    long hours = distance / (60 * 60 * 1000) % 24;
                    long minutes = distance / (60 * 1000) % 60;
                    long seconds = distance / 1000 % 60;
                    
                    TextView Hari = findViewById(R.id.tv_d);
                    TextView Jam = findViewById(R.id.tv_h);
                    TextView Menit = findViewById(R.id.tv_m);
                    TextView Detik = findViewById(R.id.tv_s);
                    
                    Hari.setText(String.format("%02d", Math.max(0, days)));
                    Jam.setText(String.format("%02d", Math.max(0, hours)));
                    Menit.setText(String.format("%02d", Math.max(0, minutes)));
                    Detik.setText(String.format("%02d", Math.max(0, seconds)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }
    
    private void GameJsonMods() {
        try {
            JSONArray games = new JSONObject(loadJSONFromAssets()).getJSONArray("games");
            TextView indiaName = findViewById(R.id.IndiaName);
            TextView indiaVersion = findViewById(R.id.IndiaVersion);
            if (indiaName != null) {
                indiaName.setText(games.getJSONObject(1).getString("name"));
            }
            if (indiaVersion != null) {
                indiaVersion.setText("Version: " + games.getJSONObject(1).getString("version"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String loadJSONFromAssets() {
        try {
            InputStream is = getAssets().open("games.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void CheckFloatViewPermission() {
        if (!Settings.canDrawOverlays(MainActivity.get())) {
            BoxApplication.get().showToastWithImage(Constants.MSG_FLOATING, TastyToast.INFO);
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FloatLogo.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startPatcher() {
        if (!Settings.canDrawOverlays(MainActivity.get())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 123);
        } else {
            startFloater();
        }
    }

    private void startFloater() {
        if (!isServiceRunning()) {
            startService(new Intent(MainActivity.get(), FloatLogo.class));
        } else {
            BoxApplication.get().showToastWithImage(Constants.MSG_RUNNING, TastyToast.WARNING);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        countDownStart();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.get(), FloatLogo.class));
        stopService(new Intent(MainActivity.get(), Overlay.class));
        stopService(new Intent(MainActivity.get(), FloatAim.class));
    }
}
