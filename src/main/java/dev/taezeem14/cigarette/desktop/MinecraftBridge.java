package dev.taezeem14.cigarette.desktop;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure JDK 8 process scanner, mod installer, and live profile JSON bridge between the desktop GUI and Minecraft.
 */
public final class MinecraftBridge {

    public static final class RunningProcess {
        public final long pid;
        public final String kind;
        public final String gameDir;
        public final String modsFolder;
        public final boolean cigaretteLoaded;
        public final String commandPreview;

        public RunningProcess(long pid, String kind, String gameDir, String modsFolder,
                              boolean cigaretteLoaded, String commandPreview) {
            this.pid = pid;
            this.kind = kind;
            this.gameDir = gameDir;
            this.modsFolder = modsFolder;
            this.cigaretteLoaded = cigaretteLoaded;
            this.commandPreview = commandPreview;
        }
    }

    public static final class InstallResult {
        public final boolean ok;
        public final String message;
        public final File targetJar;

        public InstallResult(boolean ok, String message, File targetJar) {
            this.ok = ok;
            this.message = message;
            this.targetJar = targetJar;
        }
    }

    private MinecraftBridge() {
    }

    public static File getSelfJarFile() {
        try {
            URI uri = MinecraftBridge.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File f = new File(uri);
            if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return f;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static List<RunningProcess> scanRunningMinecraft() {
        List<RunningProcess> out = new ArrayList<RunningProcess>();
        List<String> lines = queryJavaCommandLines();
        for (String line : lines) {
            int sep = line.indexOf('|');
            if (sep <= 0) continue;
            long pid;
            try {
                pid = Long.parseLong(line.substring(0, sep).trim());
            } catch (NumberFormatException e) {
                continue;
            }
            String cmd = line.substring(sep + 1).trim();
            String lower = cmd.toLowerCase(Locale.ROOT);
            if (lower.contains("cigarettedesktopapp")) continue;

            boolean isMc = lower.contains("net.minecraft.launchwrapper.launch")
                    || lower.contains("net.minecraft.client.main.main")
                    || lower.contains("net.fabricmc.loader")
                    || lower.contains("knotclient");
            boolean isTLauncher = lower.contains("tlauncher") && !isMc;
            if (!isMc && !isTLauncher) continue;

            String kind;
            if (isTLauncher) {
                kind = "TLauncher";
            } else if (lower.contains("fabricmc") || lower.contains("knotclient")) {
                kind = "Minecraft (Fabric)";
            } else if (lower.contains("fmltweaker") || lower.contains("minecraftforge")) {
                kind = "Minecraft (Forge)";
            } else {
                kind = "Minecraft";
            }

            String gameDir = extractArg(cmd, "--gameDir");
            if (gameDir == null) gameDir = extractProp(cmd, "-Dminecraft.applet.TargetDirectory=");
            if (gameDir == null) gameDir = extractProp(cmd, "-Dfabric.gameDir=");

            String modsFolder = extractProp(cmd, "-DLibLoader.modsFolder=");
            if (modsFolder == null && gameDir != null) {
                modsFolder = new File(gameDir, "mods").getAbsolutePath();
            }

            boolean loaded = lower.contains("cigarette-v1") || containsCigaretteJar(modsFolder);
            String preview = cmd.length() > 140 ? cmd.substring(0, 140) + "..." : cmd;
            out.add(new RunningProcess(pid, kind, gameDir, modsFolder, loaded, preview));
        }
        return out;
    }

    private static boolean containsCigaretteJar(String modsFolder) {
        if (modsFolder == null) return false;
        File dir = new File(modsFolder);
        if (!dir.isDirectory()) return false;
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase(Locale.ROOT).startsWith("cigarette")
                    && f.getName().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return true;
            }
        }
        return false;
    }

