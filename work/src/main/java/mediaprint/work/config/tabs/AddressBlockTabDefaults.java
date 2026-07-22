package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default values for the "Blocco indirizzo" tab.
 */
public final class AddressBlockTabDefaults implements TabDefaults {

    private static final AddressBlockTabDefaults INSTANCE = new AddressBlockTabDefaults();

    private final Map<String, Object> defaults;

    private AddressBlockTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("addressBlockEnabled", Boolean.FALSE);
        values.put("positionXmm", 110f);
        values.put("positionYmm", 220f);
        values.put("widthMm", 150f);
        values.put("heightMm", 20f);
        values.put("keyReadEnabled", Boolean.FALSE);
        values.put("keyString", "id:");
        defaults = Collections.unmodifiableMap(values);
    }

    public static AddressBlockTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Blocco indirizzo";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}
