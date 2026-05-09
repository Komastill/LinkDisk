package LinkDisk.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpListener {

    public static void startListening(
            DeviceFoundListener listener
    ) {

        new Thread(() -> {

            try {

                DatagramSocket socket =
                        new DatagramSocket(54321);

                byte[] buffer = new byte[1024];

                System.out.println("开始监听...");

                while (true) {

                    DatagramPacket packet =
                            new DatagramPacket(
                                    buffer,
                                    buffer.length
                            );

                    socket.receive(packet);

                    String ip =
                            packet.getAddress()
                                    .getHostAddress();

                    String message =
                            new String(
                                    packet.getData(),
                                    0,
                                    packet.getLength()
                            );

                    if (message.equals("LINKDISK_DEVICE")) {

                        listener.onDeviceFound(ip);
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();
            }

        }).start();
    }
}