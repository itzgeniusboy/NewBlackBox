package android.MetaCore.Service;

import android.Meta.IRemoteManager;
import android.MetaCore.RemoteManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class MetaActivationService extends Service {

    private static final String TAG = "MetaActivationService";

    private final IRemoteManager.Stub binder = new IRemoteManager.Stub() {
        @Override
        public void activateSdk(String userkey) throws RemoteException {
            try {
                RemoteManager.getInstance().activateSdk(userkey);
            } catch (Exception e) {
                Log.e(TAG, "Error in activateSdk", e);
                throw new RemoteException("Activation failed: " + e.getMessage());
            }
        }

        @Override
        public boolean getActivatedSdk() throws RemoteException {
            try {
                return RemoteManager.getInstance().getActivatedSdk();
            } catch (Exception e) {
                Log.e(TAG, "Error in getActivatedSdk", e);
                throw new RemoteException("Failed to get activation status");
            }
        }

        @Override
        public String getServerMessage() throws RemoteException {
            try {
                return RemoteManager.getInstance().getServerMessage();
            } catch (Exception e) {
                Log.e(TAG, "Error in getServerMessage", e);
                throw new RemoteException("Failed to get server message");
            }
        }

        @Override
        public boolean getNetwork() throws RemoteException {
            try {
                return RemoteManager.getInstance().getNetwork();
            } catch (Exception e) {
                Log.e(TAG, "Error in getNetwork", e);
                throw new RemoteException("Failed to get network status");
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}