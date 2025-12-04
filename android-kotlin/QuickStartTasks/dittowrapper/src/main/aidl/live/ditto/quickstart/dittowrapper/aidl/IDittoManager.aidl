// IDittoManager.aidl
package live.ditto.quickstart.dittowrapper.aidl;

// Declare any non-default types here with import statements

interface IDittoManager {
    void initDitto(String appId, String token, String customAuthUrl, String webSocketUrl);
    List<String> getMissingPermissions();
}