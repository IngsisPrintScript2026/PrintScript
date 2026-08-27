/*
 * My Project
 */

package formatter.config;

import formatter.FormatContext;
import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

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

            Boolean spaceBeforeColon =
                    getOptionalBoolean(
                            data,
                            "space-before-colon",
                            "enforce-spacing-before-colon-in-declaration");
            Boolean spaceAfterColon =
                    getOptionalBoolean(
                            data,
                            "space-after-colon",
                            "enforce-spacing-after-colon-in-declaration");

            Boolean spaceAroundEquals = null;
            if (hasKey(data, "enforce-no-spacing-around-equals")) {
                spaceAroundEquals =
                        !getBoolean(data, "enforce-no-spacing-around-equals", "", false);
            } else if (hasKey(data, "enforce-spacing-around-equals", "space-around-equals")) {
                spaceAroundEquals =
                        getBoolean(
                                data, "enforce-spacing-around-equals", "space-around-equals", true);
            }

            Boolean spaceAroundOperators =
                    getOptionalBoolean(
                            data,
                            "space-around-operators",
                            "mandatory-space-surrounding-operations");
            Boolean lineBreakAfterStatement =
                    getOptionalBoolean(
                            data,
                            "line-break-after-statement",
                            "mandatory-line-break-after-statement");
            Integer lineBreaksAfterPrintln =
                    getOptionalInt(data, "line-breaks-after-println", "line-breaks-before-println");
            Boolean singleSpaceSeparation =
                    getOptionalBoolean(
                            data, "mandatory-single-space-separation", "single-space-separation");
            Integer indentSpaces = getOptionalInt(data, "indent-inside-if", "indent-spaces");

            Boolean ifBraceSameLine = getOptionalBoolean(data, "if-brace-same-line", "");
            Boolean ifBraceBelowLine = getOptionalBoolean(data, "if-brace-below-line", "");

            return new FormatContext(
                    0,
                    indentSpaces,
                    spaceBeforeColon,
                    spaceAfterColon,
                    spaceAroundEquals,
                    spaceAroundOperators,
                    lineBreakAfterStatement,
                    lineBreaksAfterPrintln,
                    singleSpaceSeparation,
                    ifBraceSameLine,
                    ifBraceBelowLine);
        } catch (Exception e) {
            return new FormatContext();
        }
    }

    private static boolean hasKey(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (k != null && !k.isEmpty() && map.containsKey(k)) return true;
        }
        return false;
    }

    private static Boolean getOptionalBoolean(Map<String, Object> map, String key1, String key2) {
        if (map.containsKey(key1)) return parseBoolean(map.get(key1), false);
        if (key2 != null && !key2.isEmpty() && map.containsKey(key2))
            return parseBoolean(map.get(key2), false);
        return null;
    }

    private static Integer getOptionalInt(Map<String, Object> map, String key1, String key2) {
        if (map.containsKey(key1)) return parseInt(map.get(key1), 0);
        if (key2 != null && !key2.isEmpty() && map.containsKey(key2))
            return parseInt(map.get(key2), 0);
        return null;
    }

    private static boolean getBoolean(
            Map<String, Object> map, String key1, String key2, boolean defaultValue) {
        Boolean b = getOptionalBoolean(map, key1, key2);
        return b != null ? b : defaultValue;
    }

    private static boolean parseBoolean(Object val, boolean defaultValue) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }

    private static int parseInt(Object val, int defaultValue) {
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
