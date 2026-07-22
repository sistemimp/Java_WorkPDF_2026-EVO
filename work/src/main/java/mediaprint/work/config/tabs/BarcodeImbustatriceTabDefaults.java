package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import mediaprint.imbustatrice.Imbustatrice;

/**
 * Default values for the "Barcode Imbustatrice" tab.
 */
public final class BarcodeImbustatriceTabDefaults implements TabDefaults {

    private static final BarcodeImbustatriceTabDefaults INSTANCE = new BarcodeImbustatriceTabDefaults();

    private final Map<String, Object> defaults;

    private BarcodeImbustatriceTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("barcodeEnabled", Boolean.TRUE);
        values.put("useOmr", Boolean.FALSE);
        values.put("allegatiPresenti", Boolean.FALSE);
        values.put("barcodeOnAttachment", Boolean.FALSE);
        values.put("positionXmm", 6f);
        values.put("positionYmm", 100f);
        values.put("moduleWidthPt", Imbustatrice.BarcodeStandard.MODULE_WIDTH_PT);
        values.put("barHeightPt", Imbustatrice.BarcodeStandard.BAR_HEIGHT_PT);
        values.put("fontSizePt", Imbustatrice.BarcodeStandard.FONT_SIZE_PT);
        values.put("rotationDegrees", Imbustatrice.BarcodeStandard.ROTATION_DEG);
        values.put("verticalOffsetPt", Imbustatrice.BarcodeStandard.Y_OFFSET_PT);
        values.put("startProgressive", 1);
        values.put("labelFontSizePt", Imbustatrice.BarcodeStandard.LABEL_FONT_SIZE_PT);
        defaults = Collections.unmodifiableMap(values);
    }

    public static BarcodeImbustatriceTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Barcode Imbustatrice";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}
