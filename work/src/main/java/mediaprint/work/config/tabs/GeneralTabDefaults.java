package mediaprint.work.config.tabs;

import com.itextpdf.kernel.pdf.PdfVersion;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default values for the "Generale" tab (file input/output, opzioni, etichetta
 * e resize).
 */
public final class GeneralTabDefaults implements TabDefaults {

    private static final GeneralTabDefaults INSTANCE = new GeneralTabDefaults();

    private final Map<String, Object> defaults;

    private GeneralTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("pdfInputPath", "");
        values.put("pdfOutputPath", "");
        values.put("marker", "");
        values.put("pdfVersion", PdfVersion.PDF_1_7);
        values.put("ignoreCase", Boolean.FALSE);
        values.put("normalizeAccents", Boolean.TRUE);
        values.put("rotateByTextEnabled", Boolean.FALSE);
        values.put("rotateApplyResizeOnMatchedPages", Boolean.TRUE);
        values.put("rotateByTextString", "");
        values.put("rotateByTextDegrees", "90");

        values.put("resizeEnabled", Boolean.TRUE);
        values.put("resizeForceA4BeforeResize", Boolean.FALSE);
        values.put("scalePercent", 97);
        values.put("offsetXmm", 5);
        values.put("offsetYmm", 5);

        values.put("groupLabelEnabled", Boolean.TRUE);
        values.put("labelIdentifier", "");
        values.put("labelXmm", 10);
        values.put("labelYmm", 270);
        values.put("labelOrientation", LabelOrientation.VERTICAL);

        defaults = Collections.unmodifiableMap(values);
    }

    public static GeneralTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Generale";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }

    public enum LabelOrientation {
        HORIZONTAL,
        VERTICAL
    }
}
