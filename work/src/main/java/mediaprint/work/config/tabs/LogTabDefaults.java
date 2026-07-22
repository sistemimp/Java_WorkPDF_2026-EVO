package mediaprint.work.config.tabs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default values for the "Log" tab (console + barra di avanzamento).
 */
public final class LogTabDefaults implements TabDefaults {

    private static final LogTabDefaults INSTANCE = new LogTabDefaults();

    private final Map<String, Object> defaults;

    private LogTabDefaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("rows", 10);
        values.put("editable", Boolean.FALSE);
        values.put("lineWrap", Boolean.TRUE);
        values.put("wrapStyleWord", Boolean.TRUE);
        values.put("progressMin", 0);
        values.put("progressMax", 100);
        values.put("initialProgressValue", 0);
        defaults = Collections.unmodifiableMap(values);
    }

    public static LogTabDefaults getInstance() {
        return INSTANCE;
    }

    @Override
    public String tabName() {
        return "Log";
    }

    @Override
    public Map<String, Object> parameters() {
        return defaults;
    }
}

