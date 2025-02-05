namespace DittoMauiTasksApp.Utils;

using static Microsoft.Maui.ApplicationModel.Permissions;

public class NearbyDevicesPermission : BasePlatformPermission
{
    private readonly List<(string permission, bool isRuntime)> _requiredPermissions;

    public NearbyDevicesPermission()
    {
        _requiredPermissions = new List<(string permission, bool isRuntime)>
            {
                ("android.permission.BLUETOOTH_ADVERTISE", true),
                ("android.permission.BLUETOOTH_SCAN", true),
                ("android.permission.BLUETOOTH_CONNECT", true),
            };

        if (OperatingSystem.IsAndroidVersionAtLeast(33))
        {
            _requiredPermissions.Add(("android.permission.NEARBY_WIFI_DEVICES", true));
        }
    }

#if ANDROID31_0_OR_GREATER
    public override (string androidPermission, bool isRuntime)[] RequiredPermissions =>
        _requiredPermissions.ToArray();
#endif
}
