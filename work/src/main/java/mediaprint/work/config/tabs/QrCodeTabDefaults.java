package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Default values for the "QR Code" tab.
 */
public final class QrCodeTabDefaults implements TabDefaults {

    private static final QrCodeTabDefaults INSTANCE = new QrCodeTabDefaults();

    private final Map<String, Object> defaults;

    private QrCodeTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("qrEnabled", Boolean.FALSE);
        values.put("base", "acquisisci da sito postanetwork");
        values.put("digits", 5);
        values.put("startValue", 1L);
        values.put("example", example("acquisisci da sito postanetwork", 5, 1L));
        values.put("sizeMm", 8f);
        values.put("positionXmm", 170f);
        values.put("positionYmm", 240f);
        values.put("errorCorrectionLevel", "M");
        values.put("correctionsEnabled", Boolean.FALSE);
        values.put("correctionsExcelPath", "");
        values.put("correctionsXmm", 20f);
        values.put("correctionsYmm", 250f);
        values.put("correctionsWidthMm", 50f);
        values.put("correctionsFontPt", 4.5f);
        values.put("correctionsIconMm", 5f);
        defaults = Collections.unmodifiableMap(values);
    }

    public static QrCodeTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "QR Code";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }

    private static String example(String base, int digits, long start) {
        String pattern = "%0" + Math.max(1, digits) + "d";
        String progressive = String.format(Locale.US, pattern, Math.max(0L, start));
        return base + progressive;
    }
}
