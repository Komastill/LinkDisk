package LinkDisk.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

public class TcpClient {

    // 只请求连接/授权，不发送文件
    public static boolean connectDevice(String ip) {
        try {
            Socket socket = new Socket(ip, 6000);

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            DataInputStream in =
                    new DataInputStream(socket.getInputStream());

            out.writeUTF("AUTH");
            out.flush();

            String result = in.readUTF();

            in.close();
            out.close();
            socket.close();

            return "OK".equals(result);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 正式发送文件
    public static boolean sendFiles(
            File[] files,
            String ip,
            ProgressListener listener
    ) {

        try {

            Socket socket = new Socket(ip, 6000);

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            DataInputStream in =
                    new DataInputStream(socket.getInputStream());

            // 告诉服务端：这次连接是文件传输
            out.writeUTF("FILE");
            out.flush();

            // 等待服务端授权结果
            String result = in.readUTF();

            if (!"OK".equals(result)) {
                System.out.println("对方拒绝连接，文件未发送");
                in.close();
                out.close();
                socket.close();
                return false;
            }

            // 发送文件数量
            out.writeInt(files.length);

            long totalBytes = 0;

            for (File file : files) {
                totalBytes += file.length();
            }

            long sentBytes = 0;

            byte[] buffer = new byte[8192];

            for (File file : files) {

                System.out.println("准备发送文件：" + file.getAbsolutePath());
                System.out.println("发送文件名：" + file.getName());
                System.out.println("发送文件大小：" + file.length());

                out.writeUTF(file.getName());

                out.writeLong(file.length());

                FileInputStream fileIn = new FileInputStream(file);

                int len;

                while ((len = fileIn.read(buffer)) != -1) {

                    out.write(buffer, 0, len);

                    sentBytes += len;

                    int progress;

                    if (totalBytes == 0) {
                        progress = 100;
                    } else {
                        progress = (int) ((sentBytes * 100) / totalBytes);
                    }

                    listener.onProgress(progress);
                }

                fileIn.close();
            }

            out.close();
            in.close();
            socket.close();

            System.out.println("全部文件发送完成");

            return true;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }
}