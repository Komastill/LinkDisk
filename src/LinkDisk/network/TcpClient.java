package LinkDisk.network;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.DataOutputStream;

public class TcpClient {

    public static void main(String[] args) throws Exception {

        Socket socket =
                new Socket("127.0.0.1", 6000);

        System.out.println("已连接服务器");

        FileInputStream fileInputStream =
                new FileInputStream("/Users/cxr15803959066/Desktop/陈笑然-优秀学生申请表.doc");

        DataOutputStream dataOutputStream =
                new DataOutputStream(socket.getOutputStream());

        String fileName = "陈笑然-优秀学生申请表.doc";

        dataOutputStream.writeUTF(fileName);

        byte[] buffer = new byte[1024];

        int len;

        while ((len = fileInputStream.read(buffer)) != -1) {

        	dataOutputStream.write(buffer, 0, len);        }

        System.out.println("文件发送完成");

        fileInputStream.close();

        dataOutputStream.close();
        socket.close();
    }
}