package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default values for the "Riso GL9730" tab.
 */
public final class RisoTabDefaults implements TabDefaults {

    private static final RisoTabDefaults INSTANCE = new RisoTabDefaults();

    private final Map<String, Object> defaults;

    private RisoTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("optimizationEnabled", Boolean.FALSE);
        values.put("recordId", "REC-");
        defaults = Collections.unmodifiableMap(values);
    }

    public static RisoTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Riso GL9730";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}
