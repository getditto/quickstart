using System;
namespace DittoMauiTasksApp.Utils
{
    public static class PermissionHelper
    {
        public static async Task<bool> CheckPermissions()
        {
            PermissionStatus bluetoothStatus = await CheckBluetoothPermissions();

            return IsGranted(bluetoothStatus);
        }

        private static async Task<PermissionStatus> CheckBluetoothPermissions()
        {
            PermissionStatus bluetoothStatus = PermissionStatus.Granted;

            if (DeviceInfo.Platform == DevicePlatform.Android)
            {
                if (DeviceInfo.Version.Major >= 12)
                {
                    bluetoothStatus = await CheckPermissions<NearbyDevicesPermission>();
                }
                else
                {
                    bluetoothStatus = await CheckPermissions<Permissions.LocationWhenInUse>();
                }
            }

            return bluetoothStatus;
        }

        private static async Task<PermissionStatus> CheckPermissions<TPermission>() where TPermission : Permissions.BasePermission, new()
        {
            PermissionStatus status = await Permissions.CheckStatusAsync<TPermission>();

            if (status != PermissionStatus.Granted)
            {
                status = await Permissions.RequestAsync<TPermission>();
            }

            return status;
        }

        private static bool IsGranted(PermissionStatus status)
        {
            return status == PermissionStatus.Granted || status == PermissionStatus.Limited;
        }
    }
}

