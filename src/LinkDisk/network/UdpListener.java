package LinkDisk.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpListener {
    private static DatagramSocket listenSocket;
    private static boolean isRunning = false;
    private static Thread listenThread;
    private static Thread broadcastThread;

    public static void startListening(DeviceFoundListener listener) {
        if (isRunning) {
            return;
        }
        isRunning = true;

        // 启动监听线程
        listenThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listenSocket = new DatagramSocket(54321);
                    byte[] buffer = new byte[1024];
                    System.out.println("UDP监听已启动，端口：54321");

                    while (isRunning) {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        listenSocket.receive(packet);

                        String ip = packet.getAddress().getHostAddress();
                        System.out.println("UDP收到广播，来源IP = [" + ip + "]");
                        String message = new String(packet.getData(), 0, packet.getLength());

                        if ("LINKDISK_DEVICE".equals(message)) {
                            listener.onDeviceFound(ip);
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

        // 启动广播线程
        broadcastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket broadcastSocket = new DatagramSocket();
                    broadcastSocket.setBroadcast(true);
                    String message = "LINKDISK_DEVICE";
                    byte[] data = message.getBytes();

                    System.out.println("UDP广播已启动");

                    while (isRunning) {
                        DatagramPacket packet = new DatagramPacket(
                            data, data.length,
                            InetAddress.getByName("255.255.255.255"), 54321
                        );
                        broadcastSocket.send(packet);
                        System.out.println("广播已发送");
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
}