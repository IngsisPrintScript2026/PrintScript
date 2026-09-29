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
        Boolean spaceBeforeColon = parseSpaceBeforeColon(data);
        Boolean spaceAfterColon = parseSpaceAfterColon(data);
        Boolean spaceAroundEquals = parseSpaceAroundEquals(data);
        Boolean spaceAroundOps = parseSpaceAroundOps(data);
        return createLoadedContext(
                data, spaceBeforeColon, spaceAfterColon, spaceAroundEquals, spaceAroundOps);
    }

    private static Boolean parseSpaceBeforeColon(Map<String, Object> data) {
        return getOptionalBoolean(
                data,
                "space-before-colon",
                "spaceBeforeColon",
                "enforce-spacing-before-colon-in-declaration",
                "enforceSpacingBeforeColonInDeclaration");
    }

    private static Boolean parseSpaceAfterColon(Map<String, Object> data) {
        return getOptionalBoolean(
                data,
                "space-after-colon",
                "spaceAfterColon",
                "enforce-spacing-after-colon-in-declaration",
                "enforceSpacingAfterColonInDeclaration");
    }

    private static Boolean parseSpaceAroundOps(Map<String, Object> data) {
        return getOptionalBoolean(
                data,
                "space-around-operators",
                "spaceAroundOperators",
                "mandatory-space-surrounding-operations",
                "mandatorySpaceSurroundingOperations");
    }

    private static Boolean parseSpaceAroundEquals(Map<String, Object> data) {
        if (hasKey(data, "enforce-no-spacing-around-equals", "enforceNoSpacingAroundEquals")) {
            return !getBoolean(
                    data,
                    false,
                    "enforce-no-spacing-around-equals",
                    "enforceNoSpacingAroundEquals");
        }
        String[] keys = {
            "enforce-spacing-around-equals",
            "enforceSpacingAroundEquals",
            "space-around-equals",
            "spaceAroundEquals"
        };
        return hasKey(data, keys) ? getBoolean(data, true, keys) : null;
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
            Boolean belowLine,
            Boolean spaceAfterComma) {}

    private static FormattingRulesData extractRulesData(Map<String, Object> data) {
        return new FormattingRulesData(
                getOptionalInt(
                        data,
                        "indent-inside-if",
                        "indentInsideIf",
                        "indent-spaces",
                        "indentSpaces",
                        "indent-size",
                        "indentSize"),
                getLineBreak(data),
                getPrintlnBreaks(data),
                getSingleSpace(data),
                getOptionalBoolean(data, "if-brace-same-line", "ifBraceSameLine"),
                getOptionalBoolean(data, "if-brace-below-line", "ifBraceBelowLine"),
                getSpaceAfterComma(data));
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
                r.belowLine(),
                r.spaceAfterComma());
    }

    private static Boolean getSpaceAfterComma(Map<String, Object> data) {
        return getOptionalBoolean(
                data,
                "space-after-comma",
                "spaceAfterComma",
                "enforce-spacing-after-comma-in-arguments",
                "enforceSpacingAfterCommaInArguments");
    }

    private static Boolean getLineBreak(Map<String, Object> data) {
        return getOptionalBoolean(
                data,
                "line-break-after-statement",
                "lineBreakAfterStatement",
                "mandatory-line-break-after-statement",
                "mandatoryLineBreakAfterStatement");
    }

    private static Integer getPrintlnBreaks(Map<String, Object> data) {
        return getOptionalInt(
                data,
                "line-breaks-after-println",
                "lineBreaksAfterPrintln",
                "line-breaks-before-println",
                "lineBreaksBeforePrintln");
    }

    private static Boolean getSingleSpace(Map<String, Object> data) {
        return getOptionalBoolean(
                data,
                "mandatory-single-space-separation",
                "mandatorySingleSpaceSeparation",
                "single-space-separation",
                "singleSpaceSeparation");
    }

    private static boolean hasKey(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (k != null && !k.isEmpty() && map.containsKey(k)) return true;
        }
        return false;
    }

    private static Boolean getOptionalBoolean(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (k != null && !k.isEmpty() && map.containsKey(k)) {
                return parseBoolean(map.get(k), false);
            }
        }
        return null;
    }

    private static Integer getOptionalInt(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            if (k != null && !k.isEmpty() && map.containsKey(k)) {
                return parseInt(map.get(k), 0);
            }
        }
        return null;
    }

    private static boolean getBoolean(
            Map<String, Object> map, boolean defaultValue, String... keys) {
        Boolean b = getOptionalBoolean(map, keys);
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
