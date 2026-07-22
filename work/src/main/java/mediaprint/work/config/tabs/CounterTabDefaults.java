package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default values for the "Contatori" tab (page counter overlay).
 */
public final class CounterTabDefaults implements TabDefaults {

    private static final CounterTabDefaults INSTANCE = new CounterTabDefaults();

    private final Map<String, Object> defaults;

    private CounterTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("pageCounterEnabled", Boolean.TRUE);
        values.put("orientation", Orientation.VERTICAL);
        values.put("positionXmm", 5f);
        values.put("positionYmm", 35f);
        values.put("fontSizePt", 6f);
        defaults = Collections.unmodifiableMap(values);
    }

    public static CounterTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Contatori";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
}
