package LinkDisk.ui;

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

    private JTextArea logArea;

    private File selectedFile;

    public MainFrame() {

        setTitle("LinkDisk");

        setSize(500, 400);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(null);

        selectButton = new JButton("选择文件");

        selectButton.setBounds(30, 30, 120, 40);

        add(selectButton);

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

        setVisible(true);
    }

    public static void main(String[] args) {

        new MainFrame();
    }
}