import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class TextReaderApp extends JFrame {
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private File currentFile;
    private int currentPage = 1;
    private int totalPages = 0;
    private int linesPerPage = 100;
    private Map<String, Integer> bookmarks = new HashMap<>();
    private JList<String> bookmarkList;
    private DefaultListModel<String> bookmarkListModel;
    private JPanel mainPanel;
    private JLabel statusLabel;
    private String currentTheme = "Light";
    private int fontSize = 12;

    public TextReaderApp() {
        initComponents();
        setTitle("大型TXT文件阅读器");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        // 创建主面板
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // 创建工具栏
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // 打开文件按钮
        JButton openButton = new JButton("打开文件");
        openButton.addActionListener(e -> openFile());
        toolBar.add(openButton);

        // 上一页按钮
        JButton prevButton = new JButton("上一页");
        prevButton.addActionListener(e -> showPrevPage());
        toolBar.add(prevButton);

        // 下一页按钮
        JButton nextButton = new JButton("下一页");
        nextButton.addActionListener(e -> showNextPage());
        toolBar.add(nextButton);

        // 添加书签按钮
        JButton bookmarkButton = new JButton("添加书签");
        bookmarkButton.addActionListener(e -> addBookmark());
        toolBar.add(bookmarkButton);

        // 转到页面按钮
        JButton gotoButton = new JButton("转到页面");
        gotoButton.addActionListener(e -> gotoPage());
        toolBar.add(gotoButton);

        // 设置按钮
        JButton settingsButton = new JButton("设置");
        settingsButton.addActionListener(e -> openSettings());
        toolBar.add(settingsButton);

        // 页码控件
        JPanel pagePanel = new JPanel();
        pagePanel.add(new JLabel("页码:"));
        JTextField pageField = new JTextField(5);
        pagePanel.add(pageField);
        pagePanel.add(new JLabel("/"));
        JLabel totalPagesLabel = new JLabel("0");
        pagePanel.add(totalPagesLabel);
        toolBar.add(pagePanel);

        // 每页行数设置
        JPanel linesPanel = new JPanel();
        linesPanel.add(new JLabel("每页行数:"));
        JSpinner linesSpinner = new JSpinner(new SpinnerNumberModel(100, 50, 500, 10));
        linesSpinner.addChangeListener(e -> {
            linesPerPage = (int) linesSpinner.getValue();
            if (currentFile != null) {
                try {
                    calculateTotalPages();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                showPage(currentPage);
            }
        });
        linesPanel.add(linesSpinner);
        toolBar.add(linesPanel);

        mainPanel.add(toolBar, BorderLayout.NORTH);

        // 创建文本区域
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, fontSize));
        JScrollPane scrollPane = new JScrollPane(textArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 创建书签面板
        JPanel bookmarkPanel = new JPanel(new BorderLayout());
        bookmarkPanel.setPreferredSize(new Dimension(200, 0));
        bookmarkPanel.setBorder(new TitledBorder("书签"));

        bookmarkListModel = new DefaultListModel<>();
        bookmarkList = new JList<>(bookmarkListModel);
        bookmarkList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && bookmarkList.getSelectedValue() != null) {
                String bookmark = bookmarkList.getSelectedValue();
                if (bookmarks.containsKey(bookmark)) {
                    currentPage = bookmarks.get(bookmark);
                    showPage(currentPage);
                }
            }
        });

        bookmarkPanel.add(new JScrollPane(bookmarkList), BorderLayout.CENTER);
        mainPanel.add(bookmarkPanel, BorderLayout.EAST);

        // 创建状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        statusLabel = new JLabel("就绪 - 请打开一个TXT文件");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void openFile() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
                }
                public String getDescription() {
                    return "文本文件 (*.txt)";
                }
            });
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try {
                long fileSize = currentFile.length();
                double sizeMB = fileSize / (1024.0 * 1024.0);
                updateStatus("正在打开文件: " + currentFile.getName() + " (" + String.format("%.2f", sizeMB) + " MB)...");

                // 计算总页数
                calculateTotalPages();
                updateStatus("文件加载完成! 总页数: " + totalPages);

                // 显示第一页
                currentPage = 1;
                showPage(currentPage);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "无法打开文件: " + ex.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void calculateTotalPages() throws IOException {
        if (currentFile == null) return;

        long lineCount = Files.lines(currentFile.toPath(), StandardCharsets.UTF_8).count();
        totalPages = (int) Math.ceil((double) lineCount / linesPerPage);
    }

    private void showPage(int page) {
        if (currentFile == null || page < 1 || page > totalPages) return;

        currentPage = page;
        updateStatus("正在加载第 " + page + "/" + totalPages + " 页...");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(currentFile), StandardCharsets.UTF_8))) {

            textArea.setText("");
            int startLine = (page - 1) * linesPerPage;
            int endLine = startLine + linesPerPage;

            for (int i = 0; i < endLine; i++) {
                String line = reader.readLine();
                if (line == null) break;
                if (i >= startLine) {
                    textArea.append(line + "\n");
                }
            }

            updateStatus("第 " + page + "/" + totalPages + " 页 | 文件: " + currentFile.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "读取文件时出错: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPrevPage() {
        if (currentPage > 1) {
            showPage(currentPage - 1);
        }
    }

    private void showNextPage() {
        if (currentPage < totalPages) {
            showPage(currentPage + 1);
        }
    }

    private void gotoPage() {
        String input = JOptionPane.showInputDialog(this, "输入页码 (1-" + totalPages + "):", currentPage);
        if (input != null) {
            try {
                int page = Integer.parseInt(input);
                if (page >= 1 && page <= totalPages) {
                    showPage(page);
                } else {
                    JOptionPane.showMessageDialog(this, "页码超出范围!",
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "请输入有效的页码!",
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addBookmark() {
        if (currentFile == null) return;

        String name = "书签 " + (bookmarks.size() + 1) + " (第 " + currentPage + " 页)";
        bookmarks.put(name, currentPage);
        bookmarkListModel.addElement(name);
    }

    private void openSettings() {
        JDialog settingsDialog = new JDialog(this, "设置", true);
        settingsDialog.setSize(600, 400);
        settingsDialog.setLocationRelativeTo(this);

        JTabbedPane tabbedPane = new JTabbedPane();

        // 主题设置标签
        JPanel themePanel = new JPanel();
        themePanel.setLayout(new BoxLayout(themePanel, BoxLayout.Y_AXIS));
        themePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel themeLabel = new JLabel("选择主题:");
        themePanel.add(themeLabel);

        ButtonGroup themeGroup = new ButtonGroup();
        JRadioButton lightTheme = new JRadioButton("明亮模式", "Light".equals(currentTheme));
        JRadioButton darkTheme = new JRadioButton("暗黑模式", "Dark".equals(currentTheme));
        JRadioButton eyeTheme = new JRadioButton("护眼模式", "Eye".equals(currentTheme));

        themeGroup.add(lightTheme);
        themeGroup.add(darkTheme);
        themeGroup.add(eyeTheme);

        themePanel.add(lightTheme);
        themePanel.add(darkTheme);
        themePanel.add(eyeTheme);

        lightTheme.addActionListener(e -> applyTheme("Light"));
        darkTheme.addActionListener(e -> applyTheme("Dark"));
        eyeTheme.addActionListener(e -> applyTheme("Eye"));

        tabbedPane.addTab("主题设置", themePanel);

        // 免责声明标签
        JTextArea disclaimerArea = new JTextArea(
                "免责声明\n\n" +
                        "1. 本软件仅用于阅读文本文件，不收集任何用户数据。\n" +
                        "2. 使用本软件打开的文件内容由用户自行负责。\n" +
                        "3. 开发者不对软件使用造成的任何损失负责。\n" +
                        "4. 请勿使用本软件打开敏感或受版权保护的内容。"
        );
        disclaimerArea.setEditable(false);
        disclaimerArea.setLineWrap(true);
        disclaimerArea.setWrapStyleWord(true);
        disclaimerArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane disclaimerScroll = new JScrollPane(disclaimerArea);
        tabbedPane.addTab("免责声明", disclaimerScroll);

        // 关于标签
        JTextArea aboutArea = new JTextArea(
                "大型TXT文件阅读器 v1.0\n\n" +
                        "功能特点:\n" +
                        "- 高效加载大型文本文件\n" +
                        "- 分页阅读，支持快速翻页\n" +
                        "- 书签功能，记录阅读位置\n" +
                        "- 自定义显示设置\n\n" +
                        "开发者: Jin-Yan-t\n" +
                        "GitHub: https://github.com/Jin-Yan-t/java_txt_reader"
        );
        aboutArea.setEditable(false);
        aboutArea.setLineWrap(true);
        aboutArea.setWrapStyleWord(true);
        aboutArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane aboutScroll = new JScrollPane(aboutArea);

        JButton githubButton = new JButton("访问GitHub项目");
        githubButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new java.net.URI("https://github.com/yourusername"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        JPanel aboutPanel = new JPanel(new BorderLayout());
        aboutPanel.add(aboutScroll, BorderLayout.CENTER);
        aboutPanel.add(githubButton, BorderLayout.SOUTH);

        tabbedPane.addTab("关于", aboutPanel);

        settingsDialog.add(tabbedPane);
        settingsDialog.setVisible(true);
    }

    private void applyTheme(String theme) {
        currentTheme = theme;
        switch (theme) {
            case "Light":
                mainPanel.setBackground(Color.WHITE);
                textArea.setBackground(Color.WHITE);
                textArea.setForeground(Color.BLACK);
                statusLabel.setBackground(Color.LIGHT_GRAY);
                break;
            case "Dark":
                mainPanel.setBackground(Color.DARK_GRAY);
                textArea.setBackground(new Color(45, 45, 45));
                textArea.setForeground(new Color(224, 224, 224));
                statusLabel.setBackground(new Color(51, 51, 51));
                break;
            case "Eye":
                mainPanel.setBackground(new Color(224, 240, 224));
                textArea.setBackground(new Color(199, 237, 204));
                textArea.setForeground(Color.BLACK);
                statusLabel.setBackground(new Color(208, 224, 208));
                break;
        }
        mainPanel.repaint();
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            new TextReaderApp().setVisible(true);
        });
    }
}