    private static List<String> queryJavaCommandLines() {
        List<String> reflected = queryViaProcessHandle();
        if (reflected != null && !reflected.isEmpty()) {
            return reflected;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return runCommandLines(new String[]{
                    "powershell", "-NoProfile", "-Command",
                    "Get-CimInstance Win32_Process -Filter \"Name LIKE 'java%'\" | ForEach-Object { \"$($_.ProcessId)|$($_.CommandLine)\" }"
            });
        }
        return runCommandLines(new String[]{"ps", "-eo", "pid,args"});
    }

    @SuppressWarnings("unchecked")
    private static List<String> queryViaProcessHandle() {
        try {
            Class<?> phClass = Class.forName("java.lang.ProcessHandle");
            Method allProcesses = phClass.getMethod("allProcesses");
            Method pidMethod = phClass.getMethod("pid");
            Method infoMethod = phClass.getMethod("info");
            Class<?> infoClass = Class.forName("java.lang.ProcessHandle$Info");
            Method cmdLineMethod = infoClass.getMethod("commandLine");
            Method cmdMethod = infoClass.getMethod("command");

            Object stream = allProcesses.invoke(null);
            Method collect = stream.getClass().getMethod("toArray");
            Object[] arr = (Object[]) collect.invoke(stream);
            List<String> lines = new ArrayList<String>();
            for (Object ph : arr) {
                Object info = infoMethod.invoke(ph);
                Object optCmd = cmdMethod.invoke(info);
                Object optLine = cmdLineMethod.invoke(info);
                String exe = optString(optCmd);
                String line = optString(optLine);
                if (line != null && (exe == null || exe.toLowerCase(Locale.ROOT).contains("java"))) {
                    lines.add(pidMethod.invoke(ph) + "|" + line);
                }
            }
            return lines;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String optString(Object optional) {
        if (optional == null) return null;
        try {
            Method isPresent = optional.getClass().getMethod("isPresent");
            if (Boolean.TRUE.equals(isPresent.invoke(optional))) {
                return String.valueOf(optional.getClass().getMethod("get").invoke(optional));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static List<String> runCommandLines(String[] cmd) {
        List<String> out = new ArrayList<String>();
        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (!line.contains("|")) {
                        int sp = line.indexOf(' ');
                        if (sp > 0) line = line.substring(0, sp) + "|" + line.substring(sp + 1);
                    }
                    out.add(line);
                }
            }
            p.waitFor();
        } catch (Throwable ignored) {
        } finally {
            if (p != null) p.destroy();
        }
        return out;
    }

    private static String extractArg(String cmd, String flag) {
        Pattern p = Pattern.compile(Pattern.quote(flag) + "\\s+(?:\"([^\"]+)\"|(\\S+))");
        Matcher m = p.matcher(cmd);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return null;
    }

    private static String extractProp(String cmd, String prefix) {
        Pattern p = Pattern.compile(Pattern.quote(prefix) + "(?:\"([^\"]+)\"|(\\S+))");
        Matcher m = p.matcher(cmd);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return null;
    }

    public static List<File> discoverGameDirectories(List<RunningProcess> running) {
        Set<File> candidates = new LinkedHashSet<File>();

        if (running != null) {
            for (RunningProcess rp : running) {
                if (rp.gameDir != null) {
                    File gd = new File(rp.gameDir);
                    if (gd.isDirectory()) candidates.add(gd);
                }
            }
        }

        String appData = System.getenv("APPDATA");
        String userHome = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        List<File> roots = new ArrayList<File>();
        if (os.contains("win")) {
            if (appData != null) {
                roots.add(new File(appData, ".minecraft"));
                scanSubdirs(new File(appData, ".minecraft/versions"), candidates);
                scanSubdirs(new File(appData, "PrismLauncher/instances"), candidates, "minecraft");
                scanSubdirs(new File(appData, "MultiMC/instances"), candidates, ".minecraft");
                scanSubdirs(new File(appData, "ModrinthApp/profiles"), candidates);
            }
            File tlRoot = new File("D:\\Minecraft-launcher");
            if (tlRoot.isDirectory()) {
                roots.add(tlRoot);
                scanSubdirs(new File(tlRoot, "versions"), candidates);
            }
            File tlHome = new File(userHome, ".tlauncher");
            if (tlHome.isDirectory()) {
                roots.add(new File(userHome, "AppData/Roaming/.minecraft"));
            }
        } else if (os.contains("mac")) {
            roots.add(new File(userHome, "Library/Application Support/minecraft"));
            scanSubdirs(new File(userHome, "Library/Application Support/PrismLauncher/instances"), candidates, "minecraft");
        } else {
            roots.add(new File(userHome, ".minecraft"));
            scanSubdirs(new File(userHome, ".local/share/PrismLauncher/instances"), candidates, "minecraft");
        }

        for (File root : roots) {
            if (root.isDirectory()) {
                candidates.add(root);
            }
        }

        if (candidates.isEmpty()) {
            File fallback = os.contains("win") && appData != null
                    ? new File(appData, ".minecraft")
                    : new File(userHome, ".minecraft");
            candidates.add(fallback);
        }
        return new ArrayList<File>(candidates);
    }

    private static void scanSubdirs(File parent, Set<File> out, String... nested) {
        if (parent == null || !parent.isDirectory()) return;
        File[] children = parent.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (!child.isDirectory()) continue;
            File target = child;
            for (String n : nested) {
                target = new File(target, n);
            }
            if (target.isDirectory() && (new File(target, "mods").isDirectory() || new File(target, "options.txt").isFile())) {
                out.add(target);
            }
        }
    }

    public static File getProfilesDir(File gameDir, String edition) {
        String sub = edition != null && edition.contains("26.3") ? "Cigarette" : "cigarette";
        File preferred = new File(new File(gameDir, sub), "profiles");
        if (preferred.isDirectory()) return preferred;

        File alt1 = new File(new File(gameDir, "cigarette"), "profiles");
        if (alt1.isDirectory()) return alt1;
        File alt2 = new File(new File(gameDir, "Cigarette"), "profiles");
        if (alt2.isDirectory()) return alt2;
        return preferred;
    }

    public static List<String> listProfileNames(File profilesDir) {
        List<String> names = new ArrayList<String>();
        if (profilesDir != null && profilesDir.isDirectory()) {
            File[] files = profilesDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith(".json") && !f.getName().startsWith(".")) {
                        names.add(f.getName().substring(0, f.getName().length() - 5));
                    }
                }
            }
        }
        Collections.sort(names);
        if (!names.contains("default")) {
            names.add(0, "default");
        }
        return names;
    }

    public static Map<String, Object> loadProfileMap(File profilesDir, String profileName) {
        if (profilesDir != null) {
            File f = new File(profilesDir, profileName + ".json");
            if (f.isFile()) {
                try {
                    String raw = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    Map<String, Object> parsed = SimpleJson.parseObject(raw);
                    if (!parsed.isEmpty()) return parsed;
                } catch (IOException ignored) {
                }
            }
        }
        return createDefaultProfileMap(profileName);
    }

    public static Map<String, Object> createDefaultProfileMap(String profileName) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("profile_name", profileName);
        Map<String, Object> modules = new LinkedHashMap<String, Object>();
        for (DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            Map<String, Object> md = new LinkedHashMap<String, Object>();
            md.put("enabled", Boolean.valueOf(def.defaultEnabled));
            md.put("keyCode", Integer.valueOf(0));
            md.put("hidden", Boolean.FALSE);
            md.put("settings", new LinkedHashMap<String, Object>());
            modules.put(def.name, md);
        }
        root.put("modules", modules);

        Map<String, Object> ui = new LinkedHashMap<String, Object>();
        ui.put("themePreset", "Electric Blue");
        ui.put("accentColor", Integer.valueOf(DesktopCatalog.getThemeColor("Electric Blue").getRGB()));
        ui.put("guiScale", Double.valueOf(1.0));
        root.put("ui", ui);
        return root;
    }

    public static boolean saveProfileAndSignal(File profilesDir, String profileName, Map<String, Object> profileMap, String action) {
        if (profilesDir == null) return false;
        if (!profilesDir.exists() && !profilesDir.mkdirs()) return false;
        try {
            profileMap.put("profile_name", profileName);
            File target = new File(profilesDir, profileName + ".json");
            Files.write(target.toPath(), SimpleJson.stringify(profileMap).getBytes(StandardCharsets.UTF_8));

            Map<String, Object> signal = new LinkedHashMap<String, Object>();
            signal.put("profile", profileName);
            signal.put("action", action != null ? action : "reload");
            signal.put("timestamp", Long.valueOf(System.currentTimeMillis()));
            File bridge = new File(profilesDir, ".bridge_sync");
            Files.write(bridge.toPath(), SimpleJson.stringify(signal).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteProfile(File profilesDir, String profileName) {
        if (profilesDir == null || "default".equalsIgnoreCase(profileName)) return false;
        File target = new File(profilesDir, profileName + ".json");
        return target.isFile() && target.delete();
    }

    public static List<File> listCigaretteJarsInMods(File modsDir) {
        if (modsDir == null || !modsDir.isDirectory()) return Collections.emptyList();
        File[] files = modsDir.listFiles();
        if (files == null) return Collections.emptyList();
        List<File> list = new ArrayList<File>();
        for (File f : files) {
            if (f.isFile()) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.contains("cigarette") && n.endsWith(".jar")) {
                    list.add(f);
                }
            }
        }
        Collections.sort(list, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return list;
    }

    public static InstallResult installSelfToMods(File gameDir, String currentEdition) {
        File self = getSelfJarFile();
        if (self == null) {
            return new InstallResult(false, "Cannot locate running JAR file (running from unpacked classes).", null);
        }
        File modsDir = new File(gameDir, "mods");
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            return new InstallResult(false, "Failed to create mods folder: " + modsDir.getAbsolutePath(), null);
        }
        File dest = new File(modsDir, self.getName());
        if (dest.getAbsolutePath().equalsIgnoreCase(self.getAbsolutePath())) {
            return new InstallResult(true, "Already installed directly inside " + modsDir.getAbsolutePath(), dest);
        }

        try {
            // Remove older same-edition Cigarette jars to avoid Forge/Fabric duplicate mod errors
            boolean isModern = currentEdition != null && currentEdition.contains("26.3");
            for (File existing : listCigaretteJarsInMods(modsDir)) {
                String lower = existing.getName().toLowerCase(Locale.ROOT);
                boolean existingModern = lower.contains("26.3") || lower.contains("fabric");
                if (existingModern == isModern && !existing.getName().equalsIgnoreCase(self.getName())) {
                    existing.delete();
                }
            }
            Files.copy(self.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new InstallResult(true, "Installed " + self.getName() + " to " + modsDir.getAbsolutePath(), dest);
        } catch (IOException e) {
            return new InstallResult(false, "Copy failed (" + e.getMessage() + "). Close Minecraft first if file is locked.", null);
        }
    }

    public static int removeConflictingJars(File modsDir, String keepEdition) {
        boolean keepModern = keepEdition != null && keepEdition.contains("26.3");
        int removed = 0;
        for (File f : listCigaretteJarsInMods(modsDir)) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            boolean jarModern = n.contains("26.3") || n.contains("fabric");
            if (jarModern != keepModern && f.delete()) {
                removed++;
            }
        }
        return removed;
    }

    public static void openInExplorer(File dir) {
        if (dir == null) return;
        if (!dir.exists()) dir.mkdirs();
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            }
        } catch (Throwable ignored) {
        }
    }
}
