package dev.taezeem14.cigarette.desktop;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
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
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * Cigarette V1 (v1.4.0) background desktop companion &amp; multi-version launcher manager
 * with pure-Java2D vector icons, custom anti-aliased Swing widgets, live Minecraft process
 * detection, Modrinth ViaForge/ViaFabricPlus auto-installer, and live profile JSON synchronization.
 */
public class CigaretteDesktopApp {

    private static final Color BG_ROOT = new Color(0x14, 0x14, 0x16);
    private static final Color BG_SIDEBAR = new Color(0x18, 0x18, 0x1C);
    private static final Color BG_CARD = new Color(0x1D, 0x1D, 0x24);
    private static final Color BG_CARD_ALT = new Color(0x24, 0x24, 0x2E);
    private static final Color BG_INPUT = new Color(0x11, 0x11, 0x15);
    private static final Color BORDER_COL = new Color(0x2C, 0x2C, 0x38);
    private static final Color SCROLL_THUMB = new Color(0x32, 0x32, 0x3E);
    private static final Color TEXT_PRIMARY = new Color(0xEE, 0xEE, 0xF4);
    private static final Color TEXT_MUTED = new Color(0x8E, 0x8E, 0x9A);
    private static final Color EMERALD = new Color(0x10, 0xB9, 0x81);
    private static final Color AMBER = new Color(0xF5, 0x9E, 0x0B);
    private static final Color CRIMSON = new Color(0xF4, 0x3F, 0x5E);
    private static final Color CYAN = new Color(0x06, 0xB6, 0xD4);
    private static final Color EMBER = new Color(0xFF, 0x57, 0x22);

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

    private JLabel titleLogoLbl;
    private GlowBadge versionBadge;
    private JLabel editionPill;
    private JLabel headerStatusPill;
    private ToastBar toastBar;

    private RoundedPanel dashBannerCard;
    private JLabel dashBannerTitle;
    private JLabel dashBannerBody;
    private ModernButton dashOpenClickGuiBtn;
    private JLabel statModulesLbl;
    private JLabel statProfileLbl;
    private JLabel statThemeLbl;
    private JLabel statInstallLbl;

    private JComboBox<File> gameDirCombo;
    private JPanel modulesGrid;
    private JTextField moduleSearchField;
    private DesktopCatalog.CategoryType moduleCatFilter = null;
    private final List<ModernButton> categoryButtons = new ArrayList<ModernButton>();
    private ModernButton allCategoryButton;

    private JPanel profilesListPanel;
    private JTextField newProfileField;

    // Launcher & Multi-Version Tab widgets
    private JComboBox<MinecraftBridge.LauncherInfo> launcherCombo;
    private JLabel launcherHintLabel;
    private JPanel installerModsPanel;
    private JLabel installerConflictLabel;
    private JLabel viaStatusLabel;
    private ModernButton viaForgeBtn;
    private ModernButton viaFabricBtn;
    private JPanel installedVersionsPanel;
    private boolean viaDownloadInFlight = false;

    // Settings Tab widgets
    private JPanel themeButtonsWrap;
    private JLabel trayStatusLabel;

    private boolean hideOnClose = true;
    private boolean autoLiveSync = true;
    private boolean alwaysOnTop = false;
    private boolean scanInFlight = false;

    private List<MinecraftBridge.RunningProcess> runningProcesses = new ArrayList<MinecraftBridge.RunningProcess>();
    private List<File> discoveredDirs = new ArrayList<File>();
    private List<MinecraftBridge.LauncherInfo> discoveredLaunchers = new ArrayList<MinecraftBridge.LauncherInfo>();
    private File selectedGameDir;
    private String activeProfileName = "default";
    private Map<String, Object> activeProfileData = new LinkedHashMap<String, Object>();

    public static void main(String[] args) {
        // Prevent Swing Direct3D pipeline glitches on older Windows GPUs
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
                ServerSocket ss;
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
        discoveredLaunchers = MinecraftBridge.discoverLaunchers();
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
        frame = new JFrame("CIGARETTE V1");
        frame.setUndecorated(true);
        frame.setSize(1020, 660);
        frame.setMinimumSize(new Dimension(880, 560));
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

        toastBar = new ToastBar();

        root.add(buildTitleBar(), BorderLayout.NORTH);
        root.add(buildSidebar(), BorderLayout.WEST);

        contentCards = new CardLayout();
        contentArea = new JPanel(contentCards);
        contentArea.setBackground(BG_ROOT);
        contentArea.setBorder(new EmptyBorder(14, 16, 12, 16));

        contentArea.add(buildDashboardTab(), "dashboard");
        contentArea.add(buildModulesTab(), "modules");
        contentArea.add(buildProfilesTab(), "profiles");
        contentArea.add(buildLauncherTab(), "installer");
        contentArea.add(buildSettingsTab(), "settings");

        root.add(contentArea, BorderLayout.CENTER);
        root.add(toastBar, BorderLayout.SOUTH);

        frame.setContentPane(root);
        selectTab("dashboard");
        rebuildModulesGrid();
        rebuildProfilesList();
        rebuildLauncherAndVersions();
        updateStatsLabels();
    }

