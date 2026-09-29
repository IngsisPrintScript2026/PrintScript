/*
 * My Project
 */

package sca.config;

import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import sca.ScaContext;

public class YamlScaRulesLoader {

    public static ScaContext loadFromYaml(InputStream yamlStream) {
        if (yamlStream == null) {
            return new ScaContext();
        }
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(yamlStream);
            return data != null ? parseContext(data) : new ScaContext();
        } catch (Exception e) {
            return new ScaContext();
        }
    }

    private static ScaContext parseContext(Map<String, Object> data) {
        String identifierFormat =
                getString(data, null, "identifier_format", "identifier-format", "identifierFormat");
        boolean mandatoryPrintln = hasMandatoryPrintln(data);
        boolean mandatoryReadInput = hasMandatoryReadInput(data);
        return new ScaContext(identifierFormat, mandatoryPrintln, mandatoryReadInput);
    }

    private static boolean hasMandatoryPrintln(Map<String, Object> data) {
        return getBoolean(
                data,
                false,
                "mandatory-variable-or-literal-in-println",
                "mandatoryVariableOrLiteralInPrintln",
                "mandatory_variable_or_literal_in_println");
    }

    private static boolean hasMandatoryReadInput(Map<String, Object> data) {
        return getBoolean(
                data,
                false,
                "mandatory-variable-or-literal-in-readInput",
                "mandatory-variable-or-literal-in-readinput",
                "mandatoryVariableOrLiteralInReadInput",
                "mandatory_variable_or_literal_in_read_input");
    }

    private static String getString(Map<String, Object> map, String defaultValue, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k) && map.get(k) != null) {
                return map.get(k).toString();
            }
        }
        return defaultValue;
    }

    private static boolean getBoolean(
            Map<String, Object> map, boolean defaultValue, String... keys) {
        for (String k : keys) {
            if (map.containsKey(k)) {
                return parseBoolean(map.get(k), defaultValue);
            }
        }
        return defaultValue;
    }

    private static boolean parseBoolean(Object val, boolean defaultValue) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }
}
