package LinkDisk.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpListener {

    public static void main(String[] args) throws Exception {

        DatagramSocket socket = new DatagramSocket(54321);

        byte[] buffer = new byte[1024];

        System.out.println("开始监听...");

        while (true) {

            DatagramPacket packet =
                    new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);

            String msg = new String(
                    packet.getData(),
                    0,
                    packet.getLength()
            );

            String ip = packet.getAddress().getHostAddress();

            System.out.println(ip + " : " + msg);
        }
    }
}