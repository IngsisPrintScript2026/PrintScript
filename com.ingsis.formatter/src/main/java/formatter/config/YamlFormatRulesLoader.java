package formatter.config;

import formatter.FormatContext;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class YamlFormatRulesLoader {

    public static FormatContext loadFromYaml(InputStream yamlStream) {
        if (yamlStream == null) {
            return new FormatContext();
        }
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(yamlStream);
            if (data == null) {
                return new FormatContext();
            }

            boolean spaceBeforeColon = getBoolean(data, "space-before-colon", "enforce-spacing-before-colon-in-declaration", false);
            boolean spaceAfterColon = getBoolean(data, "space-after-colon", "enforce-spacing-after-colon-in-declaration", true);

            boolean spaceAroundEquals;
            if (data.containsKey("enforce-no-spacing-around-equals") && getBoolean(data, "enforce-no-spacing-around-equals", "", false)) {
                spaceAroundEquals = false;
            } else {
                spaceAroundEquals = getBoolean(data, "space-around-equals", "enforce-spacing-around-equals", true);
            }

            boolean spaceAroundOperators = getBoolean(data, "space-around-operators", "mandatory-space-surrounding-operations", true);
            int lineBreaksAfterPrintln = getInt(data, "line-breaks-after-println", "line-breaks-before-println", 1);
            int indentSpaces = getInt(data, "indent-inside-if", "indent-spaces", 4);

            boolean ifBraceSameLine = getBoolean(data, "if-brace-same-line", "if-brace-same-line", true);
            if (data.containsKey("if-brace-below-line") && getBoolean(data, "if-brace-below-line", "", false)) {
                ifBraceSameLine = false;
            }

            return new FormatContext(
                    0,
                    indentSpaces,
                    spaceBeforeColon,
                    spaceAfterColon,
                    spaceAroundEquals,
                    spaceAroundOperators,
                    lineBreaksAfterPrintln,
                    ifBraceSameLine);
        } catch (Exception e) {
            return new FormatContext();
        }
    }

    private static boolean getBoolean(Map<String, Object> map, String key1, String key2, boolean defaultValue) {
        if (map.containsKey(key1)) {
            return parseBoolean(map.get(key1), defaultValue);
        }
        if (key2 != null && !key2.isEmpty() && map.containsKey(key2)) {
            return parseBoolean(map.get(key2), defaultValue);
        }
        return defaultValue;
    }

    private static boolean parseBoolean(Object val, boolean defaultValue) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private static int getInt(Map<String, Object> map, String key1, String key2, int defaultValue) {
        if (map.containsKey(key1)) {
            return parseInt(map.get(key1), defaultValue);
        }
        if (key2 != null && !key2.isEmpty() && map.containsKey(key2)) {
            return parseInt(map.get(key2), defaultValue);
        }
        return defaultValue;
    }

    private static int parseInt(Object val, int defaultValue) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}
