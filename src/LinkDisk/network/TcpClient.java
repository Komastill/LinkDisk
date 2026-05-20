package LinkDisk.network;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

public class TcpClient {

    public static void sendFiles(
            File[] files,
            String ip,
            ProgressListener listener
    ) {

        try {

            Socket socket =
                    new Socket(ip, 6000);

            DataOutputStream out =
                    new DataOutputStream(
                            socket.getOutputStream()
                    );

            // 发送文件数量
            out.writeInt(files.length);

            // 计算总大小
            long totalBytes = 0;

            for (File file : files) {

                totalBytes += file.length();
            }

            long sentBytes = 0;

            byte[] buffer = new byte[8192];

            // 循环发送每个文件
            for (File file : files) {

            	// 文件名
            	out.writeUTF(
            	        new String(
            	                file.getName().getBytes("UTF-8"),
            	                "UTF-8"
            	        )
            	);

                // 文件大小
                out.writeLong(file.length());

                FileInputStream in =
                        new FileInputStream(file);

                int len;

                while ((len = in.read(buffer)) != -1) {

                    out.write(buffer, 0, len);

                    sentBytes += len;

                    int progress =
                            (int)((sentBytes * 100)
                                    / totalBytes);

                    listener.onProgress(progress);
                }

                in.close();
            }

            out.close();

            socket.close();

            System.out.println("全部文件发送完成");

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}