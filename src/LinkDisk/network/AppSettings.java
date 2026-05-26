package LinkDisk.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class AppSettings {

    private static final String SETTINGS_FILE = "linkdisk_settings.properties";

    private static final String KEY_RECEIVE_DIR = "receiveDir";

    private static final String DEFAULT_RECEIVE_DIR = "received_files";

    public static String getReceiveDir() {

        Properties properties = loadProperties();

        String receiveDir = properties.getProperty(KEY_RECEIVE_DIR);

        if (receiveDir == null || receiveDir.trim().length() == 0) {
            receiveDir = DEFAULT_RECEIVE_DIR;
        }

        File folder = new File(receiveDir);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder.getPath();
    }

    public static void setReceiveDir(String receiveDir) {

        if (receiveDir == null || receiveDir.trim().length() == 0) {
            return;
        }

        File folder = new File(receiveDir);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        Properties properties = loadProperties();

        properties.setProperty(KEY_RECEIVE_DIR, folder.getAbsolutePath());

        saveProperties(properties);
    }

    private static Properties loadProperties() {

        Properties properties = new Properties();

        File file = new File(SETTINGS_FILE);

        if (!file.exists()) {
            return properties;
        }

        FileInputStream in = null;

        try {
            in = new FileInputStream(file);

            properties.load(in);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(in);
        }

        return properties;
    }

    private static void saveProperties(Properties properties) {

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(SETTINGS_FILE);

            properties.store(out, "LinkDisk Settings");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeQuietly(out);
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {

        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