    private JPanel buildTitleBar() {
        final JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SIDEBAR);
        bar.setPreferredSize(new Dimension(0, 44));
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

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 9, 9));
        left.setOpaque(false);
        titleLogoLbl = new JLabel(FaIcon.of(FaIcon.Glyph.LOGO, 22, accent));
        JLabel titleLbl = new JLabel("CIGARETTE V1");
        titleLbl.setForeground(TEXT_PRIMARY);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));

        versionBadge = new GlowBadge("v1.4", accent);
        editionPill = makePill(edition.toUpperCase(Locale.ROOT), new Color(0x25, 0x25, 0x30), accent);
        headerStatusPill = makePill("SCANNING...", new Color(0x22, 0x22, 0x2A), TEXT_MUTED);

        left.add(titleLogoLbl);
        left.add(titleLbl);
        left.add(versionBadge);
        left.add(editionPill);
        left.add(headerStatusPill);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        right.setOpaque(false);

        final ModernButton pinBtn = makeIconButton(FaIcon.Glyph.PIN, "Pin window Always on Top", TEXT_MUTED, BG_CARD);
        pinBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                alwaysOnTop = !alwaysOnTop;
                frame.setAlwaysOnTop(alwaysOnTop);
                pinBtn.setIcon(FaIcon.of(FaIcon.Glyph.PIN, 14, alwaysOnTop ? accent : TEXT_MUTED));
                showToast(alwaysOnTop ? "Pinned Cigarette V1 Always on Top" : "Unpinned window", accent, FaIcon.Glyph.PIN);
            }
        });

        ModernButton hideBtn = makeActionButton("Hide", FaIcon.Glyph.EYE_SLASH, BG_CARD_ALT, TEXT_PRIMARY);
        hideBtn.setToolTipText("Hide Cigarette V1 to background system tray");
        hideBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToTray(true);
            }
        });

        ModernButton minBtn = makeIconButton(FaIcon.Glyph.MINUS, "Minimize to taskbar", TEXT_MUTED, BG_CARD);
        minBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setState(JFrame.ICONIFIED);
            }
        });

        ModernButton closeBtn = makeIconButton(FaIcon.Glyph.POWER, "Quit or Hide to Tray", CRIMSON, new Color(0x2D, 0x1B, 0x20));
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
        side.setPreferredSize(new Dimension(208, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COL));

        JPanel navList = new JPanel();
        navList.setOpaque(false);
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBorder(new EmptyBorder(12, 10, 10, 10));

        addNav(navList, "dashboard", "Dashboard", FaIcon.Glyph.GAUGE);
        addNav(navList, "modules", "Modules (" + DesktopCatalog.getModules().size() + ")", FaIcon.Glyph.CUBES);
        addNav(navList, "profiles", "Profiles", FaIcon.Glyph.LAYER_GROUP);
        addNav(navList, "installer", "Launcher & 1.8+", FaIcon.Glyph.ROCKET);
        addNav(navList, "settings", "Settings & Tray", FaIcon.Glyph.GEAR);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(new EmptyBorder(10, 12, 12, 12));

        RoundedPanel hintCard = new RoundedPanel(new BorderLayout(0, 4), 10, BG_CARD, BORDER_COL);
        hintCard.setBorder(new EmptyBorder(9, 11, 9, 11));
        JLabel bindTitle = new JLabel("Cigarette ClickGUI", FaIcon.of(FaIcon.Glyph.SLIDERS, 13, accent), SwingConstants.LEFT);
        bindTitle.setForeground(TEXT_PRIMARY);
        bindTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        JLabel bindSub = new JLabel("Press Right Shift in-world", SwingConstants.LEFT);
        bindSub.setForeground(TEXT_MUTED);
        bindSub.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hintCard.add(bindTitle, BorderLayout.NORTH);
        hintCard.add(bindSub, BorderLayout.SOUTH);

        ModernButton quitSide = makeActionButton("Exit Companion", FaIcon.Glyph.POWER, new Color(0x28, 0x18, 0x1D), CRIMSON);
        quitSide.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        quitSide.setAlignmentX(Component.LEFT_ALIGNMENT);
        quitSide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitCompletely();
            }
        });

        hintCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
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

    // ─── Tab 1: Dashboard ─────────────────────────────────────────────────────

    private JComponent buildDashboardTab() {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        dashBannerCard = new RoundedPanel(new BorderLayout(12, 8), 12, BG_CARD, BORDER_COL);
        dashBannerCard.setStripe(accent, RoundedPanel.STRIPE_LEFT);
        dashBannerCard.setBorder(new EmptyBorder(14, 18, 14, 16));

        dashBannerTitle = new JLabel("Checking running Minecraft instances...", FaIcon.of(FaIcon.Glyph.TERMINAL, 16, accent), SwingConstants.LEFT);
        dashBannerTitle.setForeground(TEXT_PRIMARY);
        dashBannerTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        dashBannerBody = new JLabel("<html>Scanning Java processes &amp; launcher directories...</html>");
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

        JPanel textStack = new JPanel(new BorderLayout(0, 5));
        textStack.setOpaque(false);
        textStack.add(dashBannerTitle, BorderLayout.NORTH);
        textStack.add(dashBannerBody, BorderLayout.CENTER);
        dashBannerCard.add(textStack, BorderLayout.CENTER);
        dashBannerCard.add(dashOpenClickGuiBtn, BorderLayout.EAST);

        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        statsGrid.setOpaque(false);
        statModulesLbl = new JLabel("4 / " + DesktopCatalog.getModules().size());
        statProfileLbl = new JLabel("default");
        statThemeLbl = new JLabel(themePreset);
        statInstallLbl = new JLabel("Checking...");
        statsGrid.add(makeStatCard("Enabled Modules", FaIcon.Glyph.CUBES, statModulesLbl));
        statsGrid.add(makeStatCard("Active Profile", FaIcon.Glyph.LAYER_GROUP, statProfileLbl));
        statsGrid.add(makeStatCard("Theme Preset", FaIcon.Glyph.PALETTE, statThemeLbl));
        statsGrid.add(makeStatCard("Mod Folder", FaIcon.Glyph.DOWNLOAD, statInstallLbl));

        RoundedPanel dirRow = new RoundedPanel(new BorderLayout(10, 0), 12, BG_CARD, BORDER_COL);
        dirRow.setBorder(new EmptyBorder(10, 14, 10, 14));
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
                    rebuildLauncherAndVersions();
                    updateStatsLabels();
                    showToast("Switched game directory to " + selectedGameDir.getAbsolutePath(), accent, FaIcon.Glyph.FOLDER_OPEN);
                }
            }
        });

        ModernButton browseDirBtn = makeActionButton("Browse", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        browseDirBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseForGameDirectory();
            }
        });

        dirRow.add(dirLbl, BorderLayout.WEST);
        dirRow.add(gameDirCombo, BorderLayout.CENTER);
        dirRow.add(browseDirBtn, BorderLayout.EAST);

        RoundedPanel actionsCard = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 9, 8), 12, BG_CARD, BORDER_COL);
        actionsCard.setBorder(new EmptyBorder(4, 8, 4, 8));

        ModernButton installNow = makeActionButton("Install / Update in mods/", FaIcon.Glyph.DOWNLOAD, accent, Color.WHITE);
        installNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runInstallToSelectedDir();
            }
        });
        ModernButton launchQuickBtn = makeActionButton("Launcher & Multi-Version", FaIcon.Glyph.GAMEPAD, BG_CARD_ALT, TEXT_PRIMARY);
        launchQuickBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectTab("installer");
            }
        });
        ModernButton openModsNow = makeActionButton("Open mods/ Folder", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        openModsNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedGameDir != null) {
                    MinecraftBridge.openInExplorer(new File(selectedGameDir, "mods"));
                    showToast("Opened mods/ directory in Explorer", accent, FaIcon.Glyph.FOLDER_OPEN);
                }
            }
        });
        ModernButton hideTrayNow = makeActionButton("Hide to Tray", FaIcon.Glyph.EYE_SLASH, BG_CARD_ALT, TEXT_PRIMARY);
        hideTrayNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToTray(true);
            }
        });
        ModernButton rescanNow = makeActionButton("Rescan Processes", FaIcon.Glyph.SYNC, BG_CARD_ALT, TEXT_PRIMARY);
        rescanNow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshEnvironmentAsync();
                showToast("Scanning running Java & Minecraft processes...", accent, FaIcon.Glyph.SYNC);
            }
        });

        actionsCard.add(installNow);
        actionsCard.add(launchQuickBtn);
        actionsCard.add(openModsNow);
        actionsCard.add(hideTrayNow);
        actionsCard.add(rescanNow);

        RoundedPanel guideCard = new RoundedPanel(new BorderLayout(0, 6), 12, BG_CARD, BORDER_COL);
        guideCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel guideTitle = new JLabel("Cigarette V1 v1.4 — Live Sync & Multi-Version Overview", FaIcon.of(FaIcon.Glyph.SPARKLES, 14, EMBER), SwingConstants.LEFT);
        guideTitle.setForeground(TEXT_PRIMARY);
        guideTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel guideText = new JLabel("<html>1. Click <b>Install / Update in mods/</b> to deploy Cigarette V1 directly into your active launcher <code>mods/</code> folder.<br>"
                + "2. Open <b>Launcher &amp; 1.8+</b> to launch TLauncher / Prism / MultiMC / Official Launcher in 1 click, or install <b>ViaForge</b> (1.8.9 &rarr; 1.21.x) / <b>ViaFabricPlus</b> (26.3 &harr; 1.8+) for universal multi-version server support.<br>"
                + "3. Press <b>Right Shift</b> in-game to open the Cigarette V1 ClickGUI, or edit any of the 47 modules, sliders, ranges, and keybinds live from here.</html>");
        guideText.setForeground(TEXT_MUTED);
        guideText.setFont(new Font("SansSerif", Font.PLAIN, 11));
        guideCard.add(guideTitle, BorderLayout.NORTH);
        guideCard.add(guideText, BorderLayout.CENTER);

        dashBannerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        dirRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        guideCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        stack.add(dashBannerCard);
        stack.add(Box.createVerticalStrut(11));
        stack.add(statsGrid);
        stack.add(Box.createVerticalStrut(11));
        stack.add(dirRow);
        stack.add(Box.createVerticalStrut(11));
        stack.add(actionsCard);
        stack.add(Box.createVerticalStrut(11));
        stack.add(guideCard);

        return wrapNorthScrollable(stack);
    }

    // ─── Tab 2: Modules (47 across 6 Categories) ──────────────────────────────

    private JPanel buildModulesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.setOpaque(false);

        final JPanel catRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        catRow.setOpaque(false);
        categoryButtons.clear();

        allCategoryButton = makeActionButton("All", FaIcon.Glyph.CUBES, accent, Color.WHITE);
        allCategoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moduleCatFilter = null;
                refreshCategoryChips();
                rebuildModulesGrid();
            }
        });
        categoryButtons.add(allCategoryButton);
        catRow.add(allCategoryButton);

        for (final DesktopCatalog.CategoryType cat : DesktopCatalog.CategoryType.values()) {
            final ModernButton cb = makeActionButton(cat.getLabel(), cat.getIcon(), BG_CARD, TEXT_MUTED);
            cb.putClientProperty("category", cat);
            cb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    moduleCatFilter = cat;
                    refreshCategoryChips();
                    rebuildModulesGrid();
                }
            });
            categoryButtons.add(cb);
            catRow.add(cb);
        }

        RoundedPanel searchWrap = new RoundedPanel(new BorderLayout(8, 0), 8, BG_CARD, BORDER_COL);
        searchWrap.setPreferredSize(new Dimension(200, 32));
        searchWrap.setBorder(new EmptyBorder(4, 10, 4, 8));
        searchWrap.add(new JLabel(FaIcon.of(FaIcon.Glyph.SEARCH, 13, TEXT_MUTED)), BorderLayout.WEST);
        moduleSearchField = new JTextField();
        moduleSearchField.setOpaque(false);
        moduleSearchField.setForeground(TEXT_PRIMARY);
        moduleSearchField.setCaretColor(accent);
        moduleSearchField.setBorder(null);
        moduleSearchField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        moduleSearchField.setToolTipText("Search 47 Cigarette V1 modules...");
        moduleSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { rebuildModulesGrid(); }
            @Override public void removeUpdate(DocumentEvent e) { rebuildModulesGrid(); }
            @Override public void changedUpdate(DocumentEvent e) { rebuildModulesGrid(); }
        });
        searchWrap.add(moduleSearchField, BorderLayout.CENTER);

        top.add(catRow, BorderLayout.CENTER);
        top.add(searchWrap, BorderLayout.EAST);

        modulesGrid = new JPanel(new GridLayout(0, 2, 10, 10));
        modulesGrid.setOpaque(false);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_ROOT);
        wrap.add(modulesGrid, BorderLayout.NORTH);

        panel.add(top, BorderLayout.NORTH);
        panel.add(makeDarkScrollPane(wrap), BorderLayout.CENTER);
        return panel;
    }

    private void refreshCategoryChips() {
        for (ModernButton b : categoryButtons) {
            Object catProp = b.getClientProperty("category");
            boolean active = (moduleCatFilter == null && b == allCategoryButton)
                    || (moduleCatFilter != null && moduleCatFilter.equals(catProp));
            b.setColors(active ? accent : BG_CARD, active ? Color.WHITE : TEXT_MUTED, active ? accent : BORDER_COL);
            FaIcon.Glyph g = catProp instanceof DesktopCatalog.CategoryType
                    ? ((DesktopCatalog.CategoryType) catProp).getIcon()
                    : FaIcon.Glyph.CUBES;
            b.setIcon(FaIcon.of(g, 13, active ? Color.WHITE : TEXT_MUTED));
        }
    }

    // ─── Tab 3: Profiles ──────────────────────────────────────────────────────

    private JPanel buildProfilesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        RoundedPanel createBar = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 8, 8), 12, BG_CARD, BORDER_COL);
        createBar.setBorder(new EmptyBorder(4, 8, 4, 8));

        newProfileField = new JTextField(18);
        newProfileField.setBackground(BG_INPUT);
        newProfileField.setForeground(TEXT_PRIMARY);
        newProfileField.setCaretColor(accent);
        newProfileField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        newProfileField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(6, 9, 6, 9)));

        ModernButton createBtn = makeActionButton("Create & Save", FaIcon.Glyph.PLUS, accent, Color.WHITE);
        createBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = newProfileField.getText().trim().replaceAll("[^a-zA-Z0-9_\\-]", "");
                if (name.isEmpty()) {
                    showToast("Enter a valid profile name (letters, numbers, - or _)", AMBER, FaIcon.Glyph.WARNING);
                    return;
                }
                activeProfileName = name;
                activeProfileData.put("profile_name", name);
                boolean ok = MinecraftBridge.saveProfileAndSignal(getProfilesDir(), name, activeProfileData, "reload");
                newProfileField.setText("");
                rebuildProfilesList();
                updateStatsLabels();
                showToast(ok ? "Created & synced profile '" + name + "'" : "Failed to write profile '" + name + "'",
                        ok ? EMERALD : CRIMSON, ok ? FaIcon.Glyph.CHECK : FaIcon.Glyph.WARNING);
            }
        });

        ModernButton syncBtn = makeActionButton("Sync to Game", FaIcon.Glyph.SYNC, BG_CARD_ALT, TEXT_PRIMARY);
        syncBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean ok = MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "reload");
                showToast(ok ? "Synced profile '" + activeProfileName + "' to Minecraft" : "Could not write profile JSON",
                        ok ? EMERALD : CRIMSON, ok ? FaIcon.Glyph.SYNC : FaIcon.Glyph.WARNING);
            }
        });

        ModernButton openProfBtn = makeActionButton("Open Folder", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        openProfBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MinecraftBridge.openInExplorer(getProfilesDir());
                showToast("Opened profiles directory", accent, FaIcon.Glyph.FOLDER_OPEN);
            }
        });

        createBar.add(new JLabel(FaIcon.of(FaIcon.Glyph.LAYER_GROUP, 14, accent)));
        createBar.add(newProfileField);
        createBar.add(createBtn);
        createBar.add(syncBtn);
        createBar.add(openProfBtn);

        profilesListPanel = new JPanel();
        profilesListPanel.setOpaque(false);
        profilesListPanel.setLayout(new BoxLayout(profilesListPanel, BoxLayout.Y_AXIS));

        panel.add(createBar, BorderLayout.NORTH);
        panel.add(wrapNorthScrollable(profilesListPanel), BorderLayout.CENTER);
        return panel;
    }

    // ─── Tab 4: Launcher & Multi-Version (1.8+) ───────────────────────────────

    private JComponent buildLauncherTab() {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        // 1. Launcher Quick-Start Card
        RoundedPanel launcherCard = new RoundedPanel(new BorderLayout(10, 8), 12, BG_CARD, BORDER_COL);
        launcherCard.setStripe(accent, RoundedPanel.STRIPE_LEFT);
        launcherCard.setBorder(new EmptyBorder(12, 16, 12, 14));
        launcherCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel launchTitle = new JLabel("Launcher Quick-Start (1-Click Detached Launch)", FaIcon.of(FaIcon.Glyph.GAMEPAD, 16, accent), SwingConstants.LEFT);
        launchTitle.setForeground(TEXT_PRIMARY);
        launchTitle.setFont(new Font("SansSerif", Font.BOLD, 13));

        launcherHintLabel = new JLabel("Auto-detects TLauncher, Prism Launcher, MultiMC, Modrinth App, and Official Minecraft Launcher.");
        launcherHintLabel.setForeground(TEXT_MUTED);
        launcherHintLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel launchHdr = new JPanel(new BorderLayout(0, 3));
        launchHdr.setOpaque(false);
        launchHdr.add(launchTitle, BorderLayout.NORTH);
        launchHdr.add(launcherHintLabel, BorderLayout.SOUTH);

        JPanel launchControls = new JPanel(new BorderLayout(8, 0));
        launchControls.setOpaque(false);
        launcherCombo = new JComboBox<MinecraftBridge.LauncherInfo>();
        styleCombo(launcherCombo);
        populateLauncherCombo();

        JPanel launchBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        launchBtns.setOpaque(false);

        ModernButton locateLauncherBtn = makeActionButton("Locate .exe", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        locateLauncherBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(selectedGameDir);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    File chosen = chooser.getSelectedFile();
                    if (chosen != null && chosen.exists()) {
                        MinecraftBridge.LauncherInfo custom = new MinecraftBridge.LauncherInfo("Custom (" + chosen.getName() + ")", chosen);
                        discoveredLaunchers.add(0, custom);
                        populateLauncherCombo();
                        launcherCombo.setSelectedItem(custom);
                        showToast("Added launcher " + chosen.getName(), EMERALD, FaIcon.Glyph.CHECK);
                    }
                }
            }
        });

        ModernButton startLauncherBtn = makeActionButton("Launch Launcher / Minecraft", FaIcon.Glyph.PLAY, accent, Color.WHITE);
        startLauncherBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object sel = launcherCombo.getSelectedItem();
                if (!(sel instanceof MinecraftBridge.LauncherInfo)) {
                    showToast("No launcher detected yet — click 'Locate .exe' to choose your launcher", AMBER, FaIcon.Glyph.WARNING);
                    return;
                }
                MinecraftBridge.LauncherInfo info = (MinecraftBridge.LauncherInfo) sel;
                MinecraftBridge.InstallResult res = MinecraftBridge.launchExecutable(info.executable);
                showToast(res.message, res.ok ? EMERALD : CRIMSON, res.ok ? FaIcon.Glyph.ROCKET : FaIcon.Glyph.WARNING);
            }
        });

        launchBtns.add(locateLauncherBtn);
        launchBtns.add(startLauncherBtn);
        launchControls.add(launcherCombo, BorderLayout.CENTER);
        launchControls.add(launchBtns, BorderLayout.EAST);

        launcherCard.add(launchHdr, BorderLayout.NORTH);
        launcherCard.add(launchControls, BorderLayout.SOUTH);

        // 2. 1-Click Cigarette V1 Mod Installer Card
        RoundedPanel modCard = new RoundedPanel(new BorderLayout(10, 8), 12, BG_CARD, BORDER_COL);
        modCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        modCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel modTitle = new JLabel("1-Click Cigarette V1 Mod Installer", FaIcon.of(FaIcon.Glyph.DOWNLOAD, 15, accent), SwingConstants.LEFT);
        modTitle.setForeground(TEXT_PRIMARY);
        modTitle.setFont(new Font("SansSerif", Font.BOLD, 13));

        installerConflictLabel = new JLabel("Checking mods/ directory...");
        installerConflictLabel.setForeground(TEXT_MUTED);
        installerConflictLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JPanel modTopText = new JPanel(new BorderLayout(0, 3));
        modTopText.setOpaque(false);
        modTopText.add(modTitle, BorderLayout.NORTH);
        modTopText.add(installerConflictLabel, BorderLayout.SOUTH);

        JPanel modBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modBtns.setOpaque(false);

        ModernButton installBtn = makeActionButton("Install / Update Cigarette JAR", FaIcon.Glyph.DOWNLOAD, accent, Color.WHITE);
        installBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runInstallToSelectedDir();
            }
        });
        ModernButton browseGameBtn = makeActionButton("Browse Game Folder", FaIcon.Glyph.FOLDER_OPEN, BG_CARD_ALT, TEXT_PRIMARY);
        browseGameBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseForGameDirectory();
            }
        });
        ModernButton cleanBtn = makeActionButton("Clean Conflicting JARs", FaIcon.Glyph.TRASH, new Color(0x2D, 0x1A, 0x20), CRIMSON);
        cleanBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectedGameDir == null) return;
                int removed = MinecraftBridge.removeConflictingJars(new File(selectedGameDir, "mods"), edition);
                rebuildLauncherAndVersions();
                updateStatsLabels();
                showToast(removed > 0
                                ? "Removed " + removed + " conflicting Cigarette JAR(s) from mods/"
                                : "No conflicting edition JARs found in mods/",
                        removed > 0 ? EMERALD : accent, FaIcon.Glyph.CHECK);
            }
        });
        modBtns.add(installBtn);
        modBtns.add(browseGameBtn);
        modBtns.add(cleanBtn);

        installerModsPanel = new JPanel();
        installerModsPanel.setOpaque(false);
        installerModsPanel.setLayout(new BoxLayout(installerModsPanel, BoxLayout.Y_AXIS));

        JPanel modCenter = new JPanel(new BorderLayout(0, 8));
        modCenter.setOpaque(false);
        modCenter.add(modBtns, BorderLayout.NORTH);
        modCenter.add(installerModsPanel, BorderLayout.CENTER);

        modCard.add(modTopText, BorderLayout.NORTH);
        modCard.add(modCenter, BorderLayout.CENTER);

        // 3. Multi-Version (1.8 -> 1.21.x -> 26.x) Protocol Bridge Auto-Installer
        RoundedPanel viaCard = new RoundedPanel(new BorderLayout(10, 8), 12, BG_CARD, BORDER_COL);
        viaCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        viaCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel viaTitle = new JLabel("Multi-Version Protocol Bridge (1.8 \u2192 1.21.x \u2192 26.x)", FaIcon.of(FaIcon.Glyph.CLOUD_DOWNLOAD, 16, CYAN), SwingConstants.LEFT);
        viaTitle.setForeground(TEXT_PRIMARY);
        viaTitle.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel viaDesc = new JLabel("<html>Connect to <b>any server version</b> from a single Cigarette V1 installation: use <b>ViaForge</b> on 1.8.9 Forge to join 1.8&ndash;1.21.x servers, or <b>ViaFabricPlus</b> on Modern Fabric to join 1.8.9 PvP &amp; modern servers seamlessly.</html>");
        viaDesc.setForeground(TEXT_MUTED);
        viaDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));

        viaStatusLabel = new JLabel("Checking multi-version bridge status...");
        viaStatusLabel.setForeground(AMBER);
        viaStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 11));

        JPanel viaTextStack = new JPanel();
        viaTextStack.setOpaque(false);
        viaTextStack.setLayout(new BoxLayout(viaTextStack, BoxLayout.Y_AXIS));
        viaTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        viaDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        viaStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        viaTextStack.add(viaTitle);
        viaTextStack.add(Box.createVerticalStrut(4));
        viaTextStack.add(viaDesc);
        viaTextStack.add(Box.createVerticalStrut(5));
        viaTextStack.add(viaStatusLabel);

        JPanel viaBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        viaBtns.setOpaque(false);

        boolean modernDefault = DesktopCatalog.isModernEdition(edition);
        viaForgeBtn = makeActionButton("Auto-Install ViaForge (1.8.9 Forge)", FaIcon.Glyph.CLOUD_DOWNLOAD,
                modernDefault ? BG_CARD_ALT : accent, modernDefault ? TEXT_PRIMARY : Color.WHITE);
        viaForgeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                triggerViaInstallAsync(false);
            }
        });

        viaFabricBtn = makeActionButton("Auto-Install ViaFabricPlus (Modern Fabric)", FaIcon.Glyph.CLOUD_DOWNLOAD,
                modernDefault ? accent : BG_CARD_ALT, modernDefault ? Color.WHITE : TEXT_PRIMARY);
        viaFabricBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                triggerViaInstallAsync(true);
            }
        });

        ModernButton modrinthWebBtn = makeActionButton("Open Modrinth", FaIcon.Glyph.EXTERNAL_LINK, BG_CARD_ALT, TEXT_MUTED);
        modrinthWebBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String url = DesktopCatalog.isModernEdition(edition)
                        ? "https://modrinth.com/mod/viafabricplus"
                        : "https://modrinth.com/mod/viaforge";
                MinecraftBridge.openWebLink(url);
                showToast("Opened Modrinth page in browser", accent, FaIcon.Glyph.EXTERNAL_LINK);
            }
        });

        viaBtns.add(viaForgeBtn);
        viaBtns.add(viaFabricBtn);
        viaBtns.add(modrinthWebBtn);

        viaCard.add(viaTextStack, BorderLayout.NORTH);
        viaCard.add(viaBtns, BorderLayout.SOUTH);

        // 4. Installed Versions Scanner (<gameDir>/versions/)
        RoundedPanel versionsCard = new RoundedPanel(new BorderLayout(0, 8), 12, BG_CARD, BORDER_COL);
        versionsCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        versionsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel verTitle = new JLabel("Installed Game Versions Scanner (versions/)", FaIcon.of(FaIcon.Glyph.GLOBE, 15, accent), SwingConstants.LEFT);
        verTitle.setForeground(TEXT_PRIMARY);
        verTitle.setFont(new Font("SansSerif", Font.BOLD, 13));

        installedVersionsPanel = new JPanel();
        installedVersionsPanel.setOpaque(false);
        installedVersionsPanel.setLayout(new BoxLayout(installedVersionsPanel, BoxLayout.Y_AXIS));

        versionsCard.add(verTitle, BorderLayout.NORTH);
        versionsCard.add(installedVersionsPanel, BorderLayout.CENTER);

        stack.add(launcherCard);
        stack.add(Box.createVerticalStrut(11));
        stack.add(modCard);
        stack.add(Box.createVerticalStrut(11));
        stack.add(viaCard);
        stack.add(Box.createVerticalStrut(11));
        stack.add(versionsCard);

        return wrapNorthScrollable(stack);
    }

    private void triggerViaInstallAsync(final boolean modernBridge) {
        if (viaDownloadInFlight || selectedGameDir == null) return;
        viaDownloadInFlight = true;
        final String label = modernBridge ? "ViaFabricPlus" : "ViaForge";
        final File modsDir = new File(selectedGameDir, "mods");

        if (viaForgeBtn != null) viaForgeBtn.setEnabled(false);
        if (viaFabricBtn != null) viaFabricBtn.setEnabled(false);
        if (viaStatusLabel != null) {
            viaStatusLabel.setText("Querying Modrinth API & downloading " + label + " into mods/...");
            viaStatusLabel.setForeground(accent);
        }
        showToast("Downloading " + label + " from Modrinth...", accent, FaIcon.Glyph.CLOUD_DOWNLOAD);

        new SwingWorker<MinecraftBridge.InstallResult, Void>() {
            @Override
            protected MinecraftBridge.InstallResult doInBackground() {
                return MinecraftBridge.installViaAddon(modsDir, modernBridge);
            }

            @Override
            protected void done() {
                viaDownloadInFlight = false;
                if (viaForgeBtn != null) viaForgeBtn.setEnabled(true);
                if (viaFabricBtn != null) viaFabricBtn.setEnabled(true);
                try {
                    MinecraftBridge.InstallResult res = get();
                    rebuildLauncherAndVersions();
                    if (viaStatusLabel != null && !res.ok) {
                        viaStatusLabel.setText(res.message);
                        viaStatusLabel.setForeground(AMBER);
                    }
                    showToast(res.message, res.ok ? EMERALD : AMBER, res.ok ? FaIcon.Glyph.CHECK : FaIcon.Glyph.WARNING);
                } catch (Exception ex) {
                    showToast("Via installer error: " + ex.getMessage(), CRIMSON, FaIcon.Glyph.WARNING);
                }
            }
        }.execute();
    }

    // ─── Tab 5: Settings & Tray ───────────────────────────────────────────────

    private JComponent buildSettingsTab() {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        RoundedPanel themeCard = new RoundedPanel(new BorderLayout(0, 8), 12, BG_CARD, BORDER_COL);
        themeCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        themeCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel themeLbl = new JLabel("Cigarette V1 Accent Theme (7 Built-In Presets)", FaIcon.of(FaIcon.Glyph.PALETTE, 14, accent), SwingConstants.LEFT);
        themeLbl.setForeground(TEXT_PRIMARY);
        themeLbl.setFont(new Font("SansSerif", Font.BOLD, 13));

        themeButtonsWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        themeButtonsWrap.setOpaque(false);
        rebuildThemePresetButtons();

        themeCard.add(themeLbl, BorderLayout.NORTH);
        themeCard.add(themeButtonsWrap, BorderLayout.CENTER);

        RoundedPanel trayCard = new RoundedPanel(new BorderLayout(0, 10), 12, BG_CARD, BORDER_COL);
        trayCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        trayCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel trayHdr = new JLabel("Background Companion & Live Bridge Behavior", FaIcon.of(FaIcon.Glyph.GEAR, 14, accent), SwingConstants.LEFT);
        trayHdr.setForeground(TEXT_PRIMARY);
        trayHdr.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel togglesCol = new JPanel();
        togglesCol.setOpaque(false);
        togglesCol.setLayout(new BoxLayout(togglesCol, BoxLayout.Y_AXIS));

        JPanel row1 = makeToggleSettingRow(
                "Close button [Power] hides Cigarette V1 to System Tray",
                "Keeps background live-sync active without cluttering your taskbar",
                hideOnClose,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ToggleSwitch sw = (ToggleSwitch) e.getSource();
                        hideOnClose = sw.isSelected();
                        showToast(hideOnClose ? "Close button will hide to System Tray" : "Close button will exit companion", accent, FaIcon.Glyph.INFO);
                    }
                });
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel row2 = makeToggleSettingRow(
                "Live-sync module, slider, keybind & theme changes into running Minecraft",
                "Writes .bridge_sync signal automatically whenever you toggle or save settings",
                autoLiveSync,
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ToggleSwitch sw = (ToggleSwitch) e.getSource();
                        autoLiveSync = sw.isSelected();
                        showToast(autoLiveSync ? "Automatic live-sync enabled" : "Automatic live-sync paused", accent, FaIcon.Glyph.SYNC);
                    }
                });
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        trayStatusLabel = new JLabel(SystemTray.isSupported()
                ? "System Tray: Active (click the Cigarette tray icon anytime to show or hide)"
                : "System Tray: Unavailable on this desktop session (use Minimize)");
        trayStatusLabel.setForeground(TEXT_MUTED);
        trayStatusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        trayStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        togglesCol.add(row1);
        togglesCol.add(Box.createVerticalStrut(8));
        togglesCol.add(row2);
        togglesCol.add(Box.createVerticalStrut(10));
        togglesCol.add(trayStatusLabel);

        JPanel trayBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        trayBtns.setOpaque(false);
        ModernButton hideBtn = makeActionButton("Hide to Background Now", FaIcon.Glyph.EYE_SLASH, BG_CARD_ALT, TEXT_PRIMARY);
        hideBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideToTray(true);
            }
        });
        ModernButton exitBtn = makeActionButton("Quit Companion Completely", FaIcon.Glyph.POWER, new Color(0x2E, 0x1A, 0x1F), CRIMSON);
        exitBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitCompletely();
            }
        });
        trayBtns.add(hideBtn);
        trayBtns.add(exitBtn);

        trayCard.add(trayHdr, BorderLayout.NORTH);
        trayCard.add(togglesCol, BorderLayout.CENTER);
        trayCard.add(trayBtns, BorderLayout.SOUTH);

        stack.add(themeCard);
        stack.add(Box.createVerticalStrut(12));
        stack.add(trayCard);
        return wrapNorthScrollable(stack);
    }

    private void rebuildThemePresetButtons() {
        if (themeButtonsWrap == null) return;
        themeButtonsWrap.removeAll();
        for (final String preset : DesktopCatalog.THEME_PRESETS) {
            Color c = DesktopCatalog.getThemeColor(preset);
            boolean active = preset.equalsIgnoreCase(themePreset);
            ModernButton tb = makeActionButton(preset, active ? FaIcon.Glyph.CHECK : FaIcon.Glyph.PALETTE,
                    active ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 46) : BG_CARD_ALT,
                    active ? Color.WHITE : c);
            tb.setBorderColor(active ? c : BORDER_COL);
            tb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    applyThemePreset(preset, true);
                    showToast("Applied '" + preset + "' theme & synced to profile", accent, FaIcon.Glyph.PALETTE);
                }
            });
            themeButtonsWrap.add(tb);
        }
        themeButtonsWrap.revalidate();
        themeButtonsWrap.repaint();
    }

    // ─── Modules Grid & Full-Featured Module Settings Dialog ──────────────────

    private void rebuildModulesGrid() {
        if (modulesGrid == null) return;
        modulesGrid.removeAll();
        String query = moduleSearchField != null ? moduleSearchField.getText().trim().toLowerCase(Locale.ROOT) : "";
        Map<String, Object> modulesMap = getModulesSection(activeProfileData);
        final boolean modern = DesktopCatalog.isModernEdition(edition);

        for (final DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            if (moduleCatFilter != null && def.category != moduleCatFilter) continue;
            if (!query.isEmpty() && !def.name.toLowerCase(Locale.ROOT).contains(query)
                    && !def.description.toLowerCase(Locale.ROOT).contains(query)
                    && !def.category.getLabel().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            final Map<String, Object> md = getOrCreateModuleMap(modulesMap, def);
            final boolean enabled = Boolean.TRUE.equals(md.get("enabled"));
            final int keyCode = getModuleKeyCode(md, def, modern);

            RoundedPanel card = new RoundedPanel(new BorderLayout(8, 6), 10,
                    enabled ? BG_CARD_ALT : BG_CARD,
                    enabled ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 145) : BORDER_COL);
            if (enabled) {
                card.setStripe(accent, RoundedPanel.STRIPE_LEFT);
            }
            card.setBorder(new EmptyBorder(10, 13, 10, 12));

            JPanel titleRow = new JPanel(new BorderLayout(8, 0));
            titleRow.setOpaque(false);
            JLabel nameLbl = new JLabel(def.name, FaIcon.of(def.category.getIcon(), 13, enabled ? accent : TEXT_MUTED), SwingConstants.LEFT);
            nameLbl.setForeground(TEXT_PRIMARY);
            nameLbl.setFont(new Font("SansSerif", Font.BOLD, 13));

            final ToggleSwitch toggleSwitch = new ToggleSwitch(enabled);
            toggleSwitch.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean next = toggleSwitch.isSelected();
                    getOrCreateModuleMap(getModulesSection(activeProfileData), def).put("enabled", Boolean.valueOf(next));
                    saveActiveIfLive();
                    rebuildModulesGrid();
                    updateStatsLabels();
                    showToast((next ? "Enabled " : "Disabled ") + def.name, next ? EMERALD : TEXT_MUTED,
                            next ? FaIcon.Glyph.CHECK : FaIcon.Glyph.MINUS);
                }
            });

            titleRow.add(nameLbl, BorderLayout.CENTER);
            titleRow.add(toggleSwitch, BorderLayout.EAST);

            JLabel descLbl = new JLabel("<html><body style='width:240px'>" + escapeHtml(def.description) + "</body></html>");
            descLbl.setForeground(TEXT_MUTED);
            descLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));

            JPanel bottomRow = new JPanel(new BorderLayout(6, 0));
            bottomRow.setOpaque(false);
            JLabel catTag = new JLabel(def.category.getLabel().toUpperCase(Locale.ROOT));
            catTag.setForeground(enabled ? accent : TEXT_MUTED);
            catTag.setFont(new Font("SansSerif", Font.BOLD, 10));

            JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            rightControls.setOpaque(false);

            final KeybindButton keyBtn = new KeybindButton(keyCode, modern, true);
            keyBtn.setChangeListener(new Runnable() {
                @Override
                public void run() {
                    int bound = keyBtn.getKeyCode();
                    getOrCreateModuleMap(getModulesSection(activeProfileData), def).put("keyCode", Integer.valueOf(bound));
                    saveActiveIfLive();
                    showToast(def.name + " keybind set to " + DesktopCatalog.formatKeyCode(bound, modern), accent, FaIcon.Glyph.KEYBOARD);
                }
            });

            int paramCount = def.getSettings(modern).size();
            String settingsLabel = paramCount > 0 ? "Settings (" + paramCount + ")" : "Settings";
            ModernButton editBtn = makeActionButton(settingsLabel, FaIcon.Glyph.SLIDERS, BG_ROOT, TEXT_PRIMARY);
            editBtn.setBorder(new EmptyBorder(4, 9, 4, 9));
            editBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openModuleSettingsDialog(def);
                }
            });

            rightControls.add(keyBtn);
            rightControls.add(editBtn);

            bottomRow.add(catTag, BorderLayout.WEST);
            bottomRow.add(rightControls, BorderLayout.EAST);

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
        final boolean modern = DesktopCatalog.isModernEdition(edition);
        final Map<String, Object> md = getOrCreateModuleMap(getModulesSection(activeProfileData), def);

        // Merge catalog defaults with any saved overrides so ALL real settings always appear
        final Map<String, Object> mergedSettings = def.createDefaultSettingsMap(modern);
        Object rawSaved = md.get("settings");
        if (rawSaved instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) rawSaved).entrySet()) {
                if (entry.getValue() != null) {
                    mergedSettings.put(entry.getKey(), entry.getValue());
                }
            }
        }
        md.put("settings", mergedSettings);

        final JDialog dlg = new JDialog(frame, def.name + " Settings", true);
        dlg.setUndecorated(true);
        dlg.setSize(490, 520);
        dlg.setLocationRelativeTo(frame);

        RoundedPanel outer = new RoundedPanel(new BorderLayout(0, 0), 12, BG_ROOT, accent);
        outer.setBorder(new EmptyBorder(1, 1, 1, 1));

        // Draggable dialog header
        JPanel dlgHeader = new JPanel(new BorderLayout(8, 0));
        dlgHeader.setBackground(BG_SIDEBAR);
        dlgHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(10, 14, 10, 12)));

        final Point[] dragPt = new Point[1];
        dlgHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragPt[0] = e.getPoint();
            }
        });
        dlgHeader.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragPt[0] != null) {
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    dlg.setLocation(p.x - dragPt[0].x, p.y - dragPt[0].y);
                }
            }
        });

        JPanel hdrLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hdrLeft.setOpaque(false);
        JLabel hdrTitle = new JLabel(def.name, FaIcon.of(def.category.getIcon(), 16, accent), SwingConstants.LEFT);
        hdrTitle.setForeground(TEXT_PRIMARY);
        hdrTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel hdrCat = makePill(def.category.getLabel().toUpperCase(Locale.ROOT), BG_CARD_ALT, accent);
        hdrLeft.add(hdrTitle);
        hdrLeft.add(hdrCat);

        JPanel hdrRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        hdrRight.setOpaque(false);
        ModernButton resetBtn = makeActionButton("Reset", FaIcon.Glyph.RESET, BG_CARD, TEXT_MUTED);
        resetBtn.setToolTipText("Reset " + def.name + " settings to Cigarette V1 defaults");
        ModernButton closeDlgBtn = makeIconButton(FaIcon.Glyph.CLOSE, "Close", TEXT_MUTED, BG_CARD);
        closeDlgBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });
        hdrRight.add(resetBtn);
        hdrRight.add(closeDlgBtn);

        dlgHeader.add(hdrLeft, BorderLayout.WEST);
        dlgHeader.add(hdrRight, BorderLayout.EAST);

        // Scrollable dialog body
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel subDesc = new JLabel("<html><body style='width:420px'>" + escapeHtml(def.description) + "</body></html>");
        subDesc.setForeground(TEXT_MUTED);
        subDesc.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(subDesc);
        body.add(Box.createVerticalStrut(10));

        // Core Module State Card (Enabled + Keybind + Hidden)
        RoundedPanel coreCard = new RoundedPanel(new GridLayout(3, 1, 0, 8), 10, BG_CARD, BORDER_COL);
        coreCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        coreCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        final ToggleSwitch enabledToggle = new ToggleSwitch(Boolean.TRUE.equals(md.get("enabled")));
        JPanel enRow = new JPanel(new BorderLayout());
        enRow.setOpaque(false);
        JLabel enLbl = new JLabel("Module Enabled");
        enLbl.setForeground(TEXT_PRIMARY);
        enLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        enRow.add(enLbl, BorderLayout.WEST);
        enRow.add(enabledToggle, BorderLayout.EAST);

        final KeybindButton dlgKeyBtn = new KeybindButton(getModuleKeyCode(md, def, modern), modern, false);
        JPanel kbRow = new JPanel(new BorderLayout());
        kbRow.setOpaque(false);
        JLabel kbLbl = new JLabel("Toggle Keybind");
        kbLbl.setForeground(TEXT_PRIMARY);
        kbLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        kbRow.add(kbLbl, BorderLayout.WEST);
        kbRow.add(dlgKeyBtn, BorderLayout.EAST);

        final ToggleSwitch hiddenToggle = new ToggleSwitch(Boolean.TRUE.equals(md.get("hidden")));
        JPanel hidRow = new JPanel(new BorderLayout());
        hidRow.setOpaque(false);
        JLabel hidLbl = new JLabel("Hide from HUD ArrayList");
        hidLbl.setForeground(TEXT_PRIMARY);
        hidLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hidRow.add(hidLbl, BorderLayout.WEST);
        hidRow.add(hiddenToggle, BorderLayout.EAST);

        coreCard.add(enRow);
        coreCard.add(kbRow);
        coreCard.add(hidRow);
        body.add(coreCard);
        body.add(Box.createVerticalStrut(12));

        final List<Runnable> appliers = new ArrayList<Runnable>();
        List<DesktopCatalog.SettingSpec> specs = def.getSettings(modern);

        if (specs.isEmpty()) {
            RoundedPanel infoCard = new RoundedPanel(new BorderLayout(), 10, BG_CARD, BORDER_COL);
            infoCard.setBorder(new EmptyBorder(10, 12, 10, 12));
            infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel noExtra = new JLabel("This module operates automatically when enabled and has no extra numeric/mode parameters.",
                    FaIcon.of(FaIcon.Glyph.INFO, 13, accent), SwingConstants.LEFT);
            noExtra.setForeground(TEXT_MUTED);
            noExtra.setFont(new Font("SansSerif", Font.PLAIN, 11));
            infoCard.add(noExtra, BorderLayout.CENTER);
            body.add(infoCard);
        } else {
            JLabel secHdr = new JLabel("MODULE PARAMETERS (" + specs.size() + ")");
            secHdr.setForeground(accent);
            secHdr.setFont(new Font("SansSerif", Font.BOLD, 10));
            secHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(secHdr);
            body.add(Box.createVerticalStrut(6));

            for (final DesktopCatalog.SettingSpec spec : specs) {
                Object curVal = mergedSettings.get(spec.name);
                if (spec.type == DesktopCatalog.SettingType.BOOLEAN) {
                    boolean initBool = curVal instanceof Boolean ? ((Boolean) curVal).booleanValue() : spec.defaultBool;
                    final ToggleSwitch sw = new ToggleSwitch(initBool);
                    RoundedPanel row = new RoundedPanel(new BorderLayout(8, 0), 9, BG_CARD, BORDER_COL);
                    row.setBorder(new EmptyBorder(8, 12, 8, 12));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    JLabel lbl = new JLabel(spec.name);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    row.add(lbl, BorderLayout.WEST);
                    row.add(sw, BorderLayout.EAST);
                    body.add(row);
                    body.add(Box.createVerticalStrut(6));

                    appliers.add(new Runnable() {
                        @Override
                        public void run() {
                            mergedSettings.put(spec.name, Boolean.valueOf(sw.isSelected()));
                        }
                    });
                } else if (spec.type == DesktopCatalog.SettingType.NUMBER) {
                    double initNum = curVal instanceof Number ? ((Number) curVal).doubleValue() : spec.defaultMin;
                    RoundedPanel row = new RoundedPanel(new BorderLayout(0, 6), 9, BG_CARD, BORDER_COL);
                    row.setBorder(new EmptyBorder(8, 12, 10, 12));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel topLine = new JPanel(new BorderLayout());
                    topLine.setOpaque(false);
                    JLabel lbl = new JLabel(spec.name);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    final JLabel valPill = makePill(formatStepValue(initNum, spec.step), BG_INPUT, accent);
                    topLine.add(lbl, BorderLayout.WEST);
                    topLine.add(valPill, BorderLayout.EAST);

                    final DarkSliderBar slider = new DarkSliderBar(spec.min, spec.max, spec.step, initNum);
                    slider.setChangeListener(new Runnable() {
                        @Override
                        public void run() {
                            valPill.setText(formatStepValue(slider.getValue(), spec.step));
                        }
                    });

                    row.add(topLine, BorderLayout.NORTH);
                    row.add(slider, BorderLayout.SOUTH);
                    body.add(row);
                    body.add(Box.createVerticalStrut(6));

                    appliers.add(new Runnable() {
                        @Override
                        public void run() {
                            mergedSettings.put(spec.name, Double.valueOf(roundToStep(slider.getValue(), spec.step)));
                        }
                    });
                } else if (spec.type == DesktopCatalog.SettingType.RANGE) {
                    double initMin = spec.defaultMin;
                    double initMax = spec.defaultMax;
                    if (curVal instanceof List) {
                        List<?> pair = (List<?>) curVal;
                        if (pair.size() >= 2 && pair.get(0) instanceof Number && pair.get(1) instanceof Number) {
                            initMin = ((Number) pair.get(0)).doubleValue();
                            initMax = ((Number) pair.get(1)).doubleValue();
                        }
                    } else {
                        Object minAlt = mergedSettings.get(spec.name + " Min");
                        Object maxAlt = mergedSettings.get(spec.name + " Max");
                        if (minAlt instanceof Number) initMin = ((Number) minAlt).doubleValue();
                        if (maxAlt instanceof Number) initMax = ((Number) maxAlt).doubleValue();
                    }
                    if (initMin > initMax) {
                        double t = initMin;
                        initMin = initMax;
                        initMax = t;
                    }

                    RoundedPanel row = new RoundedPanel(new BorderLayout(0, 6), 9, BG_CARD, BORDER_COL);
                    row.setBorder(new EmptyBorder(8, 12, 10, 12));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JPanel topLine = new JPanel(new BorderLayout());
                    topLine.setOpaque(false);
                    JLabel lbl = new JLabel(spec.name);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    final JLabel rangePill = makePill(
                            formatStepValue(initMin, spec.step) + " - " + formatStepValue(initMax, spec.step),
                            BG_INPUT, accent);
                    topLine.add(lbl, BorderLayout.WEST);
                    topLine.add(rangePill, BorderLayout.EAST);

                    final DarkSliderBar minSlider = new DarkSliderBar(spec.min, spec.max, spec.step, initMin);
                    final DarkSliderBar maxSlider = new DarkSliderBar(spec.min, spec.max, spec.step, initMax);

                    minSlider.setChangeListener(new Runnable() {
                        @Override
                        public void run() {
                            if (minSlider.getValue() > maxSlider.getValue()) {
                                maxSlider.setValueQuiet(minSlider.getValue());
                            }
                            rangePill.setText(formatStepValue(minSlider.getValue(), spec.step)
                                    + " - " + formatStepValue(maxSlider.getValue(), spec.step));
                        }
                    });
                    maxSlider.setChangeListener(new Runnable() {
                        @Override
                        public void run() {
                            if (maxSlider.getValue() < minSlider.getValue()) {
                                minSlider.setValueQuiet(maxSlider.getValue());
                            }
                            rangePill.setText(formatStepValue(minSlider.getValue(), spec.step)
                                    + " - " + formatStepValue(maxSlider.getValue(), spec.step));
                        }
                    });

                    JPanel slidersBox = new JPanel(new GridLayout(2, 1, 0, 4));
                    slidersBox.setOpaque(false);

                    JPanel minWrap = new JPanel(new BorderLayout(8, 0));
                    minWrap.setOpaque(false);
                    JLabel minTag = new JLabel("Min");
                    minTag.setForeground(TEXT_MUTED);
                    minTag.setFont(new Font("SansSerif", Font.BOLD, 10));
                    minTag.setPreferredSize(new Dimension(28, 18));
                    minWrap.add(minTag, BorderLayout.WEST);
                    minWrap.add(minSlider, BorderLayout.CENTER);

                    JPanel maxWrap = new JPanel(new BorderLayout(8, 0));
                    maxWrap.setOpaque(false);
                    JLabel maxTag = new JLabel("Max");
                    maxTag.setForeground(TEXT_MUTED);
                    maxTag.setFont(new Font("SansSerif", Font.BOLD, 10));
                    maxTag.setPreferredSize(new Dimension(28, 18));
                    maxWrap.add(maxTag, BorderLayout.WEST);
                    maxWrap.add(maxSlider, BorderLayout.CENTER);

                    slidersBox.add(minWrap);
                    slidersBox.add(maxWrap);

                    row.add(topLine, BorderLayout.NORTH);
                    row.add(slidersBox, BorderLayout.CENTER);
                    body.add(row);
                    body.add(Box.createVerticalStrut(6));

                    appliers.add(new Runnable() {
                        @Override
                        public void run() {
                            List<Object> pair = new ArrayList<Object>(2);
                            pair.add(Double.valueOf(roundToStep(minSlider.getValue(), spec.step)));
                            pair.add(Double.valueOf(roundToStep(maxSlider.getValue(), spec.step)));
                            mergedSettings.put(spec.name, pair);
                        }
                    });
                } else if (spec.type == DesktopCatalog.SettingType.MODE) {
                    final String[] modes = spec.modes != null && spec.modes.length > 0
                            ? spec.modes
                            : new String[]{spec.defaultMode != null ? spec.defaultMode : "Default"};
                    int startIdx = 0;
                    String curStr = curVal instanceof String ? (String) curVal : spec.defaultMode;
                    for (int i = 0; i < modes.length; i++) {
                        if (modes[i].equalsIgnoreCase(curStr)) {
                            startIdx = i;
                            break;
                        }
                    }
                    final int[] idxHolder = new int[]{startIdx};

                    RoundedPanel row = new RoundedPanel(new BorderLayout(8, 0), 9, BG_CARD, BORDER_COL);
                    row.setBorder(new EmptyBorder(7, 12, 7, 12));
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);

                    JLabel lbl = new JLabel(spec.name);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));

                    final ModernButton modePill = new ModernButton("<  " + modes[idxHolder[0]] + "  >", null, BG_INPUT, accent);
                    modePill.setBorderColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
                    modePill.setBorder(new EmptyBorder(5, 12, 5, 12));
                    modePill.setToolTipText("Left-click for next mode, right-click for previous mode");
                    modePill.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (SwingUtilities.isRightMouseButton(e)) {
                                idxHolder[0] = (idxHolder[0] - 1 + modes.length) % modes.length;
                                modePill.setText("<  " + modes[idxHolder[0]] + "  >");
                            }
                        }
                    });
                    modePill.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            idxHolder[0] = (idxHolder[0] + 1) % modes.length;
                            modePill.setText("<  " + modes[idxHolder[0]] + "  >");
                        }
                    });

                    row.add(lbl, BorderLayout.WEST);
                    row.add(modePill, BorderLayout.EAST);
                    body.add(row);
                    body.add(Box.createVerticalStrut(6));

                    appliers.add(new Runnable() {
                        @Override
                        public void run() {
                            mergedSettings.put(spec.name, modes[idxHolder[0]]);
                        }
                    });
                }
            }
        }

        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                md.put("enabled", Boolean.valueOf(def.defaultEnabled));
                md.put("keyCode", Integer.valueOf(def.getDefaultKeyCode(modern)));
                md.put("hidden", Boolean.FALSE);
                md.put("settings", def.createDefaultSettingsMap(modern));
                saveActiveIfLive();
                dlg.dispose();
                rebuildModulesGrid();
                updateStatsLabels();
                showToast("Reset " + def.name + " to defaults", accent, FaIcon.Glyph.RESET);
                openModuleSettingsDialog(def);
            }
        });

        JPanel dlgFooter = new JPanel(new BorderLayout(8, 0));
        dlgFooter.setBackground(BG_SIDEBAR);
        dlgFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL),
                new EmptyBorder(10, 14, 10, 14)));

        ModernButton cancelBtn = makeActionButton("Cancel", FaIcon.Glyph.CLOSE, BG_CARD, TEXT_MUTED);
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dlg.dispose();
            }
        });

        ModernButton saveBtn = makeActionButton("Save & Sync Live", FaIcon.Glyph.SAVE, accent, Color.WHITE);
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                md.put("enabled", Boolean.valueOf(enabledToggle.isSelected()));
                md.put("keyCode", Integer.valueOf(dlgKeyBtn.getKeyCode()));
                md.put("hidden", Boolean.valueOf(hiddenToggle.isSelected()));
                for (Runnable r : appliers) {
                    r.run();
                }
                if ("ClickGUI".equalsIgnoreCase(def.name) && mergedSettings.get("Theme") instanceof String) {
                    applyThemePreset((String) mergedSettings.get("Theme"), false);
                }
                saveActiveIfLive();
                dlg.dispose();
                rebuildModulesGrid();
                updateStatsLabels();
                showToast("Saved & synced " + def.name + " to profile '" + activeProfileName + "'", EMERALD, FaIcon.Glyph.CHECK);
            }
        });

        dlgFooter.add(cancelBtn, BorderLayout.WEST);
        dlgFooter.add(saveBtn, BorderLayout.CENTER);

        outer.add(dlgHeader, BorderLayout.NORTH);
        outer.add(wrapNorthScrollable(body), BorderLayout.CENTER);
        outer.add(dlgFooter, BorderLayout.SOUTH);

        dlg.setContentPane(outer);
        dlg.setVisible(true);
    }

    // ─── Profiles & Launcher/Versions Rebuilders ──────────────────────────────

    private void rebuildProfilesList() {
        if (profilesListPanel == null) return;
        profilesListPanel.removeAll();
        File dir = getProfilesDir();
        for (final String name : MinecraftBridge.listProfileNames(dir)) {
            boolean active = name.equalsIgnoreCase(activeProfileName);
            RoundedPanel row = new RoundedPanel(new BorderLayout(10, 0), 10,
                    active ? BG_CARD_ALT : BG_CARD, active ? accent : BORDER_COL);
            if (active) {
                row.setStripe(accent, RoundedPanel.STRIPE_LEFT);
            }
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(new EmptyBorder(9, 14, 9, 12));

            JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            leftInfo.setOpaque(false);
            JLabel lbl = new JLabel(name, FaIcon.of(FaIcon.Glyph.LAYER_GROUP, 14, active ? accent : TEXT_MUTED), SwingConstants.LEFT);
            lbl.setForeground(TEXT_PRIMARY);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            leftInfo.add(lbl);
            if (active) {
                leftInfo.add(makePill("ACTIVE", new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 42), accent));
            }

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.setOpaque(false);
            ModernButton loadBtn = makeActionButton("Load & Sync", FaIcon.Glyph.SYNC,
                    active ? accent : BG_ROOT, active ? Color.WHITE : TEXT_PRIMARY);
            loadBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activeProfileName = name;
                    reloadActiveProfileFromDisk();
                    MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "reload");
                    rebuildModulesGrid();
                    rebuildProfilesList();
                    updateStatsLabels();
                    showToast("Loaded & synced profile '" + name + "' to Minecraft", EMERALD, FaIcon.Glyph.SYNC);
                }
            });
            actions.add(loadBtn);

            if (!"default".equalsIgnoreCase(name)) {
                ModernButton delBtn = makeIconButton(FaIcon.Glyph.TRASH, "Delete profile '" + name + "'", CRIMSON, new Color(0x2B, 0x1A, 0x20));
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
                        showToast("Deleted profile '" + name + "'", AMBER, FaIcon.Glyph.TRASH);
                    }
                });
                actions.add(delBtn);
            }

            row.add(leftInfo, BorderLayout.CENTER);
            row.add(actions, BorderLayout.EAST);
            profilesListPanel.add(row);
            profilesListPanel.add(Box.createVerticalStrut(7));
        }
        profilesListPanel.revalidate();
        profilesListPanel.repaint();
    }

    private void rebuildLauncherAndVersions() {
        File modsDir = selectedGameDir != null ? new File(selectedGameDir, "mods") : null;

        // 1. Cigarette JARs in mods/
        if (installerModsPanel != null) {
            installerModsPanel.removeAll();
            List<File> jars = MinecraftBridge.listCigaretteJarsInMods(modsDir);
            boolean has189 = false;
            boolean has263 = false;

            if (jars.isEmpty()) {
                JLabel empty = new JLabel("No Cigarette V1 JARs currently in " + (modsDir != null ? modsDir.getAbsolutePath() : "mods/"));
                empty.setForeground(TEXT_MUTED);
                empty.setFont(new Font("SansSerif", Font.PLAIN, 11));
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                empty.setBorder(new EmptyBorder(4, 2, 4, 2));
                installerModsPanel.add(empty);
            } else {
                for (File f : jars) {
                    String n = f.getName().toLowerCase(Locale.ROOT);
                    boolean modern = n.contains("26.3") || n.contains("fabric");
                    if (modern) has263 = true;
                    else has189 = true;

                    RoundedPanel row = new RoundedPanel(new BorderLayout(10, 0), 8, BG_ROOT, BORDER_COL);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    row.setBorder(new EmptyBorder(7, 11, 7, 11));
                    JLabel lbl = new JLabel(f.getName() + "  (" + (f.length() / 1024) + " KB)",
                            FaIcon.of(FaIcon.Glyph.CHECK, 13, EMERALD), SwingConstants.LEFT);
                    lbl.setForeground(TEXT_PRIMARY);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    JLabel tag = makePill(modern ? "26.3 FABRIC" : "1.8.9 FORGE", BG_CARD_ALT, modern ? accent : EMBER);
                    row.add(lbl, BorderLayout.CENTER);
                    row.add(tag, BorderLayout.EAST);
                    installerModsPanel.add(row);
                    installerModsPanel.add(Box.createVerticalStrut(5));
                }
            }

            if (installerConflictLabel != null) {
                if (has189 && has263) {
                    installerConflictLabel.setText("Conflict detected: both 1.8.9 Forge and 26.3 Fabric JARs exist in mods/ — click 'Clean Conflicting JARs'!");
                    installerConflictLabel.setForeground(CRIMSON);
                } else if (jars.isEmpty()) {
                    installerConflictLabel.setText("Ready to install Cigarette V1 (" + edition + ") into " + (modsDir != null ? modsDir.getAbsolutePath() : "mods/"));
                    installerConflictLabel.setForeground(AMBER);
                } else {
                    installerConflictLabel.setText("Clean Cigarette V1 installation verified in " + (modsDir != null ? modsDir.getAbsolutePath() : "mods/"));
                    installerConflictLabel.setForeground(EMERALD);
                }
            }
            installerModsPanel.revalidate();
            installerModsPanel.repaint();
        }

        // 2. Multi-Version Via Bridge Status
        if (viaStatusLabel != null && !viaDownloadInFlight) {
            List<File> viaJars = MinecraftBridge.listViaJarsInMods(modsDir);
            if (viaJars.isEmpty()) {
                viaStatusLabel.setText("Not installed in mods/ yet — click Auto-Install below to enable 1.8 \u2192 1.21.x \u2192 26.x protocol bridging.");
                viaStatusLabel.setForeground(AMBER);
            } else {
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < viaJars.size(); i++) {
                    if (i > 0) names.append(", ");
                    names.append(viaJars.get(i).getName());
                }
                viaStatusLabel.setText("Installed in mods/: " + names.toString());
                viaStatusLabel.setForeground(EMERALD);
            }
        }

        // 3. Installed Versions Scanner (<gameDir>/versions/)
        if (installedVersionsPanel != null) {
            installedVersionsPanel.removeAll();
            List<MinecraftBridge.InstalledVersion> versions = MinecraftBridge.scanInstalledVersions(selectedGameDir);
            if (versions.isEmpty()) {
                JLabel none = new JLabel("No version folders detected in "
                        + (selectedGameDir != null ? new File(selectedGameDir, "versions").getAbsolutePath() : "versions/")
                        + " yet.");
                none.setForeground(TEXT_MUTED);
                none.setFont(new Font("SansSerif", Font.PLAIN, 11));
                none.setAlignmentX(Component.LEFT_ALIGNMENT);
                installedVersionsPanel.add(none);
            } else {
                for (MinecraftBridge.InstalledVersion v : versions) {
                    Color badgeCol = v.tier == 0 ? EMERALD : (v.tier == 1 ? CYAN : AMBER);
                    RoundedPanel row = new RoundedPanel(new BorderLayout(10, 2), 8, BG_ROOT, BORDER_COL);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    row.setBorder(new EmptyBorder(7, 11, 7, 11));

                    JLabel verIdLbl = new JLabel(v.id, FaIcon.of(v.tier == 0 ? FaIcon.Glyph.CHECK : FaIcon.Glyph.GLOBE, 13, badgeCol), SwingConstants.LEFT);
                    verIdLbl.setForeground(TEXT_PRIMARY);
                    verIdLbl.setFont(new Font("SansSerif", Font.BOLD, 12));

                    JLabel detailLbl = new JLabel(v.detail);
                    detailLbl.setForeground(TEXT_MUTED);
                    detailLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));

                    JPanel leftCol = new JPanel(new BorderLayout(0, 2));
                    leftCol.setOpaque(false);
                    leftCol.add(verIdLbl, BorderLayout.NORTH);
                    leftCol.add(detailLbl, BorderLayout.SOUTH);

                    JLabel pill = makePill(v.badge, BG_CARD_ALT, badgeCol);
                    JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
                    pillWrap.setOpaque(false);
                    pillWrap.add(pill);

                    row.add(leftCol, BorderLayout.CENTER);
                    row.add(pillWrap, BorderLayout.EAST);
                    installedVersionsPanel.add(row);
                    installedVersionsPanel.add(Box.createVerticalStrut(5));
                }
            }
            installedVersionsPanel.revalidate();
            installedVersionsPanel.repaint();
        }
    }

    // ─── Environment Polling & Live Status ────────────────────────────────────

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
                headerStatusPill.setText("GAME RUNNING \u2022 LIVE SYNC");
                headerStatusPill.setForeground(EMERALD);
                dashBannerCard.setStripe(EMERALD, RoundedPanel.STRIPE_LEFT);
                dashBannerTitle.setText("Minecraft Running (PID " + mcProc.pid + ") — Cigarette V1 Active");
                dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.CHECK, 16, EMERALD));
                dashBannerBody.setText("<html>Live JSON bridge connected. Toggle modules here or click <b>Open ClickGUI In-Game</b> (or press <b>Right Shift</b> in Minecraft).</html>");
                dashOpenClickGuiBtn.setVisible(true);
            } else {
                headerStatusPill.setText("GAME RUNNING (MOD NOT IN MODS)");
                headerStatusPill.setForeground(AMBER);
                dashBannerCard.setStripe(AMBER, RoundedPanel.STRIPE_LEFT);
                dashBannerTitle.setText(mcProc.kind + " Running (PID " + mcProc.pid + ") — Install Needed");
                dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.WARNING, 16, AMBER));
                dashBannerBody.setText("<html>Minecraft started without Cigarette V1 in <code>mods/</code>. Click <b>Install / Update in mods/</b>, restart Minecraft, and press <b>Right Shift</b> in-game.</html>");
                dashOpenClickGuiBtn.setVisible(false);
            }
        } else if (launcherProc != null) {
            headerStatusPill.setText("LAUNCHER OPEN (" + launcherProc.kind.toUpperCase(Locale.ROOT) + ")");
            headerStatusPill.setForeground(accent);
            dashBannerCard.setStripe(accent, RoundedPanel.STRIPE_LEFT);
            dashBannerTitle.setText("Launcher Detected (PID " + launcherProc.pid + ") — Ready");
            dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.ROCKET, 16, accent));
            dashBannerBody.setText("<html>Your launcher is open. Verify Cigarette V1 is installed in <code>mods/</code>, optionally add ViaForge/ViaFabricPlus, and launch!</html>");
            dashOpenClickGuiBtn.setVisible(false);
        } else {
            headerStatusPill.setText("MINECRAFT OFFLINE");
            headerStatusPill.setForeground(TEXT_MUTED);
            dashBannerCard.setStripe(accent, RoundedPanel.STRIPE_LEFT);
            dashBannerTitle.setText("Cigarette V1 Companion Ready — Background Tray Active");
            dashBannerTitle.setIcon(FaIcon.of(FaIcon.Glyph.GAUGE, 16, accent));
            dashBannerBody.setText("<html>Pre-configure all 47 modules, sliders, and keybinds offline anytime, launch your client from <b>Launcher &amp; 1.8+</b>, and press <b>Right Shift</b> in-game.</html>");
            dashOpenClickGuiBtn.setVisible(false);
        }
        dashBannerCard.repaint();
        rebuildLauncherAndVersions();
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

    private void populateLauncherCombo() {
        if (launcherCombo == null) return;
        DefaultComboBoxModel<MinecraftBridge.LauncherInfo> model = new DefaultComboBoxModel<MinecraftBridge.LauncherInfo>();
        for (MinecraftBridge.LauncherInfo info : discoveredLaunchers) {
            model.addElement(info);
        }
        launcherCombo.setModel(model);
        if (launcherHintLabel != null) {
            if (discoveredLaunchers.isEmpty()) {
                launcherHintLabel.setText("No launcher auto-detected in default paths — click 'Locate .exe' to add your launcher.");
            } else {
                launcherHintLabel.setText("Detected " + discoveredLaunchers.size() + " launcher(s) ready for 1-click launch.");
            }
        }
    }

    private void browseForGameDirectory() {
        JFileChooser chooser = new JFileChooser(selectedGameDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            if (chosen != null) {
                if (!discoveredDirs.contains(chosen)) {
                    discoveredDirs.add(0, chosen);
                }
                selectedGameDir = chosen;
                populateGameDirCombo();
                reloadActiveProfileFromDisk();
                rebuildLauncherAndVersions();
                rebuildModulesGrid();
                rebuildProfilesList();
                updateStatsLabels();
                showToast("Selected Minecraft folder: " + chosen.getAbsolutePath(), EMERALD, FaIcon.Glyph.FOLDER_OPEN);
            }
        }
    }

    private void runInstallToSelectedDir() {
        if (selectedGameDir == null) return;
        MinecraftBridge.InstallResult res = MinecraftBridge.installSelfToMods(selectedGameDir, edition);
        rebuildLauncherAndVersions();
        updateStatsLabels();
        showToast(res.message, res.ok ? EMERALD : AMBER, res.ok ? FaIcon.Glyph.CHECK : FaIcon.Glyph.WARNING);
    }

    private void triggerOpenClickGuiInGame() {
        MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, "open_clickgui");
        showToast("Sent Open ClickGUI signal to running Minecraft", EMERALD, FaIcon.Glyph.PLAY);
        if (trayIcon != null) {
            trayIcon.displayMessage("CIGARETTE V1", "Sent Open ClickGUI signal to running Minecraft.", TrayIcon.MessageType.INFO);
        }
    }

    private File getProfilesDir() {
        return MinecraftBridge.getProfilesDir(selectedGameDir != null ? selectedGameDir : new File("."), edition);
    }

    @SuppressWarnings("unchecked")
    private void reloadActiveProfileFromDisk() {
        boolean modern = DesktopCatalog.isModernEdition(edition);
        activeProfileData = MinecraftBridge.loadProfileMap(getProfilesDir(), activeProfileName, modern);
        Object uiObj = activeProfileData.get("ui");
        if (uiObj instanceof Map) {
            Object tp = ((Map<String, Object>) uiObj).get("themePreset");
            if (tp instanceof String) {
                applyThemePreset((String) tp, false);
            }
        }
    }

    private void saveActiveIfLive() {
        String action = autoLiveSync ? "reload" : "none";
        MinecraftBridge.saveProfileAndSignal(getProfilesDir(), activeProfileName, activeProfileData, action);
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
        if (existing instanceof Map) {
            return (Map<String, Object>) existing;
        }
        if (def.altKey != null) {
            Object altExisting = modulesMap.get(def.altKey);
            if (altExisting instanceof Map) {
                Map<String, Object> found = (Map<String, Object>) altExisting;
                modulesMap.put(def.name, found);
                return found;
            }
        }
        boolean modern = DesktopCatalog.isModernEdition(edition);
        Map<String, Object> md = new LinkedHashMap<String, Object>();
        md.put("enabled", Boolean.valueOf(def.defaultEnabled));
        md.put("keyCode", Integer.valueOf(def.getDefaultKeyCode(modern)));
        md.put("hidden", Boolean.FALSE);
        md.put("settings", def.createDefaultSettingsMap(modern));
        modulesMap.put(def.name, md);
        return md;
    }

    private static int getModuleKeyCode(Map<String, Object> md, DesktopCatalog.ModuleDef def, boolean modern) {
        Object raw = md.get("keyCode");
        if (raw instanceof Number) {
            int kc = ((Number) raw).intValue();
            if ("ClickGUI".equalsIgnoreCase(def.name) && DesktopCatalog.isBrokenClickGuiKeyCode(kc, modern) && kc != 0) {
                return def.getDefaultKeyCode(modern);
            }
            return kc;
        }
        return def.getDefaultKeyCode(modern);
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
        if (titleLogoLbl != null) {
            titleLogoLbl.setIcon(FaIcon.of(FaIcon.Glyph.LOGO, 22, accent));
        }
        if (versionBadge != null) {
            versionBadge.setAccentColor(accent);
        }
        if (editionPill != null) {
            editionPill.setForeground(accent);
        }
        refreshCategoryChips();
        rebuildThemePresetButtons();
        updateStatsLabels();
        selectTab(activeTab);
        rebuildModulesGrid();
        rebuildProfilesList();
        rebuildLauncherAndVersions();
        if (frame != null) {
            frame.repaint();
        }
        if (persist) {
            saveActiveIfLive();
        }
    }

    private void updateStatsLabels() {
        if (statModulesLbl == null) return;
        int enabled = 0;
        Map<String, Object> modules = getModulesSection(activeProfileData);
        for (DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            Map<String, Object> md = getOrCreateModuleMap(modules, def);
            if (Boolean.TRUE.equals(md.get("enabled"))) {
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

        if (toastBar != null) {
            toastBar.setSummary("Profile: " + activeProfileName + "  \u2022  " + enabled + "/" + DesktopCatalog.getModules().size() + " Active");
        }
    }

    private void selectTab(String id) {
        activeTab = id;
        if (contentCards != null) contentCards.show(contentArea, id);
        for (Map.Entry<String, NavButton> e : navButtons.entrySet()) {
            e.getValue().setActive(e.getKey().equals(id), accent);
        }
    }

    private void showToast(String message, Color tone, FaIcon.Glyph glyph) {
        if (toastBar != null) {
            toastBar.flash(message, tone, glyph);
        }
    }

    // ─── System Tray Integration ──────────────────────────────────────────────

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
            addTrayJump(quickJump, "Modules (47)", "modules");
            addTrayJump(quickJump, "Profiles", "profiles");
            addTrayJump(quickJump, "Launcher & Multi-Version", "installer");
            addTrayJump(quickJump, "Settings & Tray", "settings");

            MenuItem exitItem = new MenuItem("Quit Cigarette V1 Companion");
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

            trayIcon = new TrayIcon(FaIcon.createAppImage(32, accent), "CIGARETTE V1 v1.4 (" + edition + ")", menu);
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
            trayIcon.displayMessage("CIGARETTE V1 Running in Background",
                    "Click the Cigarette V1 tray icon anytime to open or hide.", TrayIcon.MessageType.INFO);
        }
    }

    private void updateTrayLabel() {
        if (trayToggleItem != null) {
            trayToggleItem.setLabel(frame.isVisible() ? "Hide to Background" : "Show Cigarette V1");
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

    // ─── Widget Factory Methods ───────────────────────────────────────────────

    private JScrollPane makeDarkScrollPane(Component view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(BG_ROOT);
        JScrollBar vsb = scroll.getVerticalScrollBar();
        vsb.setUI(new DarkScrollBarUI());
        vsb.setPreferredSize(new Dimension(8, 0));
        vsb.setUnitIncrement(18);
        JScrollBar hsb = scroll.getHorizontalScrollBar();
        hsb.setUI(new DarkScrollBarUI());
        hsb.setPreferredSize(new Dimension(0, 8));
        hsb.setUnitIncrement(18);
        return scroll;
    }

    private JScrollPane wrapNorthScrollable(JComponent stack) {
        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.setBackground(BG_ROOT);
        northWrap.add(stack, BorderLayout.NORTH);
        return makeDarkScrollPane(northWrap);
    }

    private RoundedPanel makeStatCard(String label, FaIcon.Glyph glyph, JLabel valueLbl) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), 10, BG_CARD, BORDER_COL);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel top = new JLabel(label, FaIcon.of(glyph, 13, accent), SwingConstants.LEFT);
        top.setForeground(TEXT_MUTED);
        top.setFont(new Font("SansSerif", Font.PLAIN, 11));
        valueLbl.setForeground(TEXT_PRIMARY);
        valueLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        card.add(top, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel makeToggleSettingRow(String title, String sub, boolean checked, ActionListener listener) {
        RoundedPanel row = new RoundedPanel(new BorderLayout(12, 0), 10, BG_ROOT, BORDER_COL);
        row.setBorder(new EmptyBorder(9, 12, 9, 12));

        JPanel labels = new JPanel(new BorderLayout(0, 2));
        labels.setOpaque(false);
        JLabel tLbl = new JLabel(title);
        tLbl.setForeground(TEXT_PRIMARY);
        tLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel sLbl = new JLabel(sub);
        sLbl.setForeground(TEXT_MUTED);
        sLbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        labels.add(tLbl, BorderLayout.NORTH);
        labels.add(sLbl, BorderLayout.SOUTH);

        ToggleSwitch sw = new ToggleSwitch(checked);
        if (listener != null) {
            sw.addActionListener(listener);
        }
        JPanel swWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        swWrap.setOpaque(false);
        swWrap.add(sw);

        row.add(labels, BorderLayout.CENTER);
        row.add(swWrap, BorderLayout.EAST);
        return row;
    }

    private JLabel makePill(final String text, final Color bg, final Color fg) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                    Color f = getForeground();
                    g2.setColor(new Color(f.getRed(), f.getGreen(), f.getBlue(), 70));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, 10, 10));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        lbl.setOpaque(false);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        return lbl;
    }

    private ModernButton makeActionButton(String text, FaIcon.Glyph glyph, Color bg, Color fg) {
        return new ModernButton(text, glyph != null ? FaIcon.of(glyph, 13, fg) : null, bg, fg);
    }

    private ModernButton makeIconButton(FaIcon.Glyph glyph, String tooltip, Color fg, Color bg) {
        ModernButton b = new ModernButton(null, FaIcon.of(glyph, 14, fg), bg, fg);
        b.setToolTipText(tooltip);
        b.setPreferredSize(new Dimension(28, 28));
        b.setBorder(new EmptyBorder(4, 4, 4, 4));
        return b;
    }

    private void styleCombo(final JComboBox<?> combo) {
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COL, 1),
                new EmptyBorder(3, 8, 3, 4)));
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                ModernButton arrow = new ModernButton(null, FaIcon.of(FaIcon.Glyph.CHEVRON_DOWN, 12, TEXT_MUTED), BG_INPUT, TEXT_MUTED);
                arrow.setBorder(new EmptyBorder(2, 6, 2, 6));
                return arrow;
            }

            @Override
            protected ComboPopup createPopup() {
                return new BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane sp = super.createScroller();
                        sp.setBorder(null);
                        sp.getVerticalScrollBar().setUI(new DarkScrollBarUI());
                        sp.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
                        return sp;
                    }
                };
            }
        });
        Object child = combo.getAccessibleContext().getAccessibleChild(0);
        if (child instanceof BasicComboPopup) {
            ((BasicComboPopup) child).setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        }
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setBackground(isSelected ? accent : BG_CARD);
                lbl.setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);
                lbl.setBorder(new EmptyBorder(5, 9, 5, 9));
                if (value instanceof File) {
                    lbl.setText(((File) value).getAbsolutePath());
                } else if (value == null) {
                    lbl.setText("No launcher detected");
                }
                return lbl;
            }
        });
    }

    private static double roundToStep(double value, double step) {
        double safeStep = step > 0.0 ? step : 0.01;
        double snapped = Math.round(value / safeStep) * safeStep;
        int scale = decimalsForStep(safeStep);
        return BigDecimal.valueOf(snapped).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private static String formatStepValue(double value, double step) {
        double safeStep = step > 0.0 ? step : 0.1;
        int decimals = decimalsForStep(safeStep);
        double clean = roundToStep(value, safeStep);
        return String.format(Locale.US, "%." + decimals + "f", Double.valueOf(clean));
    }

    private static int decimalsForStep(double step) {
        if (step >= 1.0) return 0;
        if (step >= 0.1) return 1;
        return 2;
    }

    private static Color brighten(Color c, int delta) {
        return new Color(
                Math.min(255, Math.max(0, c.getRed() + delta)),
                Math.min(255, Math.max(0, c.getGreen() + delta)),
                Math.min(255, Math.max(0, c.getBlue() + delta)),
                c.getAlpha());
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Custom Anti-Aliased Swing UI Helpers (Zero Ugly Metal Widgets)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * 1. RoundedPanel: Anti-aliased RoundRectangle2D background + subtle 1px border + optional accent stripe.
     */
    private static final class RoundedPanel extends JPanel {
        static final int STRIPE_NONE = 0;
        static final int STRIPE_LEFT = 1;
        static final int STRIPE_TOP = 2;

        private final int arc;
        private Color fillColor;
        private Color strokeColor;
        private Color stripeColor;
        private int stripeSide = STRIPE_NONE;

        RoundedPanel(java.awt.LayoutManager layout, int arc, Color fillColor, Color strokeColor) {
            super(layout);
            this.arc = arc;
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            setOpaque(false);
        }

        void setStripe(Color color, int side) {
            this.stripeColor = color;
            this.stripeSide = side;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D.Double shape = new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, arc, arc);
                if (fillColor != null) {
                    g2.setColor(fillColor);
                    g2.fill(shape);
                }
                if (stripeColor != null && stripeSide != STRIPE_NONE) {
                    g2.setClip(shape);
                    g2.setColor(stripeColor);
                    if (stripeSide == STRIPE_LEFT) {
                        g2.fillRect(0, 0, 4, h);
                    } else if (stripeSide == STRIPE_TOP) {
                        g2.fillRect(0, 0, w, 3);
                    }
                    g2.setClip(null);
                }
                if (strokeColor != null) {
                    g2.setColor(strokeColor);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.draw(shape);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * 2. DarkScrollBarUI: Sleek 6px rounded dark thumb (#32323E, hover = accent) on #141416 track, zero arrow buttons.
     */
    private final class DarkScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = SCROLL_THUMB;
            this.trackColor = BG_ROOT;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            Dimension zero = new Dimension(0, 0);
            btn.setPreferredSize(zero);
            btn.setMinimumSize(zero);
            btn.setMaximumSize(zero);
            btn.setFocusable(false);
            btn.setBorder(null);
            btn.setOpaque(false);
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(BG_ROOT);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging || isThumbRollover() ? accent : SCROLL_THUMB);
                if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                    int x = thumbBounds.x + (thumbBounds.width - 6) / 2;
                    g2.fill(new RoundRectangle2D.Double(x, thumbBounds.y + 1, 6, Math.max(12, thumbBounds.height - 2), 6, 6));
                } else {
                    int y = thumbBounds.y + (thumbBounds.height - 6) / 2;
                    g2.fill(new RoundRectangle2D.Double(thumbBounds.x + 1, y, Math.max(12, thumbBounds.width - 2), 6, 6, 6));
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * 3. ToggleSwitch: Custom-painted smooth pill toggle switch (36x18px) with circular white knob &amp; accent fill.
     */
    private final class ToggleSwitch extends JPanel {
        private boolean selected;
        private boolean hovered;
        private final List<ActionListener> listeners = new ArrayList<ActionListener>();

        ToggleSwitch(boolean initial) {
            this.selected = initial;
            Dimension sz = new Dimension(36, 18);
            setPreferredSize(sz);
            setMinimumSize(sz);
            setMaximumSize(sz);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) return;
                    selected = !selected;
                    repaint();
                    ActionEvent ev = new ActionEvent(ToggleSwitch.this, ActionEvent.ACTION_PERFORMED, "toggle");
                    for (ActionListener l : listeners) {
                        l.actionPerformed(ev);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        boolean isSelected() {
            return selected;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        void addActionListener(ActionListener l) {
            listeners.add(l);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Color pillBg = selected
                        ? (hovered ? brighten(accent, 16) : accent)
                        : (hovered ? new Color(0x34, 0x34, 0x42) : new Color(0x28, 0x28, 0x33));
                g2.setColor(pillBg);
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));
                if (!selected) {
                    g2.setColor(new Color(0x3D, 0x3D, 0x4C));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, h, h));
                }
                double knobSize = h - 4.0;
                double kx = selected ? (w - knobSize - 2.0) : 2.0;
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(kx, 2.0, knobSize, knobSize));
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * 4. ModernButton: Custom-painted rounded rect (arc=8), hover brightening, crisp text + FaIcon.
     */
    private static class ModernButton extends JButton {
        private Color bgColor;
        private Color borderColor;
        private final int arc = 8;

        ModernButton(String text, FaIcon icon, Color bg, Color fg) {
            super(text, icon);
            this.bgColor = bg != null ? bg : BG_CARD_ALT;
            this.borderColor = null;
            setForeground(fg != null ? fg : TEXT_PRIMARY);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setIconTextGap(7);
            setFont(new Font("SansSerif", Font.BOLD, 11));
            setBorder(new EmptyBorder(6, 11, 6, 11));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setColors(Color bg, Color fg, Color border) {
            this.bgColor = bg;
            this.borderColor = border;
            setForeground(fg);
            repaint();
        }

        void setBorderColor(Color border) {
            this.borderColor = border;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = bgColor;
                if (!isEnabled()) {
                    fill = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 110);
                } else if (getModel().isPressed()) {
                    fill = brighten(fill, -12);
                } else if (getModel().isRollover()) {
                    fill = brighten(fill, 18);
                }
                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), arc, arc));
                if (borderColor != null) {
                    g2.setColor(borderColor);
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1.0, getHeight() - 1.0, arc, arc));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Glowing v1.4 badge for the CIGARETTE V1 header bar.
     */
    private static final class GlowBadge extends JComponent {
        private final String label;
        private Color glowColor;

        GlowBadge(String label, Color glowColor) {
            this.label = label;
            this.glowColor = glowColor;
            setFont(new Font("SansSerif", Font.BOLD, 10));
            setOpaque(false);
        }

        void setAccentColor(Color c) {
            this.glowColor = c;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(label) + 18, 20);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 38));
                g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 10, 10));
                g2.setColor(new Color(EMBER.getRed(), EMBER.getGreen(), EMBER.getBlue(), 45));
                g2.fill(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 9, 9));
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 195));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, w - 1.0, h - 1.0, 10, 10));

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(label)) / 2;
                int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(Color.WHITE);
                g2.drawString(label, tx, ty);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Custom horizontal dark slider bar with step snapping for NUMBER and RANGE settings.
     */
    private final class DarkSliderBar extends JPanel {
        private final double min;
        private final double max;
        private final double step;
        private double value;
        private boolean hovered;
        private Runnable changeListener;

        DarkSliderBar(double min, double max, double step, double initial) {
            this.min = min;
            this.max = max > min ? max : min + 1.0;
            this.step = step > 0.0 ? step : 0.01;
            this.value = clampAndSnap(initial);
            setOpaque(false);
            setPreferredSize(new Dimension(160, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    updateFromX(e.getX());
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    updateFromX(e.getX());
                }
            });
        }

        private double clampAndSnap(double v) {
            double clamped = Math.max(min, Math.min(max, v));
            return Math.max(min, Math.min(max, roundToStep(clamped, step)));
        }

        private void updateFromX(int mouseX) {
            int pad = 7;
            int usable = Math.max(1, getWidth() - pad * 2);
            double frac = Math.max(0.0, Math.min(1.0, (mouseX - pad) / (double) usable));
            double next = clampAndSnap(min + frac * (max - min));
            if (Math.abs(next - value) > 1e-9) {
                value = next;
                repaint();
                if (changeListener != null) {
                    changeListener.run();
                }
            }
        }

        double getValue() {
            return value;
        }

        void setValueQuiet(double v) {
            this.value = clampAndSnap(v);
            repaint();
        }

        void setChangeListener(Runnable r) {
            this.changeListener = r;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int pad = 7;
                int w = getWidth();
                int h = getHeight();
                int trackY = (h - 6) / 2;
                int trackW = Math.max(4, w - pad * 2);

                g2.setColor(BG_INPUT);
                g2.fill(new RoundRectangle2D.Double(pad, trackY, trackW, 6, 6, 6));
                g2.setColor(BORDER_COL);
                g2.draw(new RoundRectangle2D.Double(pad, trackY, trackW, 6, 6, 6));

                double frac = (value - min) / (max - min);
                frac = Math.max(0.0, Math.min(1.0, frac));
                double fillW = frac * trackW;
                if (fillW > 1.0) {
                    g2.setColor(accent);
                    g2.fill(new RoundRectangle2D.Double(pad, trackY, fillW, 6, 6, 6));
                }

                double knobX = pad + fillW - 6.5;
                double knobY = (h - 13.0) / 2.0;
                if (hovered) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 75));
                    g2.fill(new Ellipse2D.Double(knobX - 2.5, knobY - 2.5, 18.0, 18.0));
                }
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(knobX, knobY, 13.0, 13.0));
                g2.setColor(accent);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(knobX, knobY, 13.0, 13.0));
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Interactive Click-to-Bind Keybind button ([Key: NONE] -> "Press any key (ESC to clear)...").
     */
    private final class KeybindButton extends ModernButton {
        private int keyCode;
        private final boolean modern;
        private final boolean compact;
        private boolean listening = false;
        private Runnable changeListener;

        KeybindButton(int initialKeyCode, final boolean modern, boolean compact) {
            super("", compact ? null : FaIcon.of(FaIcon.Glyph.KEYBOARD, 12, TEXT_PRIMARY), BG_INPUT, TEXT_PRIMARY);
            this.keyCode = initialKeyCode;
            this.modern = modern;
            this.compact = compact;
            setFocusTraversalKeysEnabled(false);
            setBorder(compact ? new EmptyBorder(4, 7, 4, 7) : new EmptyBorder(5, 10, 5, 10));
            setFont(new Font("SansSerif", Font.BOLD, compact ? 10 : 11));
            refreshLabel();

            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listening = !listening;
                    refreshLabel();
                    if (listening) {
                        requestFocusInWindow();
                    }
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!listening) return;
                    e.consume();
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        keyCode = 0;
                        listening = false;
                        refreshLabel();
                        if (changeListener != null) changeListener.run();
                        return;
                    }
                    int mapped = DesktopCatalog.awtToMinecraftKeyCode(e.getKeyCode(), e.getKeyLocation(), modern);
                    if (mapped > 0) {
                        keyCode = mapped;
                        listening = false;
                        refreshLabel();
                        if (changeListener != null) changeListener.run();
                    }
                }
            });

            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    if (listening) {
                        listening = false;
                        refreshLabel();
                    }
                }
            });
        }

        private void refreshLabel() {
            if (listening) {
                setText(compact ? "Press key..." : "Press any key (ESC to clear)...");
                setColors(new Color(0x2E, 0x24, 0x16), AMBER, AMBER);
                setToolTipText("Press any key to bind, or ESC to clear to NONE");
            } else {
                String formatted = DesktopCatalog.formatKeyCode(keyCode, modern);
                setText(compact ? "[" + formatted + "]" : "[Key: " + formatted + "]");
                boolean bound = keyCode > 0;
                setColors(BG_INPUT, bound ? accent : TEXT_MUTED, bound ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110) : BORDER_COL);
                setToolTipText("Click to bind key (Current: " + formatted + ")");
            }
        }

        int getKeyCode() {
            return keyCode;
        }

        void setChangeListener(Runnable r) {
            this.changeListener = r;
        }
    }

    /**
     * 5. Non-blocking Status Toast Bar docked at the bottom of the Cigarette V1 window.
     */
    private final class ToastBar extends JPanel {
        private final JLabel msgLabel;
        private final JLabel summaryLabel;
        private final Timer revertTimer;
        private Color activeHighlight = null;

        ToastBar() {
            super(new BorderLayout(10, 0));
            setBackground(new Color(0x16, 0x16, 0x1B));
            setPreferredSize(new Dimension(0, 30));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL),
                    new EmptyBorder(5, 14, 5, 14)));

            msgLabel = new JLabel("Cigarette V1 v1.4 Ready \u2022 Live JSON bridge standing by",
                    FaIcon.of(FaIcon.Glyph.CHECK, 12, EMERALD), SwingConstants.LEFT);
            msgLabel.setForeground(TEXT_MUTED);
            msgLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

            summaryLabel = new JLabel("Profile: default");
            summaryLabel.setForeground(TEXT_MUTED);
            summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 10));

            add(msgLabel, BorderLayout.CENTER);
            add(summaryLabel, BorderLayout.EAST);

            revertTimer = new Timer(5200, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    activeHighlight = null;
                    msgLabel.setText("Cigarette V1 v1.4 Ready \u2022 Live JSON bridge active");
                    msgLabel.setForeground(TEXT_MUTED);
                    msgLabel.setIcon(FaIcon.of(FaIcon.Glyph.CHECK, 12, EMERALD));
                    repaint();
                }
            });
            revertTimer.setRepeats(false);
        }

        void flash(String text, Color tone, FaIcon.Glyph glyph) {
            Color safeTone = tone != null ? tone : accent;
            activeHighlight = safeTone;
            msgLabel.setText(text);
            msgLabel.setForeground(TEXT_PRIMARY);
            msgLabel.setIcon(FaIcon.of(glyph != null ? glyph : FaIcon.Glyph.INFO, 13, safeTone));
            revertTimer.restart();
            repaint();
        }

        void setSummary(String summary) {
            summaryLabel.setText(summary);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (activeHighlight != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setColor(new Color(activeHighlight.getRed(), activeHighlight.getGreen(), activeHighlight.getBlue(), 26));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(activeHighlight);
                    g2.fillRect(0, 0, 4, getHeight());
                } finally {
                    g2.dispose();
                }
            }
        }
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
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(activeAccent);
                    g2.fillRoundRect(0, 5, 3, getHeight() - 10, 3, 3);
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 14));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
