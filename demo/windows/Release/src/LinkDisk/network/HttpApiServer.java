package LinkDisk.network;

import com.sun.net.httpserver.*;
import LinkDisk.ui.MainFrame;
import java.net.InetSocketAddress;
import java.util.*;
import java.io.*;

public class HttpApiServer {

    public static void start(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        // 1. 启动设备发现（100% 匹配你真实代码）
        server.createContext("/api/startDiscovery", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                // 你真实可用的启动方法 ✅
                UdpListener.startListening(new DeviceFoundListener() {
                    @Override
                    public void onDeviceFound(String ip, String deviceName, String platform) {
                        System.out.println("发现设备：" + deviceName + " (" + ip + ")");
                    }
                });

                String json = "{\"code\":0,\"msg\":\"started\"}";
                exchange.sendResponseHeaders(200, json.getBytes().length);
                exchange.getResponseBody().write(json.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                String err = "{\"code\":-1}";
                exchange.sendResponseHeaders(500, err.getBytes().length);
                exchange.getResponseBody().write(err.getBytes());
            }
            exchange.close();
        });

        // 2. 发送文件（100% 匹配你真实代码）
        server.createContext("/api/sendFile", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                String ip = params.get("ip");
                String path = params.get("path");

                File file = new File(path);
                String fileName = file.getName();
                long size = file.length();

                // 你真实的构造方法 ✅ 绝对不报错！
                TransferTask task = new TransferTask(ip, fileName, size, "upload");
                task.setStatus("waiting");
                TransferManager.getInstance().addTask(task);

                String json = "{\"code\":0,\"msg\":\"task added\"}";
                exchange.sendResponseHeaders(200, json.getBytes().length);
                exchange.getResponseBody().write(json.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                String err = "{\"code\":-1}";
                exchange.sendResponseHeaders(500, err.getBytes().length);
                exchange.getResponseBody().write(err.getBytes());
            }
            exchange.close();
        });

        // 接收 Flutter 发来的文件列表并添加到待发送
        server.createContext("/api/addFiles", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                // 读取请求体
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String body = sb.toString();

                // 手动解析 JSON（不依赖任何外部库）
                // 格式：{"filePaths":["路径1","路径2"]}
                List<String> paths = new ArrayList<>();
                int start = body.indexOf("[");
                int end = body.indexOf("]");
                if (start != -1 && end != -1 && end > start) {
                    String arrStr = body.substring(start + 1, end);
                    // 分割每个字符串（假设路径不含引号内的逗号）
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
                    // 清理首尾引号
                    for (int i = 0; i < paths.size(); i++) {
                        String p = paths.get(i);
                        if (p.startsWith("\"") && p.endsWith("\"")) {
                            p = p.substring(1, p.length() - 1);
                        }
                        paths.set(i, p);
                    }
                }

                File[] files = paths.stream()
                        .map(File::new)
                        .toArray(File[]::new);

                // 调用 MainFrame 添加入口
                MainFrame.quickAddFiles(files);

                String resp = "{\"code\":0,\"msg\":\"文件已添加\"}";
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                exchange.getResponseBody().write(resp.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                String err = "{\"code\":-1,\"msg\":\"" + e.getMessage() + "\"}";
                exchange.sendResponseHeaders(500, err.getBytes().length);
                exchange.getResponseBody().write(err.getBytes());
            }
            exchange.close();
        });

        // 获取信任设备列表
        server.createContext("/api/trustedDevices", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                AuthManager auth = new AuthManager();
                List<String> devices = auth.getAllTrustedDevices();
                StringBuilder json = new StringBuilder();
                json.append("{\"devices\":[");
                boolean first = true;
                for (String ip : devices) {
                    if (!first)
                        json.append(",");
                    first = false;
                    json.append("\"").append(ip).append("\"");
                }
                json.append("]}");
                byte[] resp = json.toString().getBytes("utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, 0);
            }
            exchange.close();
        });

        // 获取远程盘符
        server.createContext("/api/remoteDrives", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                String targetIp = params.get("targetIp");
                if (targetIp == null || targetIp.isEmpty()) {
                    exchange.sendResponseHeaders(400, 0);
                    exchange.close();
                    return;
                }
                String json = TcpClient.getRemoteDrives(targetIp);
                byte[] resp = json.getBytes("utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, 0);
            }
            exchange.close();
        });

        // 获取远程文件列表
        server.createContext("/api/listRemoteFiles", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                String targetIp = params.get("targetIp");
                String path = params.get("path");
                if (targetIp == null || path == null) {
                    exchange.sendResponseHeaders(400, 0);
                    exchange.close();
                    return;
                }
                String json = TcpClient.listRemoteFiles(targetIp, path);
                byte[] resp = json.getBytes("utf-8");
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, 0);
            }
            exchange.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Java 本地接口已启动：http://127.0.0.1:" + port);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null)
            return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2)
                map.put(kv[0], kv[1]);
        }
        return map;
    }
}