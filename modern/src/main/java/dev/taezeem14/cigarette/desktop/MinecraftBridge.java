package dev.taezeem14.cigarette.desktop;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
 * Pure JDK 8 process scanner, launcher detector, Modrinth multi-version protocol bridge installer,
 * version scanner, and live profile JSON bridge between the Cigarette V1 desktop companion and Minecraft.
 */
public final class MinecraftBridge {

    private static final String USER_AGENT = "Cigarette-V1/1.4.0";

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

    public static final class LauncherInfo {
        public final String name;
        public final File executable;

        public LauncherInfo(String name, File executable) {
            this.name = name;
            this.executable = executable;
        }

        @Override
        public String toString() {
            return name + " — " + executable.getAbsolutePath();
        }
    }

    public static final class InstalledVersion {
        public final String id;
        public final String badge;
        public final String detail;
        public final int tier; // 0 = native match, 1 = via protocol bridge, 2 = loader required

        public InstalledVersion(String id, String badge, String detail, int tier) {
            this.id = id;
            this.badge = badge;
            this.detail = detail;
            this.tier = tier;
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
            File tlRoot = new File("D:\\Minecraft-launcher");
            if (tlRoot.isDirectory()) {
                roots.add(tlRoot);
                scanSubdirs(new File(tlRoot, "versions"), candidates);
            }
            if (appData != null) {
                roots.add(new File(appData, ".minecraft"));
                scanSubdirs(new File(appData, ".minecraft/versions"), candidates);
                scanSubdirs(new File(appData, "PrismLauncher/instances"), candidates, "minecraft");
                scanSubdirs(new File(appData, "MultiMC/instances"), candidates, ".minecraft");
                scanSubdirs(new File(appData, "ModrinthApp/profiles"), candidates);
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

    // ─── Launcher Discovery & Detached Quick-Start ────────────────────────────

    public static List<LauncherInfo> discoverLaunchers() {
        List<LauncherInfo> found = new ArrayList<LauncherInfo>();
        Set<String> seen = new LinkedHashSet<String>();

        String appData = System.getenv("APPDATA");
        String localAppData = System.getenv("LOCALAPPDATA");
        String progFiles = System.getenv("ProgramFiles");
        String progFilesX86 = System.getenv("ProgramFiles(x86)");
        String userHome = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            addLauncherIfPresent(found, seen, "TLauncher", new File("D:\\Minecraft-launcher\\starter\\TLauncher.exe"));
            addLauncherIfPresent(found, seen, "TLauncher", new File("D:\\Minecraft-launcher\\TLauncher.exe"));
            if (appData != null) {
                addLauncherIfPresent(found, seen, "TLauncher", new File(appData, ".minecraft\\TLauncher.exe"));
                addLauncherIfPresent(found, seen, "TLauncher", new File(appData, ".tlauncher\\starter\\TLauncher.exe"));
                addLauncherIfPresent(found, seen, "Prism Launcher", new File(appData, "PrismLauncher\\prismlauncher.exe"));
                addLauncherIfPresent(found, seen, "MultiMC", new File(appData, "MultiMC\\MultiMC.exe"));
            }
            if (localAppData != null) {
                addLauncherIfPresent(found, seen, "Prism Launcher", new File(localAppData, "Programs\\PrismLauncher\\prismlauncher.exe"));
                addLauncherIfPresent(found, seen, "Modrinth App", new File(localAppData, "Programs\\Modrinth App\\Modrinth App.exe"));
            }
            if (progFiles != null) {
                addLauncherIfPresent(found, seen, "Prism Launcher", new File(progFiles, "PrismLauncher\\prismlauncher.exe"));
                addLauncherIfPresent(found, seen, "Minecraft Launcher", new File(progFiles, "Minecraft Launcher\\MinecraftLauncher.exe"));
            }
            if (progFilesX86 != null) {
                addLauncherIfPresent(found, seen, "Minecraft Launcher", new File(progFilesX86, "Minecraft Launcher\\MinecraftLauncher.exe"));
            }
            addLauncherIfPresent(found, seen, "Minecraft Launcher", new File("C:\\XboxGames\\Minecraft Launcher\\Content\\Minecraft.exe"));
            addLauncherIfPresent(found, seen, "MultiMC", new File("C:\\MultiMC\\MultiMC.exe"));
            addLauncherIfPresent(found, seen, "MultiMC", new File("D:\\MultiMC\\MultiMC.exe"));
            addLauncherIfPresent(found, seen, "MultiMC", new File(userHome, "Desktop\\MultiMC\\MultiMC.exe"));
        } else if (os.contains("mac")) {
            addLauncherIfPresent(found, seen, "Prism Launcher", new File("/Applications/Prism Launcher.app"));
            addLauncherIfPresent(found, seen, "MultiMC", new File("/Applications/MultiMC.app"));
            addLauncherIfPresent(found, seen, "Minecraft Launcher", new File("/Applications/Minecraft.app"));
        } else {
            addLauncherIfPresent(found, seen, "Prism Launcher", new File("/usr/bin/prismlauncher"));
            addLauncherIfPresent(found, seen, "Minecraft Launcher", new File("/usr/bin/minecraft-launcher"));
            addLauncherIfPresent(found, seen, "TLauncher (JAR)", new File(userHome, ".tlauncher/TLauncher.jar"));
        }
        return found;
    }

    private static void addLauncherIfPresent(List<LauncherInfo> out, Set<String> seen, String name, File exe) {
        if (exe == null || !exe.exists()) return;
        String key = exe.getAbsolutePath().toLowerCase(Locale.ROOT);
        if (seen.add(key)) {
            out.add(new LauncherInfo(name, exe));
        }
    }

    public static InstallResult launchExecutable(File exe) {
        if (exe == null || !exe.exists()) {
            return new InstallResult(false, "Launcher executable not found on disk.", null);
        }
        try {
            String lower = exe.getName().toLowerCase(Locale.ROOT);
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            ProcessBuilder pb;
            if (lower.endsWith(".jar")) {
                String javaCmd = os.contains("win") ? "javaw" : "java";
                pb = new ProcessBuilder(javaCmd, "-jar", exe.getAbsolutePath());
            } else if (os.contains("mac") && lower.endsWith(".app") && exe.isDirectory()) {
                pb = new ProcessBuilder("open", "-a", exe.getAbsolutePath());
            } else {
                pb = new ProcessBuilder(exe.getAbsolutePath());
            }
            File parent = exe.getParentFile();
            if (parent != null && parent.isDirectory()) {
                pb.directory(parent);
            }
            pb.start();
            return new InstallResult(true, "Launched " + exe.getName(), exe);
        } catch (IOException e) {
            return new InstallResult(false, "Failed to start " + exe.getName() + ": " + e.getMessage(), exe);
        }
    }

    // ─── Installed Versions Scanner (<gameDir>/versions/) ─────────────────────

    public static List<InstalledVersion> scanInstalledVersions(File gameDir) {
        if (gameDir == null) return Collections.emptyList();
        File versionsDir = new File(gameDir, "versions");
        if (!versionsDir.isDirectory()) return Collections.emptyList();

        File[] dirs = versionsDir.listFiles();
        if (dirs == null) return Collections.emptyList();

        List<InstalledVersion> results = new ArrayList<InstalledVersion>();
        for (File d : dirs) {
            if (!d.isDirectory() || d.getName().startsWith(".")) continue;
            String id = d.getName();
            File jsonFile = new File(d, id + ".json");
            File jarFile = new File(d, id + ".jar");
            if (!jsonFile.isFile() && !jarFile.isFile()) {
                // Check if any .json exists inside the version folder
                boolean hasJson = false;
                File[] inner = d.listFiles();
                if (inner != null) {
                    for (File f : inner) {
                        if (f.isFile() && f.getName().endsWith(".json")) {
                            jsonFile = f;
                            hasJson = true;
                            break;
                        }
                    }
                }
                if (!hasJson) continue;
            }

            String metaLower = id.toLowerCase(Locale.ROOT);
            if (jsonFile.isFile() && jsonFile.length() > 0 && jsonFile.length() < 512 * 1024) {
                try {
                    byte[] raw = Files.readAllBytes(jsonFile.toPath());
                    int cap = Math.min(raw.length, 32768);
                    metaLower = metaLower + " " + new String(raw, 0, cap, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                } catch (IOException ignored) {
                }
            }

            boolean hasForge = metaLower.contains("forge") || metaLower.contains("fmltweaker");
            boolean hasFabric = metaLower.contains("fabricmc") || metaLower.contains("fabric-loader") || id.toLowerCase(Locale.ROOT).contains("fabric");
            boolean is189 = id.contains("1.8.9") || id.startsWith("1.8") || metaLower.contains("\"1.8.9\"");
            boolean is26x = id.contains("26.") || metaLower.contains("\"26.3\"") || metaLower.contains("\"26.");

            if (is189 && hasForge) {
                results.add(new InstalledVersion(id,
                        "LEGACY FORGE JAR",
                        "Native 1.8.9 Forge target — add ViaForge to join 1.8 through 1.21.x servers",
                        0));
            } else if (is26x && hasFabric) {
                results.add(new InstalledVersion(id,
                        "MODERN FABRIC JAR",
                        "Native 26.x Fabric target — add ViaFabricPlus to join 1.8 through 26.x servers",
                        0));
            } else if (hasFabric) {
                results.add(new InstalledVersion(id,
                        "FABRIC + VIA BRIDGE",
                        "Fabric profile — pair Modern Fabric Edition with ViaFabricPlus multi-version bridge",
                        1));
            } else if (is189) {
                results.add(new InstalledVersion(id,
                        "1.8.9 (INSTALL FORGE)",
                        "Vanilla/OptiFine 1.8.9 — select Forge 1.8.9 or ForgeOptiFine 1.8.9 to load Cigarette V1",
                        2));
            } else if (is26x) {
                results.add(new InstalledVersion(id,
                        "26.x (INSTALL FABRIC)",
                        "Vanilla 26.x — install Fabric Loader to load Cigarette V1 Modern Edition",
                        2));
            } else {
                results.add(new InstalledVersion(id,
                        "VIA PROTOCOL BRIDGE",
                        "Playable from 1.8.9 Forge (ViaForge) or 26.3 Fabric (ViaFabricPlus) without switching JARs",
                        1));
            }
        }

        Collections.sort(results, new Comparator<InstalledVersion>() {
            @Override
            public int compare(InstalledVersion a, InstalledVersion b) {
                if (a.tier != b.tier) return a.tier - b.tier;
                return a.id.compareToIgnoreCase(b.id);
            }
        });
        return results;
    }

    // ─── Multi-Version Protocol Bridge (ViaForge / ViaFabricPlus) ─────────────

    public static List<File> listViaJarsInMods(File modsDir) {
        if (modsDir == null || !modsDir.isDirectory()) return Collections.emptyList();
        File[] files = modsDir.listFiles();
        if (files == null) return Collections.emptyList();
        List<File> out = new ArrayList<File>();
        for (File f : files) {
            if (f.isFile()) {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".jar") && (n.contains("viaforge") || n.contains("viafabricplus")
                        || n.contains("viaversion") || n.contains("viabackwards"))) {
                    out.add(f);
                }
            }
        }
        Collections.sort(out, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        return out;
    }

    @SuppressWarnings("unchecked")
    public static InstallResult installViaAddon(File modsDir, boolean modern) {
        String slug = modern ? "viafabricplus" : "viaforge";
        String label = modern ? "ViaFabricPlus" : "ViaForge";
        String apiUrl = "https://api.modrinth.com/v2/project/" + slug + "/version";
        String manualUrl = "https://modrinth.com/mod/" + slug;

        if (modsDir == null) {
            return new InstallResult(false, "No target mods/ directory selected. Manual download: " + manualUrl, null);
        }
        if (!modsDir.exists() && !modsDir.mkdirs()) {
            return new InstallResult(false, "Cannot create mods folder: " + modsDir.getAbsolutePath(), null);
        }

        ensureJava8Tls12();

        String jsonRaw;
        try {
            jsonRaw = httpGetText(apiUrl);
        } catch (IOException e) {
            return new InstallResult(false,
                    "Offline or Modrinth unreachable (" + e.getMessage() + "). Get " + label + " at " + manualUrl,
                    null);
        }

        List<Object> versions = SimpleJson.parseArray(jsonRaw);
        if (versions.isEmpty()) {
            return new InstallResult(false,
                    "Unexpected Modrinth response for " + label + ". Get manually at " + manualUrl,
                    null);
        }

        String chosenUrl = null;
        String chosenFilename = null;

        // Pass 1: exact match (1.8.9 for ViaForge; release channel for ViaFabricPlus)
        for (int pass = 0; pass < 2 && chosenUrl == null; pass++) {
            for (Object item : versions) {
                if (!(item instanceof Map)) continue;
                Map<String, Object> ver = (Map<String, Object>) item;

                if (modern && pass == 0) {
                    Object vt = ver.get("version_type");
                    if (vt instanceof String && !"release".equalsIgnoreCase((String) vt)) {
                        continue;
                    }
                }

                boolean supports189 = false;
                Object gvObj = ver.get("game_versions");
                if (gvObj instanceof List) {
                    for (Object gv : (List<Object>) gvObj) {
                        String s = String.valueOf(gv);
                        if ("1.8.9".equals(s) || (pass == 1 && s.startsWith("1.8"))) {
                            supports189 = true;
                            break;
                        }
                    }
                }

                Object filesObj = ver.get("files");
                if (!(filesObj instanceof List)) continue;
                for (Object fo : (List<Object>) filesObj) {
                    if (!(fo instanceof Map)) continue;
                    Map<String, Object> fileMap = (Map<String, Object>) fo;
                    String fname = fileMap.get("filename") instanceof String ? (String) fileMap.get("filename") : null;
                    String url = fileMap.get("url") instanceof String ? (String) fileMap.get("url") : null;
                    if (fname == null || url == null) continue;
                    String lowerName = fname.toLowerCase(Locale.ROOT);
                    if (!lowerName.endsWith(".jar") || lowerName.endsWith("-sources.jar") || lowerName.endsWith("-dev.jar")) {
                        continue;
                    }

                    if (!modern) {
                        boolean fileMatches189 = lowerName.contains("1.8.9") || supports189;
                        if (pass == 0 && !fileMatches189) continue;
                    }

                    chosenUrl = url;
                    chosenFilename = sanitizeFileName(fname);
                    if (Boolean.TRUE.equals(fileMap.get("primary"))) {
                        break;
                    }
                }
                if (chosenUrl != null) break;
            }
        }

        if (chosenUrl == null || chosenFilename == null) {
            return new InstallResult(false,
                    "No compatible " + label + " JAR found on Modrinth. Manual link: " + manualUrl,
                    null);
        }

        File partFile = new File(modsDir, chosenFilename + ".part");
        File destFile = new File(modsDir, chosenFilename);
        try {
            downloadFile(chosenUrl, partFile);
            if (!partFile.isFile() || partFile.length() < 1024) {
                partFile.delete();
                return new InstallResult(false,
                        "Downloaded " + label + " archive was incomplete. Manual link: " + manualUrl,
                        null);
            }

            // Clean older versions of the same Via addon to prevent loader conflicts
            File[] existing = modsDir.listFiles();
            if (existing != null) {
                for (File f : existing) {
                    if (f.isFile() && !f.getName().equalsIgnoreCase(chosenFilename)) {
                        String n = f.getName().toLowerCase(Locale.ROOT);
                        if (n.endsWith(".jar") && n.contains(slug)) {
                            f.delete();
                        }
                    }
                }
            }

            Files.move(partFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new InstallResult(true,
                    "Installed " + chosenFilename + " (" + (destFile.length() / 1024) + " KB) to mods/",
                    destFile);
        } catch (IOException e) {
            partFile.delete();
            return new InstallResult(false,
                    "Download failed (" + e.getMessage() + "). Get " + label + " manually at " + manualUrl,
                    null);
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static void ensureJava8Tls12() {
        String ver = System.getProperty("java.version", "");
        if (ver.startsWith("1.8")) {
            String existing = System.getProperty("https.protocols", "");
            if (!existing.contains("TLSv1.2")) {
                System.setProperty("https.protocols", "TLSv1.2");
            }
        }
    }

    private static HttpURLConnection openConnectionWithRedirects(String urlStr, String accept) throws IOException {
        String current = urlStr;
        for (int redirects = 0; redirects < 5; redirects++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", USER_AGENT);
            if (accept != null) {
                conn.setRequestProperty("Accept", accept);
            }
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307 || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null || loc.isEmpty()) {
                    throw new IOException("Redirect missing Location header");
                }
                current = new URL(new URL(current), loc).toString();
                continue;
            }
            if (code < 200 || code >= 300) {
                conn.disconnect();
                throw new IOException("HTTP " + code);
            }
            return conn;
        }
        throw new IOException("Too many redirects");
    }

    private static String httpGetText(String urlStr) throws IOException {
        HttpURLConnection conn = openConnectionWithRedirects(urlStr, "application/json");
        InputStream in = null;
        try {
            in = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            conn.disconnect();
        }
    }

    private static void downloadFile(String urlStr, File dest) throws IOException {
        HttpURLConnection conn = openConnectionWithRedirects(urlStr, "application/octet-stream");
        InputStream in = null;
        FileOutputStream out = null;
        try {
            in = conn.getInputStream();
            out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ignored) {}
            }
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            conn.disconnect();
        }
    }

