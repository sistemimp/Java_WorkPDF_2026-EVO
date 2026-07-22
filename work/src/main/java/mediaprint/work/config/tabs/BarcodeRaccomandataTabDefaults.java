package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import mediaprint.imbustatrice.Imbustatrice;

/**
 * Default values for the "Barcode Raccomandata" tab (preset AR caricata all'avvio).
 */
public final class BarcodeRaccomandataTabDefaults implements TabDefaults {

    private static final BarcodeRaccomandataTabDefaults INSTANCE = new BarcodeRaccomandataTabDefaults();

    private final Map<String, Object> defaults;

    private BarcodeRaccomandataTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("barcodeEnabled", Boolean.FALSE);
        values.put("presetKey", "AR");
        values.put("positionXmm", 30f);
        values.put("positionYmm", 225f);
        values.put("barcodeHeightMm", Imbustatrice.RaccomandataStandard.BAR_HEIGHT_MM);
        values.put("moduleWidthMm", Imbustatrice.RaccomandataStandard.NARROW_MODULE_MM);
        values.put("humanReadableFontPt", Imbustatrice.RaccomandataStandard.HUMAN_READABLE_FONT_PT);
        values.put("humanReadableGapMm", Imbustatrice.RaccomandataStandard.HUMAN_READABLE_GAP_MM);
        values.put("prefix", "R");
        values.put("prefixGapMm", Imbustatrice.RaccomandataStandard.PREFIX_GAP_MM);
        values.put("prefixFontPt", Imbustatrice.RaccomandataStandard.PREFIX_FONT_PT);
        values.put("startCode", "61000000001");
        values.put("identifierDigits", Imbustatrice.RaccomandataStandard.IDENTIFIER_DIGITS);
        defaults = Collections.unmodifiableMap(values);
    }

    public static BarcodeRaccomandataTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Barcode Raccomandata";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}
