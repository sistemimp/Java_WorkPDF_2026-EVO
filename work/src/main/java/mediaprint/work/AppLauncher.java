package mediaprint.work;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.Charset;

public final class AppLauncher {
        private static final String HEAP_CONFIGURED_PROPERTY = "workpdf.heap.configured";
        static final String LAUNCHER_ACTIVE_PROPERTY = "workpdf.launcher.active";
        private static final long MEBIBYTE = 1024L * 1024L;
        private static final long GIBIBYTE = 1024L * MEBIBYTE;
        private static final long MIN_HEAP_BYTES = 1024L * MEBIBYTE;
        private static final long CURRENT_HEAP_TOLERANCE_BYTES = 128L * MEBIBYTE;

        private AppLauncher() {
        }

        public static void main(String[] args) {
                forceUtf8DefaultCharset();
                configureGraphicsProperties();
                App.installGlobalExceptionHandler();
                if (shouldRestartWithCalculatedHeap()) {
                        restartWithCalculatedHeap(args);
                        return;
                }
                System.setProperty(LAUNCHER_ACTIVE_PROPERTY, "true");
                App.main(args);
        }

        private static boolean shouldRestartWithCalculatedHeap() {
                if (Boolean.getBoolean(HEAP_CONFIGURED_PROPERTY)) {
                        return false;
                }
                if (!Files.isRegularFile(javaExecutable())) {
                        return false;
                }
                long targetHeapBytes = calculateHeapLimitBytes(getInstalledMemoryBytes());
                long currentHeapBytes = Runtime.getRuntime().maxMemory();
                return targetHeapBytes > currentHeapBytes + CURRENT_HEAP_TOLERANCE_BYTES;
        }

        private static void restartWithCalculatedHeap(String[] args) {
                long targetHeapBytes = calculateHeapLimitBytes(getInstalledMemoryBytes());
                List<String> command = new ArrayList<>();
                command.add(javaExecutable().toString());
                command.add("-Xmx" + toMegabytes(targetHeapBytes) + "m");
                command.add("-XX:+UseG1GC");
                command.add("-XX:MinHeapFreeRatio=5");
                command.add("-XX:MaxHeapFreeRatio=20");
                command.add("-D" + HEAP_CONFIGURED_PROPERTY + "=true");
                command.add("-D" + LAUNCHER_ACTIVE_PROPERTY + "=true");
                command.add("-Dfile.encoding=UTF-8");
                command.add("-Dnative.encoding=UTF-8");
                command.add("-Dsun.jnu.encoding=UTF-8");
                command.add("-Dsun.java2d.d3d=false");
                command.add("-Dsun.java2d.noddraw=true");
                appendLaunchTarget(command);
                command.addAll(Arrays.asList(args));

                try {
                        Process process = new ProcessBuilder(command)
                                .inheritIO()
                                .start();
                        int exitCode = process.waitFor();
                        System.exit(exitCode);
                } catch (Exception e) {
                        System.err.println("Impossibile riavviare WorkPDF con limite heap automatico: " + e.getMessage());
                        System.err.println("Avvio con la memoria JVM corrente.");
                }
        }

        static long calculateHeapLimitBytes(long installedMemoryBytes) {
                if (installedMemoryBytes <= 0L) {
                        return 2L * GIBIBYTE;
                }

                long percentage;
                if (installedMemoryBytes <= 4L * GIBIBYTE) {
                        percentage = 50L;
                } else if (installedMemoryBytes <= 8L * GIBIBYTE) {
                        percentage = 60L;
                } else if (installedMemoryBytes <= 16L * GIBIBYTE) {
                        percentage = 70L;
                } else {
                        percentage = 75L;
                }

                long byPercentage = installedMemoryBytes * percentage / 100L;
                long reservedForSystem = installedMemoryBytes - Math.max(2L * GIBIBYTE, installedMemoryBytes / 4L);
                return Math.max(MIN_HEAP_BYTES, Math.min(byPercentage, reservedForSystem));
        }

        private static long getInstalledMemoryBytes() {
                java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
                if (bean instanceof com.sun.management.OperatingSystemMXBean) {
                        return ((com.sun.management.OperatingSystemMXBean) bean).getTotalPhysicalMemorySize();
                }
                return -1L;
        }

        private static long toMegabytes(long bytes) {
                return Math.max(1L, bytes / MEBIBYTE);
        }

        private static Path javaExecutable() {
                return Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
        }

        private static boolean isWindows() {
                return System.getProperty("os.name", "").toLowerCase().contains("win");
        }

        private static void appendLaunchTarget(List<String> command) {
                Path sourcePath = getLauncherSourcePath();
                if (sourcePath != null && Files.isRegularFile(sourcePath)
                        && sourcePath.getFileName().toString().toLowerCase().endsWith(".jar")) {
                        command.add("-jar");
                        command.add(sourcePath.toString());
                        return;
                }

                command.add("-cp");
                command.add(System.getProperty("java.class.path"));
                command.add(AppLauncher.class.getName());
        }

        private static Path getLauncherSourcePath() {
                CodeSource codeSource = AppLauncher.class.getProtectionDomain().getCodeSource();
                if (codeSource == null || codeSource.getLocation() == null) {
                        return null;
                }
                try {
                        return new File(codeSource.getLocation().toURI()).toPath();
                } catch (URISyntaxException | IllegalArgumentException e) {
                        return null;
                }
        }

        private static void forceUtf8DefaultCharset() {
                System.setProperty("file.encoding", "UTF-8");
                System.setProperty("native.encoding", "UTF-8");
                System.setProperty("sun.jnu.encoding", "UTF-8");
                try {
                        Field defaultCharset = Charset.class.getDeclaredField("defaultCharset");
                        defaultCharset.setAccessible(true);
                        defaultCharset.set(null, null);
                } catch (Exception ignored) {
                }
        }

        static void configureGraphicsProperties() {
                System.setProperty("sun.java2d.d3d", "false");
                System.setProperty("sun.java2d.noddraw", "true");
        }
}
