package mediaprint.work;

import junit.framework.TestCase;

public class AppLauncherTest extends TestCase {
        private static final long GB = 1024L * 1024L * 1024L;

        public void testHeapLimitForSmallPcKeepsSystemMemoryFree() {
                assertEquals(2L * GB, AppLauncher.calculateHeapLimitBytes(4L * GB));
        }

        public void testHeapLimitForMediumPcUsesConfiguredPercentage() {
                assertEquals(8L * GB * 60L / 100L, AppLauncher.calculateHeapLimitBytes(8L * GB));
        }

        public void testHeapLimitForLargePcUsesMostRamButLeavesReserve() {
                assertEquals(24L * GB, AppLauncher.calculateHeapLimitBytes(32L * GB));
        }

        public void testHeapLimitFallbackWhenPhysicalMemoryIsUnavailable() {
                assertEquals(2L * GB, AppLauncher.calculateHeapLimitBytes(-1L));
        }
}