    // ─── Profile Persistence & Live Sync Bridge ───────────────────────────────

    public static File getProfilesDir(File gameDir, String edition) {
        String sub = DesktopCatalog.isModernEdition(edition) ? "Cigarette" : "cigarette";
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
        return loadProfileMap(profilesDir, profileName, DesktopCatalog.isModernEdition(DesktopCatalog.detectEditionFromClasspath()));
    }

    public static Map<String, Object> loadProfileMap(File profilesDir, String profileName, boolean modern) {
        if (profilesDir != null) {
            File f = new File(profilesDir, profileName + ".json");
            if (f.isFile()) {
                try {
                    String raw = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    Map<String, Object> parsed = SimpleJson.parseObject(raw);
                    if (!parsed.isEmpty()) {
                        normalizeModuleAliases(parsed);
                        return parsed;
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return createDefaultProfileMap(profileName, modern);
    }

    public static Map<String, Object> createDefaultProfileMap(String profileName) {
        return createDefaultProfileMap(profileName, DesktopCatalog.isModernEdition(DesktopCatalog.detectEditionFromClasspath()));
    }

    public static Map<String, Object> createDefaultProfileMap(String profileName, boolean modern) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("profile_name", profileName);
        Map<String, Object> modules = new LinkedHashMap<String, Object>();
        for (DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            Map<String, Object> md = new LinkedHashMap<String, Object>();
            md.put("enabled", Boolean.valueOf(def.defaultEnabled));
            md.put("keyCode", Integer.valueOf(def.getDefaultKeyCode(modern)));
            md.put("hidden", Boolean.FALSE);
            md.put("settings", def.createDefaultSettingsMap(modern));
            modules.put(def.name, md);
            if (def.altKey != null) {
                modules.put(def.altKey, copyModuleEntry(md));
            }
        }
        root.put("modules", modules);

        Map<String, Object> ui = new LinkedHashMap<String, Object>();
        ui.put("themePreset", "Electric Blue");
        ui.put("accentColor", Integer.valueOf(DesktopCatalog.getThemeColor("Electric Blue").getRGB()));
        ui.put("guiScale", Double.valueOf(1.0));
        root.put("ui", ui);
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void normalizeModuleAliases(Map<String, Object> profileMap) {
        if (profileMap == null) return;
        Object rawMods = profileMap.get("modules");
        if (!(rawMods instanceof Map)) return;
        Map<String, Object> modules = (Map<String, Object>) rawMods;
        for (DesktopCatalog.ModuleDef def : DesktopCatalog.getModules()) {
            if (def.altKey == null) continue;
            Object primary = modules.get(def.name);
            Object secondary = modules.get(def.altKey);
            if (primary instanceof Map) {
                modules.put(def.altKey, copyModuleEntry((Map<String, Object>) primary));
            } else if (secondary instanceof Map) {
                modules.put(def.name, copyModuleEntry((Map<String, Object>) secondary));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copyModuleEntry(Map<String, Object> src) {
        Map<String, Object> copy = new LinkedHashMap<String, Object>(src);
        Object s = src.get("settings");
        if (s instanceof Map) {
            copy.put("settings", new LinkedHashMap<String, Object>((Map<String, Object>) s));
        }
        return copy;
    }

    public static boolean saveProfileAndSignal(File profilesDir, String profileName, Map<String, Object> profileMap, String action) {
        if (profilesDir == null) return false;
        if (!profilesDir.exists() && !profilesDir.mkdirs()) return false;
        try {
            profileMap.put("profile_name", profileName);
            normalizeModuleAliases(profileMap);
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

    // ─── Mod JAR Management ───────────────────────────────────────────────────

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
            return new InstallResult(true, "Already installed inside " + modsDir.getAbsolutePath(), dest);
        }

        try {
            boolean isModern = DesktopCatalog.isModernEdition(currentEdition);
            for (File existing : listCigaretteJarsInMods(modsDir)) {
                String lower = existing.getName().toLowerCase(Locale.ROOT);
                boolean existingModern = lower.contains("26.3") || lower.contains("fabric");
                if (existingModern == isModern && !existing.getName().equalsIgnoreCase(self.getName())) {
                    existing.delete();
                }
            }
            Files.copy(self.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new InstallResult(true, "Installed " + self.getName() + " to mods/", dest);
        } catch (IOException e) {
            return new InstallResult(false, "Copy failed (" + e.getMessage() + "). Close Minecraft first if locked.", null);
        }
    }

    public static int removeConflictingJars(File modsDir, String keepEdition) {
        boolean keepModern = DesktopCatalog.isModernEdition(keepEdition);
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

    public static void openWebLink(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Throwable ignored) {
        }
    }
}
