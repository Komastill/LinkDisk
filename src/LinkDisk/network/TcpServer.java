package LinkDisk.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

public class TcpServer {

    private static AuthManager authManager = new AuthManager();
    private static AuthCallback authCallback;
    private static ServerSocket serverSocket;
    private static boolean isRunning = false;
    private static String saveDir = "received_files/";

    public interface AuthCallback {
        boolean onAuthRequest(String ip);
    }

    public static void startServer(AuthCallback callback) {
        if (isRunning) {
            return;
        }

        authCallback = callback;
        isRunning = true;

        new File(saveDir).mkdirs();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(6000);
                    System.out.println("TCP服务器已启动，端口：6000");

                    while (isRunning) {

                        Socket socket = serverSocket.accept();

                        String clientIp =
                                socket.getInetAddress().getHostAddress();

                        System.out.println("收到连接请求来自：" + clientIp);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                handleConnection(socket, clientIp);
                            }
                        }).start();
                    }

                } catch (Exception e) {
                    if (isRunning) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public static void stopServer() {
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleConnection(Socket socket, String clientIp) {

        try {

            DataInputStream in =
                    new DataInputStream(socket.getInputStream());

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            String command = in.readUTF();

            boolean authorized = true;

            if (!authManager.isTrusted(clientIp)) {

                authorized = false;

                if (authCallback != null) {
                    authorized = authCallback.onAuthRequest(clientIp);
                }

                if (authorized) {
                    authManager.addTrustedDevice(clientIp);
                    System.out.println("已授权设备：" + clientIp);
                } else {
                    System.out.println("拒绝设备连接：" + clientIp);
                }
            } else {
                System.out.println("信任设备已连接：" + clientIp);
            }

            if (!authorized) {
                out.writeUTF("DENIED");
                out.flush();
                in.close();
                out.close();
                socket.close();
                return;
            }

            out.writeUTF("OK");
            out.flush();

            if ("AUTH".equals(command)) {

                System.out.println("设备 " + clientIp + " 连接授权完成");

            } else if ("FILE".equals(command)) {

                receiveFiles(in, clientIp);

            } else {

                System.out.println("未知请求类型：" + command);
            }

            in.close();
            out.close();
            socket.close();

        } catch (Exception e) {
            System.err.println("处理设备 " + clientIp + " 连接时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void receiveFiles(DataInputStream in, String clientIp) {

        try {

            int fileCount = in.readInt();

            System.out.println("从 " + clientIp + " 接收文件数量：" + fileCount);

            for (int i = 0; i < fileCount; i++) {

                String fileName = in.readUTF();

                long fileSize = in.readLong();

                System.out.println("接收文件：" + fileName + " 大小：" + fileSize + " bytes");

                File saveFile = new File(saveDir, fileName);

                int count = 1;
                String name = fileName;
                String baseName = name;
                String ext = "";

                int dotIndex = name.lastIndexOf(".");

                if (dotIndex != -1) {
                    baseName = name.substring(0, dotIndex);
                    ext = name.substring(dotIndex);
                }

                while (saveFile.exists()) {
                    saveFile = new File(saveDir, baseName + "(" + count + ")" + ext);
                    count++;
                }

                FileOutputStream fileOut =
                        new FileOutputStream(saveFile);

                byte[] buffer = new byte[8192];

                long received = 0;

                int len;

                while (received < fileSize) {

                    len = in.read(
                            buffer,
                            0,
                            (int) Math.min(buffer.length, fileSize - received)
                    );

                    if (len == -1) {
                        break;
                    }

                    fileOut.write(buffer, 0, len);

                    received += len;
                }

                fileOut.close();

                System.out.println(fileName + " 接收完成");
                System.out.println("实际保存路径：" + saveFile.getAbsolutePath());
                System.out.println("实际接收大小：" + received + " bytes");
            }

            System.out.println("设备 " + clientIp + " 所有文件接收完成\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}