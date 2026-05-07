package LinkDisk.network;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;

public class TcpServer {

    public static void main(String[] args) throws Exception {

        ServerSocket serverSocket = new ServerSocket(6000);

        System.out.println("等待客户端发送文件...");

        Socket socket = serverSocket.accept();

        System.out.println("客户端已连接");

        DataInputStream dataInputStream =
                new DataInputStream(socket.getInputStream());
        String fileName = dataInputStream.readUTF();

        System.out.println("接收到文件名：" + fileName);
        
        FileOutputStream fileOutputStream =
        		new FileOutputStream(fileName);

        byte[] buffer = new byte[1024];

        int len;

        while ((len = dataInputStream.read(buffer)) != -1) {

            fileOutputStream.write(buffer, 0, len);
        }

        System.out.println("文件接收完成");

        fileOutputStream.close();
        dataInputStream.close();        socket.close();
        serverSocket.close();
    }
}