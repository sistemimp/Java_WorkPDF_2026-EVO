package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import mediaprint.imbustatrice.Imbustatrice;

/**
 * Default values for the "Omologazione Postale" tab (sincronizzati col preset
 * AR).
 */
public final class OmologazionePostaleTabDefaults implements TabDefaults {

    private static final OmologazionePostaleTabDefaults INSTANCE = new OmologazionePostaleTabDefaults();

    private final Map<String, Object> defaults;

    private OmologazionePostaleTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("omologazioneEnabled", Boolean.TRUE);
        values.put("presetKey", "Raccomandata (DCOCC0015)");
        values.put("codice", "DCOCC0015");
        values.put("positionXmm", 110f);
        values.put("positionYmm", 245f);
        values.put("fontHeightPt", Imbustatrice.RaccomandataStandard.OMOLOGAZIONE_FONT_PT);
        defaults = Collections.unmodifiableMap(values);
    }

    public static OmologazionePostaleTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Omologazione Postale";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}
