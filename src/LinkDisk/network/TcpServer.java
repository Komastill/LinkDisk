package LinkDisk.network;

import java.io.DataInputStream;
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
        
        // 创建接收文件目录
        new File(saveDir).mkdirs();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(6000);
                    System.out.println("TCP服务器已启动，端口：6000");

                    while (isRunning) {
                        Socket socket = serverSocket.accept();
                        String clientIp = socket.getInetAddress().getHostAddress();
                        System.out.println("收到连接请求来自：" + clientIp);

                        // 检查是否已信任
                        if (!authManager.isTrusted(clientIp)) {
                            // 请求授权
                            if (authCallback != null) {
                                boolean authorized = authCallback.onAuthRequest(clientIp);
                                if (authorized) {
                                    authManager.addTrustedDevice(clientIp);
                                    System.out.println("已授权设备：" + clientIp);
                                    handleClient(socket, clientIp);
                                } else {
                                    System.out.println("拒绝设备连接：" + clientIp);
                                    socket.close();
                                }
                            } else {
                                socket.close();
                            }
                        } else {
                            System.out.println("信任设备已连接：" + clientIp);
                            handleClient(socket, clientIp);
                        }
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

    private static void handleClient(Socket socket, String clientIp) {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // 接收文件数量
            int fileCount = in.readInt();
            System.out.println("从 " + clientIp + " 接收文件数量：" + fileCount);

            // 循环接收文件
            for (int i = 0; i < fileCount; i++) {
                // 文件名
                String fileName = in.readUTF();
                // 文件大小
                long fileSize = in.readLong();

                System.out.println("接收文件：" + fileName + " 大小：" + fileSize + " bytes");

                // 保存文件，添加设备IP前缀避免重名
                String saveFileName = saveDir + clientIp + "_" + fileName;
                FileOutputStream out = new FileOutputStream(saveFileName);

                byte[] buffer = new byte[8192];
                long received = 0;
                int len;

                while (received < fileSize) {
                    len = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize - received));
                    if (len == -1) {
                        break;
                    }
                    out.write(buffer, 0, len);
                    received += len;
                }

                out.close();
                System.out.println(fileName + " 接收完成，保存为：" + saveFileName);
            }

            in.close();
            socket.close();
            System.out.println("设备 " + clientIp + " 所有文件接收完成\n");

        } catch (Exception e) {
            System.err.println("处理设备 " + clientIp + " 连接时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
}