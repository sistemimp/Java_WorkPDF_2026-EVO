package mediaprint.work;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

public final class AppLauncher {
        private AppLauncher() {
        }

        public static void main(String[] args) {
                forceUtf8DefaultCharset();
                App.main(args);
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
}
