package LinkDisk.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpBroadcaster {

    public static void main(String[] args) throws Exception {

        DatagramSocket socket = new DatagramSocket();

        String message = "LINKDISK_DEVICE";

        byte[] data = message.getBytes();

        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                InetAddress.getByName("172.20.10.255"),
                54321
        );

        while (true) {

            socket.send(packet);

            System.out.println("广播已发送");

            Thread.sleep(3000);
        }
    }
}