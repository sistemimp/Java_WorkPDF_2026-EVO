package mediaprint.work.config.tabs;

import java.util.Map;

/**
 * Contract for exposing the default parameters attached to each UI tab.
 */
public interface TabDefaults {

    /**
     * @return localized name of the tab as displayed in the UI.
     */
    String tabName();

    /**
     * @return immutable map of parameter identifiers and their default values.
     */
    Map<String, Object> parameters();

    /**
     * Typed accessor with a simple cast helper.
     */
    default <T> T getValue(String key, Class<T> type) {
        Object value = parameters().get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    default String getString(String key) {
        Object value = parameters().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    default boolean getBoolean(String key) {
        Object value = parameters().get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    default float getFloat(String key) {
        Object value = parameters().get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value != null) {
            try {
                return Float.parseFloat(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0f;
    }

    default int getInt(String key) {
        Object value = parameters().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    default long getLong(String key) {
        Object value = parameters().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }
}
