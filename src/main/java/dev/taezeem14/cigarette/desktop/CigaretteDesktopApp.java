package dev.taezeem14.cigarette.desktop;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vape V4-styled background desktop companion application with vector fa-* icons,
 * Windows/Linux/macOS SystemTray hide/show control, live Minecraft detection,
 * 1-click mod installer, and live profile JSON synchronization.
 */
public class CigaretteDesktopApp {

    private static final Color BG_ROOT = new Color(0x14, 0x14, 0x16);
    private static final Color BG_SIDEBAR = new Color(0x1A, 0x1A, 0x1D);
    private static final Color BG_CARD = new Color(0x1E, 0x1E, 0x24);
    private static final Color BG_CARD_ALT = new Color(0x24, 0x24, 0x2C);
    private static final Color BORDER_COL = new Color(0x2C, 0x2C, 0x36);
    private static final Color TEXT_PRIMARY = new Color(0xEB, 0xEB, 0xEF);
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x96);
    private static final Color EMERALD = new Color(0x10, 0xB9, 0x81);
    private static final Color AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color CRIMSON = new Color(0xEF, 0x44, 0x44);

    private static final File PORT_LOCK_FILE = new File(System.getProperty("user.home", "."), ".cigarette-v1-companion.port");

    private final String edition = DesktopCatalog.detectEditionFromClasspath();
    private Color accent = new Color(0x38, 0x82, 0xF6);
    private String themePreset = "Electric Blue";

    private JFrame frame;
    private TrayIcon trayIcon;
    private MenuItem trayToggleItem;

    private CardLayout contentCards;
    private JPanel contentArea;
    private String activeTab = "dashboard";
    private final Map<String, NavButton> navButtons = new LinkedHashMap<String, NavButton>();

    private JLabel headerStatusPill;
    private JLabel dashBannerTitle;
    private JLabel dashBannerBody;
    private JPanel dashBannerCard;
    private JButton dashOpenClickGuiBtn;
    private JLabel statModulesLbl;
    private JLabel statProfileLbl;
    private JLabel statThemeLbl;
    private JLabel statInstallLbl;

    private JComboBox<File> gameDirCombo;
    private JPanel modulesGrid;
    private JTextField moduleSearchField;
    private DesktopCatalog.CategoryType moduleCatFilter = null;

    private JPanel profilesListPanel;
    private JTextField newProfileField;
    private JPanel installerModsPanel;
    private JLabel installerConflictLabel;
    private JLabel trayStatusLabel;

    private boolean hideOnClose = true;
    private boolean autoLiveSync = true;
    private boolean alwaysOnTop = false;
    private boolean scanInFlight = false;

    private List<MinecraftBridge.RunningProcess> runningProcesses = new ArrayList<MinecraftBridge.RunningProcess>();
    private List<File> discoveredDirs = new ArrayList<File>();
    private File selectedGameDir;
    private String activeProfileName = "default";
    private Map<String, Object> activeProfileData = new LinkedHashMap<String, Object>();

    public static void main(String[] args) {
        // Prevent Swing Direct3D pipeline glitches on older Windows Intel GPUs (e.g. Sandy Bridge)
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.d3d", "false");

        final boolean startMinimized = hasFlag(args, "--minimized") || hasFlag(args, "--tray");
        if (tryActivateExistingInstance()) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                new CigaretteDesktopApp().start(startMinimized);
            }
        });
    }

    private static boolean hasFlag(String[] args, String flag) {
        if (args == null) return false;
        for (String a : args) {
            if (flag.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    private static boolean tryActivateExistingInstance() {
        if (!PORT_LOCK_FILE.isFile()) return false;
        try {
            String raw = new String(Files.readAllBytes(PORT_LOCK_FILE.toPath()), StandardCharsets.UTF_8).trim();
            int port = Integer.parseInt(raw);
            Socket s = new Socket(InetAddress.getLoopbackAddress(), port);
            try {
                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                out.println("SHOW");
            } finally {
                s.close();
            }
            return true;
        } catch (Exception ignored) {
            PORT_LOCK_FILE.delete();
            return false;
        }
    }

    private void startSingleInstanceServer() {
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
                    Files.write(PORT_LOCK_FILE.toPath(), String.valueOf(ss.getLocalPort()).getBytes(StandardCharsets.UTF_8));
                    PORT_LOCK_FILE.deleteOnExit();
                    while (!Thread.currentThread().isInterrupted()) {
                        final Socket client = ss.accept();
                        try {
                            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                            String line = br.readLine();
                            if ("SHOW".equalsIgnoreCase(line)) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        showWindow();
                                    }
                                });
                            }
                        } finally {
                            client.close();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, "Cigarette-SingleInstance");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void start(boolean startMinimized) {
        discoveredDirs = MinecraftBridge.discoverGameDirectories(null);
        selectedGameDir = discoveredDirs.get(0);
        reloadActiveProfileFromDisk();

        buildFrame();
        setupSystemTray();
        startSingleInstanceServer();
        refreshEnvironmentAsync();

        Timer pollTimer = new Timer(4500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshEnvironmentAsync();
            }
        });
        pollTimer.start();

        if (startMinimized && trayIcon != null) {
            frame.setVisible(false);
            updateTrayLabel();
        } else {
            showWindow();
        }
    }

    private void buildFrame() {
        frame = new JFrame("Cigarette V1 Companion");
        frame.setUndecorated(true);
        frame.setSize(960, 620);
        frame.setMinimumSize(new Dimension(840, 540));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImage(FaIcon.createAppImage(48, accent));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (hideOnClose && trayIcon != null) {
                    hideToTray(true);
                } else {
                    quitCompletely();
                }
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);

        contentCards = new CardLayout();
        contentArea = new JPanel(contentCards);
        contentArea.setBackground(BG_ROOT);
        contentArea.setBorder(new EmptyBorder(16, 18, 16, 18));

        contentArea.add(buildDashboardTab(), "dashboard");
        contentArea.add(buildModulesTab(), "modules");
        contentArea.add(buildProfilesTab(), "profiles");
        contentArea.add(buildInstallerTab(), "installer");
        contentArea.add(buildSettingsTab(), "settings");

        root.add(contentArea, BorderLayout.CENTER);
        frame.setContentPane(root);
        selectTab("dashboard");
        rebuildModulesGrid();
        rebuildProfilesList();
        rebuildInstallerMods();
    }

    private JPanel buildTitleBar() {
        final JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SIDEBAR);
        bar.setPreferredSize(new Dimension(0, 42));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL));

        final Point[] dragAnchor = new Point[1];
        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragAnchor[0] = e.getPoint();
            }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragAnchor[0] != null) {
                    Point mouse = MouseInfo.getPointerInfo().getLocation();
                    frame.setLocation(mouse.x - dragAnchor[0].x, mouse.y - dragAnchor[0].y);
                }
            }
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 9));
        left.setOpaque(false);
        JLabel logoLbl = new JLabel(FaIcon.of(FaIcon.Glyph.LOGO, 20, accent));
        JLabel titleLbl = new JLabel("CIGARETTE V1");
        titleLbl.setForeground(TEXT_PRIMARY);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        JLabel edPill = makePill(edition.toUpperCase(Locale.ROOT), new Color(0x28, 0x28, 0x33), accent);
        headerStatusPill = makePill("SCANNING...", new Color(0x25, 0x25, 0x2D), TEXT_MUTED);

        left.add(logoLbl);
        left.add(titleLbl);
        left.add(edPill);
        left.add(headerStatusPill);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 7));
        right.setOpaque(false);

        final JButton pinBtn = makeIconButton(FaIcon.Glyph.PIN, "Pin window Always on Top", TEXT_MUTED, BG_CARD);
        pinBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alwaysOnTop = !alwaysOnTop;
                frame.setAlwaysOnTop(alwaysOnTop);
                pinBtn.setIcon(FaIcon.of(FaIcon.Glyph.PIN, 14, alwaysOnTop ? accent : TEXT_MUTED));
            }
        });

        JButton hideBtn = makeActionButton("Hide", FaIcon.Glyph.EYE_SLASH, BG_CARD_ALT, TEXT_PRIMARY);
        hideBtn.setToolTipText("Hide window to background system tray");
        hideBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToTray(true);
            }
        });

        JButton minBtn = makeIconButton(FaIcon.Glyph.MINUS, "Minimize to taskbar", TEXT_MUTED, BG_CARD);
        minBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setState(JFrame.ICONIFIED);
            }
        });

        JButton closeBtn = makeIconButton(FaIcon.Glyph.POWER, "Quit or Hide", CRIMSON, new Color(0x2D, 0x1D, 0x20));
        closeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (hideOnClose && trayIcon != null) {
                    hideToTray(true);
                } else {
                    quitCompletely();
                }
            }
        });

        right.add(pinBtn);
        right.add(hideBtn);
        right.add(minBtn);
        right.add(closeBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(BG_SIDEBAR);
        side.setPreferredSize(new Dimension(196, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COL));

        JPanel navList = new JPanel();
        navList.setOpaque(false);
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBorder(new EmptyBorder(12, 10, 10, 10));

        addNav(navList, "dashboard", "Dashboard", FaIcon.Glyph.GAUGE);
        addNav(navList, "modules", "Modules (41)", FaIcon.Glyph.CUBES);
        addNav(navList, "profiles", "Profiles", FaIcon.Glyph.LAYER_GROUP);
        addNav(navList, "installer", "Mod Installer", FaIcon.Glyph.ROCKET);
        addNav(navList, "settings", "Settings & Tray", FaIcon.Glyph.GEAR);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(10, 12, 12, 12));

        JPanel hintCard = new JPanel(new BorderLayout(0, 4));
        hintCard.setBackground(BG_CARD);
        hintCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(8, 10, 8, 10)));
        JLabel bindTitle = new JLabel("In-Game ClickGUI", FaIcon.of(FaIcon.Glyph.SLIDERS, 13, accent), SwingConstants.LEFT);
        bindTitle.setForeground(TEXT_PRIMARY);
        bindTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel bindSub = new JLabel("Press Right Shift in-world", SwingConstants.LEFT);
        bindSub.setForeground(TEXT_MUTED);
        bindSub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hintCard.add(bindTitle, BorderLayout.NORTH);
        hintCard.add(bindSub, BorderLayout.SOUTH);

        JButton quitSide = makeActionButton("Exit Companion", FaIcon.Glyph.POWER, new Color(0x28, 0x1A, 0x1D), CRIMSON);
        quitSide.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        quitSide.setAlignmentX(Component.LEFT_ALIGNMENT);
        quitSide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitCompletely();
            }
        });

        hintCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        footer.add(hintCard);
        footer.add(Box.createVerticalStrut(8));
        footer.add(quitSide);

        side.add(navList, BorderLayout.NORTH);
        side.add(footer, BorderLayout.SOUTH);
        return side;
    }

    private void addNav(JPanel parent, final String id, String label, FaIcon.Glyph glyph) {
        NavButton btn = new NavButton(label, glyph);
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab(id);
            }
        });
        navButtons.put(id, btn);
        parent.add(btn);
        parent.add(Box.createVerticalStrut(5));
    }

    private JPanel buildDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        dashBannerCard = new JPanel(new BorderLayout(12, 8));
        dashBannerCard.setBackground(BG_CARD);
        dashBannerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(14, 16, 14, 16)));

        dashBannerTitle = new JLabel("Checking running Minecraft instances...", FaIcon.of(FaIcon.Glyph.TERMINAL, 16, accent), SwingConstants.LEFT);
        dashBannerTitle.setForeground(TEXT_PRIMARY);
        dashBannerTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        dashBannerBody = new JLabel("<html>Scanning Java processes &amp; launcher folders...</html>");
        dashBannerBody.setForeground(TEXT_MUTED);
        dashBannerBody.setFont(new Font("SansSerif", Font.PLAIN, 12));

        dashOpenClickGuiBtn = makeActionButton("Open ClickGUI In-Game", FaIcon.Glyph.PLAY, accent, Color.WHITE);
        dashOpenClickGuiBtn.setVisible(false);
        dashOpenClickGuiBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                triggerOpenClickGuiInGame();
            }
        });

        JPanel textStack = new JPanel(new BorderLayout(0, 6));
        textStack.setOpaque(false);
        textStack.add(dashBannerTitle, BorderLayout.NORTH);
        textStack.add(dashBannerBody, BorderLayout.CENTER);
        dashBannerCard.add(textStack, BorderLayout.CENTER);
        dashBannerCard.add(dashOpenClickGuiBtn, BorderLayout.EAST);

        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        statsGrid.setOpaque(false);
        statModulesLbl = new JLabel("2 / 41");
        statProfileLbl = new JLabel("default");
        statThemeLbl = new JLabel(themePreset);
        statInstallLbl = new JLabel("Checking...");
        statsGrid.add(makeStatCard("Enabled Modules", FaIcon.Glyph.CUBES, statModulesLbl));
        statsGrid.add(makeStatCard("Active Profile", FaIcon.Glyph.LAYER_GROUP, statProfileLbl));
        statsGrid.add(makeStatCard("Theme Preset", FaIcon.Glyph.PALETTE, statThemeLbl));
        statsGrid.add(makeStatCard("Mod Folder", FaIcon.Glyph.DOWNLOAD, statInstallLbl));

        JPanel dirRow = new JPanel(new BorderLayout(10, 0));
        dirRow.setBackground(BG_CARD);
        dirRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(10, 14, 10, 14)));
        JLabel dirLbl = new JLabel("Target Minecraft Directory", FaIcon.of(FaIcon.Glyph.FOLDER_OPEN, 14, accent), SwingConstants.LEFT);
        dirLbl.setForeground(TEXT_PRIMARY);
        dirLbl.setFont(new Font("SansSerif", Font.BOLD, 12));

        gameDirCombo = new JComboBox<File>();
        styleCombo(gameDirCombo);
        populateGameDirCombo();
        gameDirCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object sel = gameDirCombo.getSelectedItem();
                if (sel instanceof File && !sel.equals(selectedGameDir)) {
                    selectedGameDir = (File) sel;
                    reloadActiveProfileFromDisk();
                    rebuildModulesGrid();
                    rebuildProfilesList();
                    rebuildInstallerMods();
                    updateStatsLabels();
                }
            }
        });
        dirRow.add(dirLbl, BorderLayout.WEST);
        dirRow.add(gameDirCombo, BorderLayout.CENTER);

        JPanel actionsCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        actionsCard.setBackground(BG_CARD);
        actionsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(6, 8, 6, 8)));

        JButton installNow = makeActionButton("Install / Update in mods/", FaIcon.Glyph.DOWNLOAD, accent, Color.WHITE);
        installNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runInstallToSelectedDir();
            }
        });
        JButton hideTrayNow = makeActionButton("Hide App to Background", FaIcon.Glyph.EYE_SLASH, BG_CARD_ALT, TEXT_PRIMARY);
        hideTrayNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToTray(true);
            }
        });
        JButton openModsNow = makeActionButton("Open mods/ Folder", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        openModsNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedGameDir != null) MinecraftBridge.openInExplorer(new File(selectedGameDir, "mods"));
            }
        });
        JButton rescanNow = makeActionButton("Rescan Processes", FaIcon.Glyph.SYNC, BG_CARD_ALT, TEXT_PRIMARY);
        rescanNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshEnvironmentAsync();
            }
        });
        actionsCard.add(installNow);
        actionsCard.add(hideTrayNow);
        actionsCard.add(openModsNow);
        actionsCard.add(rescanNow);

        JPanel guideCard = new JPanel(new BorderLayout(0, 6));
        guideCard.setBackground(BG_CARD);
        guideCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(12, 14, 12, 14)));
        JLabel guideTitle = new JLabel("How Live Background Sync & Launch Works", FaIcon.of(FaIcon.Glyph.WARNING, 14, AMBER), SwingConstants.LEFT);
        guideTitle.setForeground(TEXT_PRIMARY);
        guideTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel guideText = new JLabel("<html>1. Click <b>Install / Update in mods/</b> to place this JAR into your Minecraft/TLauncher <code>mods/</code> folder.<br>"
                + "2. Use <b>Java 8</b> for <b>1.8.9 Forge</b> (and on older Windows Intel GPUs, set Win7/Win8 compatibility on <code>javaw.exe</code> if LWJGL throws <i>Pixel format not accelerated</i>). Use <b>Java 25+</b> for <b>26.3 Fabric</b>.<br>"
                + "3. While in-game, press <b>Right Shift</b> for the ClickGUI — or keep this app minimized in the system tray to toggle modules &amp; themes live.</html>");
        guideText.setForeground(TEXT_MUTED);
        guideText.setFont(new Font("SansSerif", Font.PLAIN, 11));
        guideCard.add(guideTitle, BorderLayout.NORTH);
        guideCard.add(guideText, BorderLayout.CENTER);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        dashBannerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        guideCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        stack.add(dashBannerCard);
        stack.add(Box.createVerticalStrut(12));
        stack.add(statsGrid);
        stack.add(Box.createVerticalStrut(12));
        stack.add(dirRow);
        stack.add(Box.createVerticalStrut(12));
        stack.add(actionsCard);
        stack.add(Box.createVerticalStrut(12));
        stack.add(guideCard);

        panel.add(stack, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildModulesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setOpaque(false);

        final JPanel catRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        catRow.setOpaque(false);
        final List<JButton> catBtns = new ArrayList<JButton>();

        JButton allBtn = makeActionButton("All", FaIcon.Glyph.CUBES, accent, Color.WHITE);
        allBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moduleCatFilter = null;
                refreshCategoryChips(catBtns, allBtn);
                rebuildModulesGrid();
            }
        });
        catBtns.add(allBtn);
        catRow.add(allBtn);

        for (final DesktopCatalog.CategoryType cat : DesktopCatalog.CategoryType.values()) {
            final JButton cb = makeActionButton(cat.getLabel(), cat.getIcon(), BG_CARD, TEXT_MUTED);
            cb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moduleCatFilter = cat;
                    refreshCategoryChips(catBtns, cb);
                    rebuildModulesGrid();
                }
            });
            catBtns.add(cb);
            catRow.add(cb);
        }

        JPanel searchWrap = new JPanel(new BorderLayout(8, 0));
        searchWrap.setBackground(BG_CARD);
        searchWrap.setPreferredSize(new Dimension(210, 32));
        searchWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(4, 10, 4, 8)));
        searchWrap.add(new JLabel(FaIcon.of(FaIcon.Glyph.SEARCH, 13, TEXT_MUTED)), BorderLayout.WEST);
        moduleSearchField = new JTextField();
        moduleSearchField.setBackground(BG_CARD);
        moduleSearchField.setForeground(TEXT_PRIMARY);
        moduleSearchField.setCaretColor(accent);
        moduleSearchField.setBorder(null);
        moduleSearchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        moduleSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { rebuildModulesGrid(); }
            @Override public void removeUpdate(DocumentEvent e) { rebuildModulesGrid(); }
            @Override public void changedUpdate(DocumentEvent e) { rebuildModulesGrid(); }
        });
        searchWrap.add(moduleSearchField, BorderLayout.CENTER);

        top.add(catRow, BorderLayout.CENTER);
        top.add(searchWrap, BorderLayout.EAST);

        modulesGrid = new JPanel(new GridLayout(0, 2, 10, 10));
        modulesGrid.setBackground(BG_ROOT);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_ROOT);
        wrap.add(modulesGrid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_ROOT);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void refreshCategoryChips(List<JButton> all, JButton selected) {
        for (JButton b : all) {
            boolean active = b == selected;
            b.setBackground(active ? accent : BG_CARD);
            b.setForeground(active ? Color.WHITE : TEXT_MUTED);
        }
    }

    private JPanel buildProfilesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel createBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        createBar.setBackground(BG_CARD);
        createBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(4, 8, 4, 8)));

        newProfileField = new JTextField(18);
        newProfileField.setBackground(BG_ROOT);
        newProfileField.setForeground(TEXT_PRIMARY);
        newProfileField.setCaretColor(accent);
        newProfileField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(5, 8, 5, 8)));

        JButton createBtn = makeActionButton("Create & Save", FaIcon.Glyph.PLUS, accent, Color.WHITE);
        createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = newProfileField.getText().trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
                if (name.isEmpty()) return;
                activeProfileName = name;
                activeProfileData.put("profile_name", name);
                MinecraftBridge.saveProfileAndSignal(getProfilesDir(), name, activeProfileData, "reload");
                newProfileField.setText("");
                rebuildProfilesList();
                updateStatsLabels();
            }
        });

        JButton syncBtn = makeActionButton("Sync to Game", FaIcon.Glyph.SYNC, BG_CARD_ALT, TEXT_PRIMARY);
        syncBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "reload");
            }
        });

        JButton openProfBtn = makeActionButton("Open Folder", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        openProfBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MinecraftBridge.openInExplorer(getProfilesDir());
            }
        });

        createBar.add(new JLabel(FaIcon.of(FaIcon.Glyph.LAYER_GROUP, 14, accent)));
        createBar.add(newProfileField);
        createBar.add(createBtn);
        createBar.add(syncBtn);
        createBar.add(openProfBtn);

        profilesListPanel = new JPanel();
        profilesListPanel.setBackground(BG_ROOT);
        profilesListPanel.setLayout(new BoxLayout(profilesListPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(profilesListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_ROOT);

        panel.add(createBar, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInstallerTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(10, 8));
        top.setBackground(BG_CARD);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel("Minecraft / TLauncher Mod Installer", FaIcon.of(FaIcon.Glyph.ROCKET, 16, accent), SwingConstants.LEFT);
        title.setForeground(TEXT_PRIMARY);
        title.setFont(new Font("SansSerif", Font.BOLD, 14));

        installerConflictLabel = new JLabel("No conflicting edition JARs detected.");
        installerConflictLabel.setForeground(TEXT_MUTED);
        installerConflictLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        JButton installBtn = makeActionButton("Install / Update This JAR", FaIcon.Glyph.DOWNLOAD, accent, Color.WHITE);
        installBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runInstallToSelectedDir();
            }
        });
        JButton browseBtn = makeActionButton("Browse Game Folder", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        browseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(selectedGameDir);
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File chosen = chooser.getSelectedFile();
                    if (!discoveredDirs.contains(chosen)) discoveredDirs.add(0, chosen);
                    selectedGameDir = chosen;
                    populateGameDirCombo();
                    rebuildInstallerMods();
                    rebuildModulesGrid();
                    rebuildProfilesList();
                }
            }
        });
        JButton cleanBtn = makeActionButton("Fix Version Conflict", FaIcon.Glyph.TRASH, new Color(0x2F, 0x1F, 0x22), CRIMSON);
        cleanBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedGameDir == null) return;
                int removed = MinecraftBridge.removeConflictingJars(new File(selectedGameDir, "mods"), edition);
                rebuildInstallerMods();
                JOptionPane.showMessageDialog(frame, "Removed " + removed + " incompatible Cigarette JAR(s).");
            }
        });
        btns.add(installBtn);
        btns.add(browseBtn);
        btns.add(cleanBtn);

        top.add(title, BorderLayout.NORTH);
        top.add(installerConflictLabel, BorderLayout.CENTER);
        top.add(btns, BorderLayout.SOUTH);

        installerModsPanel = new JPanel();
        installerModsPanel.setBackground(BG_ROOT);
        installerModsPanel.setLayout(new BoxLayout(installerModsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(installerModsPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_ROOT);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSettingsTab() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel themeCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        themeCard.setBackground(BG_CARD);
        themeCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(8, 10, 8, 10)));
        themeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel themeLbl = new JLabel("Accent Theme:", FaIcon.of(FaIcon.Glyph.PALETTE, 14, accent), SwingConstants.LEFT);
        themeLbl.setForeground(TEXT_PRIMARY);
        themeLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        themeCard.add(themeLbl);

        for (final String preset : DesktopCatalog.THEME_PRESETS) {
            Color c = DesktopCatalog.getThemeColor(preset);
            JButton tb = makeActionButton(preset, FaIcon.Glyph.PALETTE, BG_CARD_ALT, c);
            tb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyThemePreset(preset, true);
                }
            });
            themeCard.add(tb);
        }

        JPanel trayCard = new JPanel();
        trayCard.setBackground(BG_CARD);
        trayCard.setLayout(new BoxLayout(trayCard, BoxLayout.Y_AXIS));
        trayCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(12, 14, 12, 14)));
        trayCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        trayCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        final JCheckBox hideCloseChk = makeDarkCheck("Close button [X] hides app to background System Tray (recommended)", hideOnClose);
        hideCloseChk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideOnClose = hideCloseChk.isSelected();
            }
        });

        final JCheckBox syncChk = makeDarkCheck("Live-sync module & theme changes directly into running Minecraft", autoLiveSync);
        syncChk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoLiveSync = syncChk.isSelected();
            }
        });

        trayStatusLabel = new JLabel(SystemTray.isSupported()
                ? "System Tray: Active (click tray icon anytime to show or hide)"
                : "System Tray: Unavailable in this environment (use Hide/Minimize)");
        trayStatusLabel.setForeground(TEXT_MUTED);
        trayStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel trayBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        trayBtns.setOpaque(false);
        JButton hideBtn = makeActionButton("Hide to Background Now", FaIcon.Glyph.EYE_SLASH, BG_CARD_ALT, TEXT_PRIMARY);
        hideBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToTray(true);
            }
        });
        JButton exitBtn = makeActionButton("Quit Application Completely", FaIcon.Glyph.POWER, new Color(0x2E, 0x1A, 0x1E), CRIMSON);
        exitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitCompletely();
            }
        });
        trayBtns.add(hideBtn);
        trayBtns.add(exitBtn);

        trayCard.add(hideCloseChk);
        trayCard.add(Box.createVerticalStrut(6));
        trayCard.add(syncChk);
        trayCard.add(Box.createVerticalStrut(8));
        trayCard.add(trayStatusLabel);
        trayCard.add(Box.createVerticalStrut(6));
        trayCard.add(trayBtns);

        panel.add(themeCard);
        panel.add(Box.createVerticalStrut(12));
        panel.add(trayCard);
        return panel;
    }

    private void rebuildModulesGrid() {
        if (modulesGrid == null) return;
        modulesGrid.removeAll();
        String query = moduleSearchField != null ? moduleSearchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        Map<String, Object> modulesMap = getModulesSection(activeProfileData);

        for (final DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            if (moduleCatFilter != null && def.category != moduleCatFilter) continue;
            if (!query.isEmpty() && !def.name.toLowerCase(Locale.ROOT).contains(query)
                    && !def.description.toLowerCase(Locale.ROOT).contains(query)
                    && !def.category.getLabel().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            Map<String, Object> md = getOrCreateModuleMap(modulesMap, def);
            final boolean enabled = Boolean.TRUE.equals(md.get("enabled"));

            JPanel card = new JPanel(new BorderLayout(8, 4));
            card.setBackground(enabled ? BG_CARD_ALT : BG_CARD);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, enabled ? 3 : 1, 1, 1, enabled ? accent : BORDER_COL),
                    new EmptyBorder(10, 12, 10, 12)));

            JPanel titleRow = new JPanel(new BorderLayout(6, 0));
            titleRow.setOpaque(false);
            JLabel nameLbl = new JLabel(def.name, FaIcon.of(def.category.getIcon(), 13, enabled ? accent : TEXT_MUTED), SwingConstants.LEFT);
            nameLbl.setForeground(TEXT_PRIMARY);
            nameLbl.setFont(new Font("SansSerif", Font.BOLD, 13));

            final JButton toggleBtn = makeActionButton(enabled ? "ON" : "OFF",
                    enabled ? FaIcon.Glyph.CHECK : FaIcon.Glyph.MINUS,
                    enabled ? accent : BG_ROOT,
                    enabled ? Color.WHITE : TEXT_MUTED);
            toggleBtn.setPreferredSize(new Dimension(70, 24));
            toggleBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean next = !Boolean.TRUE.equals(getOrCreateModuleMap(getModulesSection(activeProfileData), def).get("enabled"));
                    getOrCreateModuleMap(getModulesSection(activeProfileData), def).put("enabled", Boolean.valueOf(next));
                    saveActiveIfLive();
                    rebuildModulesGrid();
                    updateStatsLabels();
                }
            });

            titleRow.add(nameLbl, BorderLayout.CENTER);
            titleRow.add(toggleBtn, BorderLayout.EAST);

            JLabel descLbl = new JLabel("<html><body style='width:220px'>" + escapeHtml(def.description) + "</body></html>");
            descLbl.setForeground(TEXT_MUTED);
            descLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));

            JPanel bottomRow = new JPanel(new BorderLayout());
            bottomRow.setOpaque(false);
            JLabel catTag = new JLabel(def.category.getLabel().toUpperCase(Locale.ROOT));
            catTag.setForeground(enabled ? accent : TEXT_MUTED);
            catTag.setFont(new Font("SansSerif", Font.BOLD, 10));

            JButton editBtn = makeActionButton("Settings", FaIcon.Glyph.SLIDERS, BG_ROOT, TEXT_PRIMARY);
            editBtn.setPreferredSize(new Dimension(92, 22));
            editBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openModuleSettingsDialog(def);
                }
            });

            bottomRow.add(catTag, BorderLayout.WEST);
            bottomRow.add(editBtn, BorderLayout.EAST);

            card.add(titleRow, BorderLayout.NORTH);
            card.add(descLbl, BorderLayout.CENTER);
            card.add(bottomRow, BorderLayout.SOUTH);
            modulesGrid.add(card);
        }
        modulesGrid.revalidate();
        modulesGrid.repaint();
    }

    @SuppressWarnings("unchecked")
    private void openModuleSettingsDialog(final DesktopCatalog.ModuleDef def) {
        final Map<String, Object> md = getOrCreateModuleMap(getModulesSection(activeProfileData), def);
        Object rawSettings = md.get("settings");
        final Map<String, Object> settings = rawSettings instanceof Map
                ? (Map<String, Object>) rawSettings
                : new LinkedHashMap<String, Object>();
        md.put("settings", settings);

        final JDialog dlg = new JDialog(frame, def.name + " Settings", true);
        dlg.setSize(420, 420);
        dlg.setLocationRelativeTo(frame);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BG_ROOT);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel hdr = new JLabel(def.name + " (" + def.category.getLabel() + ")", FaIcon.of(def.category.getIcon(), 16, accent), SwingConstants.LEFT);
        hdr.setForeground(TEXT_PRIMARY);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel body = new JPanel();
        body.setBackground(BG_ROOT);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        final JCheckBox hiddenChk = makeDarkCheck("Hide from HUD ArrayList", Boolean.TRUE.equals(md.get("hidden")));
        body.add(hiddenChk);
        body.add(Box.createVerticalStrut(8));

        final List<Runnable> appliers = new ArrayList<Runnable>();
        if (settings.isEmpty()) {
            JLabel note = new JLabel("<html>No saved per-setting overrides yet in <b>" + escapeHtml(activeProfileName) + ".json</b>.<br>Launch in-game once to populate all slider/mode defaults for " + escapeHtml(def.name) + ", then edit live here.</html>");
            note.setForeground(TEXT_MUTED);
            note.setFont(new Font("SansSerif", Font.PLAIN, 11));
            body.add(note);
        } else {
            for (final Map.Entry<String, Object> entry : settings.entrySet()) {
                final String key = entry.getKey();
                final Object val = entry.getValue();
                if (val instanceof Boolean) {
                    final JCheckBox cb = makeDarkCheck(key, ((Boolean) val).booleanValue());
                    body.add(cb);
                    body.add(Box.createVerticalStrut(4));
                    appliers.add(new Runnable() {
                        @Override public void run() { settings.put(key, Boolean.valueOf(cb.isSelected())); }
                    });
                } else if (val instanceof Number && !key.endsWith("_speed")) {
                    JPanel row = new JPanel(new BorderLayout(8, 0));
                    row.setOpaque(false);
                    JLabel lbl = new JLabel(key);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    final JTextField tf = new JTextField(String.valueOf(val), 8);
                    tf.setBackground(BG_CARD);
                    tf.setForeground(TEXT_PRIMARY);
                    tf.setCaretColor(accent);
                    tf.setBorder(BorderFactory.createLineBorder(BORDER_COL));
                    row.add(lbl, BorderLayout.CENTER);
                    row.add(tf, BorderLayout.EAST);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                    body.add(row);
                    body.add(Box.createVerticalStrut(4));
                    appliers.add(new Runnable() {
                        @Override public void run() {
                            try {
                                settings.put(key, Double.valueOf(Double.parseDouble(tf.getText().trim())));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    });
                } else if (val instanceof String) {
                    JPanel row = new JPanel(new BorderLayout(8, 0));
                    row.setOpaque(false);
                    JLabel lbl = new JLabel(key);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    final JTextField tf = new JTextField((String) val, 10);
                    tf.setBackground(BG_CARD);
                    tf.setForeground(TEXT_PRIMARY);
                    tf.setCaretColor(accent);
                    tf.setBorder(BorderFactory.createLineBorder(BORDER_COL));
                    row.add(lbl, BorderLayout.CENTER);
                    row.add(tf, BorderLayout.EAST);
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                    body.add(row);
                    body.add(Box.createVerticalStrut(4));
                    appliers.add(new Runnable() {
                        @Override public void run() { settings.put(key, tf.getText().trim()); }
                    });
                }
            }
        }

        JButton saveBtn = makeActionButton("Save & Sync Live", FaIcon.Glyph.SAVE, accent, Color.WHITE);
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                md.put("hidden", Boolean.valueOf(hiddenChk.isSelected()));
                for (Runnable r : appliers) r.run();
                saveActiveIfLive();
                dlg.dispose();
                rebuildModulesGrid();
            }
        });

        root.add(hdr, BorderLayout.NORTH);
        JScrollPane sc = new JScrollPane(body);
        sc.setBorder(null);
        sc.getViewport().setBackground(BG_ROOT);
        root.add(sc, BorderLayout.CENTER);
        root.add(saveBtn, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void rebuildProfilesList() {
        if (profilesListPanel == null) return;
        profilesListPanel.removeAll();
        File dir = getProfilesDir();
        for (final String name : MinecraftBridge.listProfileNames(dir)) {
            boolean active = name.equalsIgnoreCase(activeProfileName);
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(active ? BG_CARD_ALT : BG_CARD);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, active ? 3 : 1, 1, 1, active ? accent : BORDER_COL),
                    new EmptyBorder(8, 12, 8, 12)));

            JLabel lbl = new JLabel(name + (active ? "  [ACTIVE]" : ""), FaIcon.of(FaIcon.Glyph.LAYER_GROUP, 14, active ? accent : TEXT_MUTED), SwingConstants.LEFT);
            lbl.setForeground(TEXT_PRIMARY);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);
            JButton loadBtn = makeActionButton("Load & Sync", FaIcon.Glyph.SYNC, active ? accent : BG_ROOT, active ? Color.WHITE : TEXT_PRIMARY);
            loadBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activeProfileName = name;
                    reloadActiveProfileFromDisk();
                    MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "reload");
                    rebuildModulesGrid();
                    rebuildProfilesList();
                    updateStatsLabels();
                }
            });
            actions.add(loadBtn);

            if (!"default".equalsIgnoreCase(name)) {
                JButton delBtn = makeIconButton(FaIcon.Glyph.TRASH, "Delete profile", CRIMSON, new Color(0x2B, 0x1C, 0x20));
                delBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        MinecraftBridge.deleteProfile(getProfilesDir(), name);
                        if (name.equalsIgnoreCase(activeProfileName)) {
                            activeProfileName = "default";
                            reloadActiveProfileFromDisk();
                        }
                        rebuildProfilesList();
                        rebuildModulesGrid();
                        updateStatsLabels();
                    }
                });
                actions.add(delBtn);
            }

            row.add(lbl, BorderLayout.CENTER);
            row.add(actions, BorderLayout.EAST);
            profilesListPanel.add(row);
            profilesListPanel.add(Box.createVerticalStrut(6));
        }
        profilesListPanel.revalidate();
        profilesListPanel.repaint();
    }

    private void rebuildInstallerMods() {
        if (installerModsPanel == null) return;
        installerModsPanel.removeAll();
        File modsDir = selectedGameDir != null ? new File(selectedGameDir, "mods") : null;
        List<File> jars = MinecraftBridge.listCigaretteJarsInMods(modsDir);

        boolean has189 = false;
        boolean has263 = false;
        if (jars.isEmpty()) {
            JLabel empty = new JLabel("No Cigarette JARs currently installed in " + (modsDir != null ? modsDir.getAbsolutePath() : "mods/"));
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(new EmptyBorder(10, 4, 10, 4));
            installerModsPanel.add(empty);
        } else {
            for (File f : jars) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                boolean modern = n.contains("26.3") || n.contains("fabric");
                if (modern) has263 = true;
                else has189 = true;

                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(BG_CARD);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COL, 1),
                        new EmptyBorder(8, 12, 8, 12)));
                JLabel lbl = new JLabel(f.getName() + "  (" + (f.length() / 1024) + " KB)",
                        FaIcon.of(FaIcon.Glyph.CHECK, 13, EMERALD), SwingConstants.LEFT);
                lbl.setForeground(TEXT_PRIMARY);
                lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                JLabel tag = makePill(modern ? "26.3 FABRIC" : "1.8.9 FORGE", BG_ROOT, modern ? accent : EMBER_FALLBACK());
                row.add(lbl, BorderLayout.CENTER);
                row.add(tag, BorderLayout.EAST);
                installerModsPanel.add(row);
                installerModsPanel.add(Box.createVerticalStrut(6));
            }
        }

        if (has189 && has263) {
            installerConflictLabel.setText("Conflict detected: both 1.8.9 and 26.3 JARs exist in this mods/ folder!");
            installerConflictLabel.setForeground(CRIMSON);
        } else if (jars.isEmpty()) {
            installerConflictLabel.setText("Ready to install " + edition + " into " + (modsDir != null ? modsDir.getAbsolutePath() : "mods/"));
            installerConflictLabel.setForeground(AMBER);
        } else {
            installerConflictLabel.setText("Clean installation in " + (modsDir != null ? modsDir.getAbsolutePath() : "mods/"));
            installerConflictLabel.setForeground(EMERALD);
        }

        installerModsPanel.revalidate();
        installerModsPanel.repaint();
    }

    private Color EMBER_FALLBACK() {
        return new Color(0xFF, 0x57, 0x22);
    }

    private void refreshEnvironmentAsync() {
        if (scanInFlight) return;
        scanInFlight = true;
        new SwingWorker<List<MinecraftBridge.RunningProcess>, Void>() {
            @Override
            protected List<MinecraftBridge.RunningProcess> doInBackground() {
                return MinecraftBridge.scanRunningMinecraft();
            }

            @Override
            protected void done() {
                scanInFlight = false;
                try {
                    runningProcesses = get();
                    discoveredDirs = MinecraftBridge.discoverGameDirectories(runningProcesses);
                    if (!discoveredDirs.isEmpty() && (selectedGameDir == null || !discoveredDirs.contains(selectedGameDir))) {
                        selectedGameDir = discoveredDirs.get(0);
                    }
                    populateGameDirCombo();
                    updateLiveStatusUI();
                    updateStatsLabels();
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void updateLiveStatusUI() {
        MinecraftBridge.RunningProcess mcProc = null;
        MinecraftBridge.RunningProcess launcherProc = null;
        for (MinecraftBridge.RunningProcess p : runningProcesses) {
            if (p.kind.startsWith("Minecraft") && mcProc == null) mcProc = p;
            else if (launcherProc == null) launcherProc = p;
        }

        if (mcProc != null) {
            if (mcProc.cigaretteLoaded) {
                headerStatusPill.setText("GAME RUNNING • LIVE SYNC");
                headerStatusPill.setForeground(EMERALD);
                dashBannerTitle.setText("Minecraft Running (PID " + mcProc.pid + ") — Cigarette Loaded");
                dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.CHECK, 16, EMERALD));
                dashBannerBody.setText("<html>Live profile sync is connected. Toggle modules here or click <b>Open ClickGUI In-Game</b> (or press <b>Right Shift</b> in Minecraft).</html>");
                dashOpenClickGuiBtn.setVisible(true);
            } else {
                headerStatusPill.setText("GAME RUNNING (MOD NOT LOADED)");
                headerStatusPill.setForeground(AMBER);
                dashBannerTitle.setText(mcProc.kind + " Running (PID " + mcProc.pid + ") — Install Needed");
                dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.WARNING, 16, AMBER));
                dashBannerBody.setText("<html>Minecraft started without Cigarette in <code>mods/</code> (double-clicking a JAR opens this companion GUI). Click <b>Install / Update in mods/</b>, restart Minecraft, and press <b>Right Shift</b> in-game.</html>");
                dashOpenClickGuiBtn.setVisible(false);
            }
        } else if (launcherProc != null) {
            headerStatusPill.setText("LAUNCHER OPEN (" + launcherProc.kind.toUpperCase(Locale.ROOT) + ")");
            headerStatusPill.setForeground(accent);
            dashBannerTitle.setText("Launcher Detected (PID " + launcherProc.pid + ") — Ready");
            dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.ROCKET, 16, accent));
            dashBannerBody.setText("<html>Your launcher is open. Verify Cigarette is installed below, keep this app hidden in the tray if desired, and launch the game!</html>");
            dashOpenClickGuiBtn.setVisible(false);
        } else {
            headerStatusPill.setText("MINECRAFT OFFLINE");
            headerStatusPill.setForeground(TEXT_MUTED);
            dashBannerTitle.setText("Companion Ready — Background Tray Active");
            dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.GAUGE, 16, accent));
            dashBannerBody.setText("<html>Configure your modules &amp; profiles offline anytime, install into <code>mods/</code>, or hide this app to the tray and press <b>Right Shift</b> in-game.</html>");
            dashOpenClickGuiBtn.setVisible(false);
        }
        rebuildInstallerMods();
    }

    private void populateGameDirCombo() {
        if (gameDirCombo == null) return;
        DefaultComboBoxModel<File> model = new DefaultComboBoxModel<File>();
        for (File f : discoveredDirs) {
            model.addElement(f);
        }
        gameDirCombo.setModel(model);
        if (selectedGameDir != null) {
            gameDirCombo.setSelectedItem(selectedGameDir);
        }
    }

    private void runInstallToSelectedDir() {
        if (selectedGameDir == null) return;
        MinecraftBridge.InstallResult res = MinecraftBridge.installSelfToMods(selectedGameDir, edition);
        rebuildInstallerMods();
        updateStatsLabels();
        JOptionPane.showMessageDialog(frame, res.message, res.ok ? "Installed" : "Install Failed",
                res.ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    private void triggerOpenClickGuiInGame() {
        MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "open_clickgui");
        if (trayIcon != null) {
            trayIcon.displayMessage("Cigarette V1", "Sent Open ClickGUI signal to running Minecraft.", TrayIcon.MessageType.INFO);
        }
    }

    private File getProfilesDir() {
        return MinecraftBridge.getProfilesDir(selectedGameDir != null ? selectedGameDir : new File("."), edition);
    }

    @SuppressWarnings("unchecked")
    private void reloadActiveProfileFromDisk() {
        activeProfileData = MinecraftBridge.loadProfileMap(getProfilesDir(), activeProfileName);
        Object uiObj = activeProfileData.get("ui");
        if (uiObj instanceof Map) {
            Object tp = ((Map<String, Object>) uiObj).get("themePreset");
            if (tp instanceof String) {
                applyThemePreset((String) tp, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void saveActiveIfLive() {
        if (autoLiveSync) {
            MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "reload");
        } else {
            MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "none");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getModulesSection(Map<String, Object> root) {
        Object m = root.get("modules");
        if (m instanceof Map) return (Map<String, Object>) m;
        Map<String, Object> created = new LinkedHashMap<String, Object>();
        root.put("modules", created);
        return created;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateModuleMap(Map<String, Object> modulesMap, DesktopCatalog.ModuleDef def) {
        Object existing = modulesMap.get(def.name);
        if (existing instanceof Map) return (Map<String, Object>) existing;
        Map<String, Object> md = new LinkedHashMap<String, Object>();
        md.put("enabled", Boolean.valueOf(def.defaultEnabled));
        md.put("keyCode", Integer.valueOf(0));
        md.put("hidden", Boolean.FALSE);
        md.put("settings", new LinkedHashMap<String, Object>());
        modulesMap.put(def.name, md);
        return md;
    }

    @SuppressWarnings("unchecked")
    private void applyThemePreset(String preset, boolean persist) {
        this.themePreset = preset;
        this.accent = DesktopCatalog.getThemeColor(preset);
        Object uiObj = activeProfileData.get("ui");
        Map<String, Object> ui = uiObj instanceof Map ? (Map<String, Object>) uiObj : new LinkedHashMap<String, Object>();
        ui.put("themePreset", preset);
        ui.put("accentColor", Integer.valueOf(accent.getRGB()));
        activeProfileData.put("ui", ui);

        if (frame != null) {
            frame.setIconImage(FaIcon.createAppImage(48, accent));
        }
        if (trayIcon != null) {
            trayIcon.setImage(FaIcon.createAppImage(32, accent));
        }
        updateStatsLabels();
        selectTab(activeTab);
        rebuildModulesGrid();
        rebuildProfilesList();
        if (persist) {
            saveActiveIfLive();
        }
    }

    private void updateStatsLabels() {
        if (statModulesLbl == null) return;
        int enabled = 0;
        Map<String, Object> modules = getModulesSection(activeProfileData);
        for (DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            Object raw = modules.get(def.name);
            if (raw instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) raw).get("enabled"))) {
                enabled++;
            }
        }
        statModulesLbl.setText(enabled + " / " + DesktopCatalog.getModules().size());
        statProfileLbl.setText(activeProfileName);
        statThemeLbl.setText(themePreset);

        File modsDir = selectedGameDir != null ? new File(selectedGameDir, "mods") : null;
        int count = MinecraftBridge.listCigaretteJarsInMods(modsDir).size();
        statInstallLbl.setText(count > 0 ? "Installed (" + count + ")" : "Not Installed");
        statInstallLbl.setForeground(count > 0 ? EMERALD : AMBER);
    }

    private void selectTab(String id) {
        activeTab = id;
        if (contentCards != null) contentCards.show(contentArea, id);
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(id), accent);
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        try {
            SystemTray tray = SystemTray.getSystemTray();
            PopupMenu menu = new PopupMenu();

            trayToggleItem = new MenuItem("Hide Window");
            trayToggleItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleVisibility();
                }
            });

            MenuItem openGuiItem = new MenuItem("Open ClickGUI In-Game");
            openGuiItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    triggerOpenClickGuiInGame();
                }
            });

            Menu quickJump = new Menu("Open Tab");
            addTrayJump(quickJump, "Dashboard", "dashboard");
            addTrayJump(quickJump, "Modules", "modules");
            addTrayJump(quickJump, "Profiles", "profiles");
            addTrayJump(quickJump, "Mod Installer", "installer");

            MenuItem exitItem = new MenuItem("Quit Cigarette Companion");
            exitItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    quitCompletely();
                }
            });

            menu.add(trayToggleItem);
            menu.add(openGuiItem);
            menu.add(quickJump);
            menu.addSeparator();
            menu.add(exitItem);

            trayIcon = new TrayIcon(FaIcon.createAppImage(32, accent), "Cigarette V1 (" + edition + ")", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleVisibility();
                }
            });
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        toggleVisibility();
                    }
                }
            });
            tray.add(trayIcon);
        } catch (Throwable ignored) {
            trayIcon = null;
        }
    }

    private void addTrayJump(Menu menu, String label, final String tab) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showWindow();
                selectTab(tab);
            }
        });
        menu.add(item);
    }

    private void toggleVisibility() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (frame.isVisible()) {
                    hideToTray(false);
                } else {
                    showWindow();
                }
            }
        });
    }

    private void showWindow() {
        frame.setVisible(true);
        frame.setState(JFrame.NORMAL);
        frame.toFront();
        frame.requestFocus();
        updateTrayLabel();
    }

    private void hideToTray(boolean showHint) {
        if (trayIcon == null) {
            frame.setState(JFrame.ICONIFIED);
            return;
        }
        frame.setVisible(false);
        updateTrayLabel();
        if (showHint) {
            trayIcon.displayMessage("Cigarette V1 Running in Background",
                    "Click the Cigarette tray icon anytime to open or hide.", TrayIcon.MessageType.INFO);
        }
    }

    private void updateTrayLabel() {
        if (trayToggleItem != null) {
            trayToggleItem.setLabel(frame.isVisible() ? "Hide to Background" : "Show Cigarette GUI");
        }
    }

    private void quitCompletely() {
        PORT_LOCK_FILE.delete();
        if (trayIcon != null && SystemTray.isSupported()) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
            } catch (Throwable ignored) {
            }
        }
        frame.dispose();
        System.exit(0);
    }

    private JPanel makeStatCard(String label, FaIcon.Glyph glyph, JLabel valueLbl) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(10, 12, 10, 12)));
        JLabel top = new JLabel(label, FaIcon.of(glyph, 13, accent), SwingConstants.LEFT);
        top.setForeground(TEXT_MUTED);
        top.setFont(new Font("SansSerif", Font.PLAIN, 11));
        valueLbl.setForeground(TEXT_PRIMARY);
        valueLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        card.add(top, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.SOUTH);
        return card;
    }

    private JLabel makePill(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private JButton makeActionButton(String text, FaIcon.Glyph glyph, Color bg, Color fg) {
        JButton b = new JButton(text, FaIcon.of(glyph, 13, fg));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setIconTextGap(7);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setBorder(new EmptyBorder(6, 11, 6, 11));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton makeIconButton(FaIcon.Glyph glyph, String tooltip, Color fg, Color bg) {
        JButton b = new JButton(FaIcon.of(glyph, 14, fg));
        b.setToolTipText(tooltip);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(28, 28));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JCheckBox makeDarkCheck(String text, boolean checked) {
        JCheckBox cb = new JCheckBox(text, checked);
        cb.setOpaque(false);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFocusPainted(false);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return cb;
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(BG_ROOT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class NavButton extends JButton {
        private final FaIcon.Glyph glyph;
        private boolean active = false;
        private Color activeAccent = new Color(0x38, 0x82, 0xF6);

        NavButton(String text, FaIcon.Glyph glyph) {
            super(text, FaIcon.of(glyph, 14, TEXT_MUTED));
            this.glyph = glyph;
            setHorizontalAlignment(SwingConstants.LEFT);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setForeground(TEXT_MUTED);
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setIconTextGap(10);
            setBorder(new EmptyBorder(9, 12, 9, 10));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setActive(boolean active, Color accentColor) {
            this.active = active;
            this.activeAccent = accentColor;
            setForeground(active ? TEXT_PRIMARY : TEXT_MUTED);
            setIcon(FaIcon.of(glyph, 14, active ? accentColor : TEXT_MUTED));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(new Color(activeAccent.getRed(), activeAccent.getGreen(), activeAccent.getBlue(), 34));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(activeAccent);
                    g2.fillRoundRect(0, 5, 3, getHeight() - 10, 3, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 14));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
