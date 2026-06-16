package LinkDisk.network;

import com.sun.net.httpserver.*;
import LinkDisk.ui.MainFrame;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.io.*;

public class HttpApiServer {

    public static void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        // 1. 启动设备发现
        server.createContext("/api/startDiscovery", exchange -> {
            setJsonHeaders(exchange);
            try {
                UdpListener.startListening(new DeviceFoundListener() {
                    @Override
                    public void onDeviceFound(String ip, String deviceName, String platform) {
                        System.out.println("发现设备：" + deviceName + " (" + ip + ")");
                    }
                });
                sendResponse(exchange, 200, "{\"code\":0,\"msg\":\"started\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"code\":-1}");
            }
        });

        // 2. 发送文件（旧接口）
        server.createContext("/api/sendFile", exchange -> {
            setJsonHeaders(exchange);
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String ip = params.get("ip");
                String path = params.get("path");

                File file = new File(path);
                String fileName = file.getName();
                long size = file.length();

                TransferTask task = new TransferTask(ip, fileName, size, "upload");
                task.setStatus("waiting");
                TransferManager.getInstance().addTask(task);

                sendResponse(exchange, 200, "{\"code\":0,\"msg\":\"task added\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"code\":-1}");
            }
        });

        // 3. 接收 Flutter 发来的文件列表并添加到待发送
        server.createContext("/api/addFiles", exchange -> {
            setJsonHeaders(exchange);
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();

                List<String> paths = new ArrayList<>();
                int start = body.indexOf("[");
                int end = body.indexOf("]");
                if (start != -1 && end != -1 && end > start) {
                    String arrStr = body.substring(start + 1, end);
                    boolean inQuotes = false;
                    StringBuilder current = new StringBuilder();
                    for (char c : arrStr.toCharArray()) {
                        if (c == '"') {
                            inQuotes = !inQuotes;
                        } else if (c == ',' && !inQuotes) {
                            paths.add(current.toString().trim());
                            current.setLength(0);
                        } else {
                            current.append(c);
                        }
                    }
                    if (current.length() > 0) {
                        paths.add(current.toString().trim());
                    }
                    for (int i = 0; i < paths.size(); i++) {
                        String p = paths.get(i);
                        if (p.startsWith("\"") && p.endsWith("\"")) {
                            p = p.substring(1, p.length() - 1);
                        }
                        paths.set(i, p);
                    }
                }

                File[] files = paths.stream().map(File::new).toArray(File[]::new);
                MainFrame.quickAddFiles(files);

                sendResponse(exchange, 200, "{\"code\":0,\"msg\":\"文件已添加\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"code\":-1,\"msg\":\"" + e.getMessage() + "\"}");
            }
        });

        // 4. 获取信任设备列表
        server.createContext("/api/trustedDevices", exchange -> {
            setJsonHeaders(exchange);
            try {
                AuthManager auth = new AuthManager();
                List<String> devices = auth.getAllTrustedDevices();
                StringBuilder json = new StringBuilder("{\"devices\":[");
                for (int i = 0; i < devices.size(); i++) {
                    if (i > 0)
                        json.append(",");
                    json.append("\"").append(devices.get(i)).append("\"");
                }
                json.append("]}");
                sendResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{}");
            }
        });

        // 5. 获取远程盘符
        server.createContext("/api/remoteDrives", exchange -> {
            setJsonHeaders(exchange);
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String targetIp = params.get("targetIp");
                if (targetIp == null || targetIp.isEmpty()) {
                    sendResponse(exchange, 400, "{}");
                    return;
                }
                String json = TcpClient.getRemoteDrives(targetIp);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{}");
            }
        });

        // 6. 获取远程文件列表
        server.createContext("/api/listRemoteFiles", exchange -> {
            setJsonHeaders(exchange);
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String targetIp = params.get("targetIp");
                String path = params.get("path");
                if (targetIp == null || path == null) {
                    sendResponse(exchange, 400, "{}");
                    return;
                }
                String json = TcpClient.listRemoteFiles(targetIp, path);
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{}");
            }
        });

        // 7. 远程删除（只传 path）
        server.createContext("/api/remoteDelete", exchange -> {
            handleRemoteOperation(exchange, "DELETE", "path");
        });

        // 8. 远程重命名（传 oldPath, newPath）
        server.createContext("/api/remoteRename", exchange -> {
            handleRemoteOperation(exchange, "RENAME", "oldPath", "newPath");
        });

        // 9. 远程创建文件夹（只传 path）
        server.createContext("/api/remoteMkdir", exchange -> {
            handleRemoteOperation(exchange, "MKDIR", "path");
        });

        // 10. 远程移动（传 src, dest）
        server.createContext("/api/remoteMove", exchange -> {
            setJsonHeaders(exchange);
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String targetIp = params.get("targetIp");
                String src = params.get("src");
                String dest = params.get("dest");
                String cmd = params.getOrDefault("cmd", "MOVE"); // 默认移动
                if (targetIp == null || src == null || dest == null) {
                    sendResponse(exchange, 400, "{\"code\":-1,\"msg\":\"参数缺失\"}");
                    return;
                }
                String result = TcpClient.sendCommand(targetIp, cmd, src, dest);
                String json = "OK".equals(result) ? "{\"code\":0,\"msg\":\"成功\"}" : "{\"code\":-1,\"msg\":\"操作失败\"}";
                sendResponse(exchange, 200, json);
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"code\":-1,\"msg\":\"内部错误\"}");
            }
        });

        // 11. 远程下载
        server.createContext("/api/remoteDownload", exchange -> {
            setJsonHeaders(exchange);
            try {
                Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
                String targetIp = params.get("targetIp");
                String path = params.get("path");
                String saveDir = params.get("saveDir");
                if (targetIp == null || path == null || saveDir == null) {
                    sendResponse(exchange, 400, "{\"code\":-1,\"msg\":\"参数缺失\"}");
                    return;
                }

                // 确保保存目录存在
                File dir = new File(saveDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // 调用 TcpClient 发送 DOWNLOAD 命令，获取临时文件
                String result = TcpClient.sendCommand(targetIp, "DOWNLOAD", path);
                if (result.startsWith("OK:")) {
                    String tempPath = result.substring(3);
                    File tempFile = new File(tempPath);
                    String fileName = new File(path).getName();
                    File destFile = new File(saveDir, fileName);

                    // 移动临时文件到目标目录（覆盖已有文件）
                    java.nio.file.Files.move(
                            tempFile.toPath(),
                            destFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    String localPath = destFile.getAbsolutePath().replace("\\", "/");
                    String json = String.format("{\"code\":0,\"msg\":\"下载成功\",\"localPath\":\"%s\"}", localPath);
                    sendResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 500, "{\"code\":-1,\"msg\":\"远程文件不存在或无权访问\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"code\":-1,\"msg\":\"下载异常\"}");
            }
        });
        
        server.setExecutor(null);
        server.start();
        System.out.println("✅ Java 本地接口已启动：http://127.0.0.1:" + port);
    }

    // ========== 通用处理方法 ==========
    private static void setJsonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
        exchange.close();
    }

    private static void handleRemoteOperation(HttpExchange exchange, String command, String... paramKeys) {
        setJsonHeaders(exchange);
        try {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            String targetIp = params.get("targetIp");
            if (targetIp == null || targetIp.isEmpty()) {
                sendResponse(exchange, 400, "{\"code\":-1,\"msg\":\"targetIp缺失\"}");
                return;
            }

            String[] paramValues = new String[paramKeys.length];
            for (int i = 0; i < paramKeys.length; i++) {
                paramValues[i] = params.get(paramKeys[i]);
                if (paramValues[i] == null) {
                    sendResponse(exchange, 400, "{\"code\":-1,\"msg\":\"参数缺失\"}");
                    return;
                }
            }

            // 此处不再将 targetIp 传入命令参数
            String result = TcpClient.sendCommand(targetIp, command, paramValues);
            String json = "OK".equals(result) ? "{\"code\":0,\"msg\":\"成功\"}" : "{\"code\":-1,\"msg\":\"操作失败\"}";
            sendResponse(exchange, 200, json);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(exchange, 500, "{\"code\":-1,\"msg\":\"内部错误\"}");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null)
            return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    map.put(kv[0], URLDecoder.decode(kv[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }
}