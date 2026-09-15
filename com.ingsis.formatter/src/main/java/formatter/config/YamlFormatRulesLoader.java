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
            return data != null ? parseContext(data) : new FormatContext();
        } catch (Exception e) {
            return new FormatContext();
        }
    }

    private static FormatContext parseContext(Map<String, Object> data) {
        Boolean spaceBeforeColon =
                getOptionalBoolean(
                        data, "space-before-colon", "enforce-spacing-before-colon-in-declaration");
        Boolean spaceAfterColon =
                getOptionalBoolean(
                        data, "space-after-colon", "enforce-spacing-after-colon-in-declaration");
        Boolean spaceAroundEquals = parseSpaceAroundEquals(data);
        Boolean spaceAroundOps =
                getOptionalBoolean(
                        data, "space-around-operators", "mandatory-space-surrounding-operations");
        return createLoadedContext(
                data, spaceBeforeColon, spaceAfterColon, spaceAroundEquals, spaceAroundOps);
    }

    private static Boolean parseSpaceAroundEquals(Map<String, Object> data) {
        if (hasKey(data, "enforce-no-spacing-around-equals")) {
            return !getBoolean(data, "enforce-no-spacing-around-equals", "", false);
        }
        if (hasKey(data, "enforce-spacing-around-equals", "space-around-equals")) {
            return getBoolean(data, "enforce-spacing-around-equals", "space-around-equals", true);
        }
        return null;
    }

    private static FormatContext createLoadedContext(
            Map<String, Object> data,
            Boolean beforeColon,
            Boolean afterColon,
            Boolean aroundEquals,
            Boolean aroundOps) {
        FormattingRulesData r = extractRulesData(data);
        return assembleContext(beforeColon, afterColon, aroundEquals, aroundOps, r);
    }

    private record FormattingRulesData(
            Integer indent,
            Boolean lineBreak,
            Integer printlnBreaks,
            Boolean singleSpace,
            Boolean sameLine,
            Boolean belowLine) {}

    private static FormattingRulesData extractRulesData(Map<String, Object> data) {
        return new FormattingRulesData(
                getOptionalInt(data, "indent-inside-if", "indent-spaces"),
                getLineBreak(data),
                getPrintlnBreaks(data),
                getSingleSpace(data),
                getOptionalBoolean(data, "if-brace-same-line", ""),
                getOptionalBoolean(data, "if-brace-below-line", ""));
    }

    private static FormatContext assembleContext(
            Boolean before, Boolean after, Boolean equals, Boolean ops, FormattingRulesData r) {
        return new FormatContext(
                0,
                r.indent(),
                before,
                after,
                equals,
                ops,
                r.lineBreak(),
                r.printlnBreaks(),
                r.singleSpace(),
                r.sameLine(),
                r.belowLine());
    }

    private static Boolean getLineBreak(Map<String, Object> data) {
        return getOptionalBoolean(
                data, "line-break-after-statement", "mandatory-line-break-after-statement");
    }

    private static Integer getPrintlnBreaks(Map<String, Object> data) {
        return getOptionalInt(data, "line-breaks-after-println", "line-breaks-before-println");
    }

    private static Boolean getSingleSpace(Map<String, Object> data) {
        return getOptionalBoolean(
                data, "mandatory-single-space-separation", "single-space-separation");
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
