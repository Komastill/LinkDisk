package LinkDisk.network;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.UUID;

public class UdpListener {

    private static DatagramSocket listenSocket;
    private static boolean isRunning = false;
    private static Thread listenThread;
    private static Thread broadcastThread;

    private static final int UDP_PORT = 54321;
    private static final String PREFIX = "LINKDISK_DEVICE";
    private static final String DEVICE_ID_FILE = "local_device_id.txt";

    private static final String localDeviceId = loadOrCreateDeviceId();
    private static final String localDeviceName = getLocalDeviceName();
    private static final String localPlatform = getPlatformName();

    public static void startListening(DeviceFoundListener listener) {
        if (isRunning) {
            return;
        }

        isRunning = true;

        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listenSocket = new DatagramSocket(UDP_PORT);
                    byte[] buffer = new byte[1024];

                    System.out.println("UDP监听已启动，端口：" + UDP_PORT);
                    System.out.println("本机设备ID：" + localDeviceId);
                    System.out.println("本机设备名：" + localDeviceName);
                    System.out.println("本机平台：" + localPlatform);

                    while (isRunning) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                        listenSocket.receive(packet);

                        String ip = packet.getAddress().getHostAddress();

                        String message = new String(
                                packet.getData(),
                                0,
                                packet.getLength(),
                                "UTF-8");

                        String[] parts = message.split("\\|", 4);

                        if (parts.length == 4 && PREFIX.equals(parts[0])) {

                            String deviceId = parts[1];
                            String deviceName = parts[2];
                            String platform = parts[3];

                            // 关键：过滤自己
                            if (localDeviceId.equals(deviceId)) {
                                continue;
                            }

                            System.out.println(
                                    "发现远程设备：" +
                                            deviceName + " / " +
                                            platform + " / " +
                                            ip);

                            listener.onDeviceFound(ip, deviceName, platform);
                        }
                    }

                } catch (Exception e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        });

        listenThread.start();

        broadcastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket broadcastSocket = new DatagramSocket();
                    broadcastSocket.setBroadcast(true);

                    System.out.println("UDP广播已启动");

                    while (isRunning) {

                        String message = PREFIX + "|" +
                                localDeviceId + "|" +
                                localDeviceName + "|" +
                                localPlatform;

                        byte[] data = message.getBytes("UTF-8");

                        DatagramPacket packet = new DatagramPacket(
                                data,
                                data.length,
                                InetAddress.getByName("255.255.255.255"),
                                UDP_PORT);

                        broadcastSocket.send(packet);

                        System.out.println("广播已发送：" + message);

                        Thread.sleep(3000);
                    }

                    broadcastSocket.close();

                } catch (Exception e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        });

        broadcastThread.start();
    }

    public static void stopListening() {
        isRunning = false;

        if (listenSocket != null && !listenSocket.isClosed()) {
            listenSocket.close();
        }

        if (listenThread != null) {
            listenThread.interrupt();
        }

        if (broadcastThread != null) {
            broadcastThread.interrupt();
        }
    }

    private static String loadOrCreateDeviceId() {
        File file = new File(DEVICE_ID_FILE);

        try {
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));

                String id = reader.readLine();

                reader.close();

                if (id != null && id.trim().length() > 0) {
                    return id.trim();
                }
            }

            String newId = UUID.randomUUID().toString();

            FileWriter writer = new FileWriter(file);
            writer.write(newId);
            writer.close();

            return newId;

        } catch (Exception e) {
            e.printStackTrace();
            return UUID.randomUUID().toString();
        }
    }

    private static String getLocalDeviceName() {
        try {
            String name = System.getenv("COMPUTERNAME");

            if (name == null || name.trim().length() == 0) {
                name = System.getenv("HOSTNAME");
            }

            if (name == null || name.trim().length() == 0) {
                name = InetAddress.getLocalHost().getHostName();
            }

            if (name == null || name.trim().length() == 0) {
                name = "UnknownDevice";
            }

            return name.replace("|", "_");

        } catch (Exception e) {
            return "UnknownDevice";
        }
    }

    private static String getPlatformName() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return "Windows";
        } else if (os.contains("mac")) {
            return "macOS";
        } else if (os.contains("linux")) {
            return "Linux";
        } else {
            return System.getProperty("os.name");
        }
    }

    public static String getThisDeviceName() {
        return localDeviceName;
    }

    public static String getThisPlatform() {
        return localPlatform;
    }

    public static boolean isLocalIp(String ip) {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface
                    .getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();

                java.util.Enumeration<java.net.InetAddress> addresses = ni.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    java.net.InetAddress address = addresses.nextElement();

                    if (address.getHostAddress().equals(ip)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp())
                    continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}