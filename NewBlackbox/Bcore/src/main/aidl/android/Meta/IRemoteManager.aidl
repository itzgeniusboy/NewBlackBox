package android.Meta;

interface IRemoteManager {
    void activateSdk(String userkey);
    boolean getActivatedSdk();
    String getServerMessage();
    boolean getNetwork();
}