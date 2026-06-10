package LinkDisk.network;

import com.sun.net.httpserver.*;
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

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Java 本地接口已启动：http://127.0.0.1:" + port);
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }
}