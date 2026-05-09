package LinkDisk.network;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

public class TcpClient {

    public static void sendFile(
            File file,
            String ip
    ) {

        try {

            Socket socket =
                    new Socket(ip, 6000);

            DataOutputStream dataOutputStream =
                    new DataOutputStream(
                            socket.getOutputStream()
                    );

            dataOutputStream.writeUTF(
                    file.getName()
            );

            FileInputStream fileInputStream =
                    new FileInputStream(file);

            byte[] buffer = new byte[1024];

            int len;

            while ((len =
                    fileInputStream.read(buffer))
                    != -1) {

                dataOutputStream.write(
                        buffer,
                        0,
                        len
                );
            }

            fileInputStream.close();

            dataOutputStream.close();

            socket.close();

            System.out.println("文件发送成功");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}