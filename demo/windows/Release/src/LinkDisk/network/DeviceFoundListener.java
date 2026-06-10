package LinkDisk.network;

public interface DeviceFoundListener {
    void onDeviceFound(String ip, String deviceName, String platform);
}