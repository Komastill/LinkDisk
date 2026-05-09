package LinkDisk.ui;

import LinkDisk.network.TcpClient;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import LinkDisk.network.UdpListener;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class MainFrame extends JFrame {

    private JButton selectButton;
    private JButton sendButton;
    
    private JTextArea logArea;
    
    private JList<String> deviceList;

    private DefaultListModel<String> deviceListModel;

    private File selectedFile;
    
    private String selectedIp;

    public MainFrame() {

        setTitle("LinkDisk");

        setSize(500, 400);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(null);
        deviceListModel =
                new DefaultListModel<>();

        deviceList =
                new JList<>(deviceListModel);

        JScrollPane deviceScrollPane =
                new JScrollPane(deviceList);

        deviceScrollPane.setBounds(300, 30, 150, 120);

        add(deviceScrollPane);
        deviceList.addListSelectionListener(
                new ListSelectionListener() {

                    @Override
                    public void valueChanged(
                            ListSelectionEvent e
                    ) {

                        selectedIp =
                                deviceList.getSelectedValue();

                        logArea.append(
                                "已选择设备："
                                        + selectedIp
                                        + "\n"
                        );
                    }
                }
        );

        selectButton = new JButton("选择文件");

        selectButton.setBounds(30, 30, 120, 40);

        add(selectButton);
        sendButton = new JButton("发送文件");

        sendButton.setBounds(180, 30, 120, 40);

        add(sendButton);
        
        logArea = new JTextArea();

        JScrollPane scrollPane =
                new JScrollPane(logArea);

        scrollPane.setBounds(30, 100, 420, 220);

        add(scrollPane);

        selectButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                JFileChooser fileChooser =
                        new JFileChooser();

                int result =
                        fileChooser.showOpenDialog(null);

                if (result == JFileChooser.APPROVE_OPTION) {

                    selectedFile =
                            fileChooser.getSelectedFile();

                    logArea.append(
                            "已选择文件：\n"
                            + selectedFile.getAbsolutePath()
                            + "\n\n"
                    );
                }
            }
        });
        sendButton.addActionListener(e -> {

            if (selectedFile == null) {

                logArea.append(
                        "请先选择文件\n"
                );

                return;
            }

            if (selectedIp == null) {

                logArea.append(
                        "请先选择设备\n"
                );

                return;
            }

            logArea.append(
                    "开始发送文件...\n"
            );

            TcpClient.sendFile(
                    selectedFile,
                    selectedIp
            );

            logArea.append(
                    "文件发送完成\n"
            );
        });
        
        
        UdpListener.startListening(ip -> {

            if (!deviceListModel.contains(ip)) {

                deviceListModel.addElement(ip);

                logArea.append(
                        "发现设备：" + ip + "\n"
                );
            }
        });
        
        setVisible(true);
    }

    public static void main(String[] args) {

        new MainFrame();
    }
}