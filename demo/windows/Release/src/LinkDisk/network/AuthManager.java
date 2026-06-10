package LinkDisk.network;

import java.io.*;
import java.util.*;

public class AuthManager {
    private static final String TRUST_FILE = "trusted_devices.dat";
    private Set<String> trustedDevices = new HashSet<>();
    
    public AuthManager() {
        loadTrustedDevices();
    }
    
    public boolean isTrusted(String ip) {
        return trustedDevices.contains(ip);
    }
    
    public void addTrustedDevice(String ip) {
        trustedDevices.add(ip);
        saveTrustedDevices();
        System.out.println("已添加信任设备：" + ip);
    }
    
    public void removeTrustedDevice(String ip) {
        trustedDevices.remove(ip);
        saveTrustedDevices();
        System.out.println("已移除信任设备：" + ip);
    }
    
    public List<String> getAllTrustedDevices() {
        return new ArrayList<>(trustedDevices);
    }
    
    private void loadTrustedDevices() {
        File file = new File(TRUST_FILE);
        if (!file.exists()) {
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            trustedDevices = (Set<String>) ois.readObject();
            System.out.println("加载信任设备：" + trustedDevices);
        } catch (Exception e) {
            System.out.println("加载信任设备失败，将使用空列表");
            trustedDevices = new HashSet<>();
        }
    }
    
    private void saveTrustedDevices() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TRUST_FILE))) {
            oos.writeObject(trustedDevices);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}