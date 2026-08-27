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
            if (data == null) {
                return new ScaContext();
            }

            String identifierFormat =
                    getString(data, "identifier_format", "identifier-format", null);
            boolean mandatoryPrintln =
                    getBoolean(
                            data,
                            "mandatory-variable-or-literal-in-println",
                            "mandatory-variable-or-literal-in-println",
                            false);
            boolean mandatoryReadInput =
                    getBoolean(
                            data,
                            "mandatory-variable-or-literal-in-readInput",
                            "mandatory-variable-or-literal-in-readinput",
                            false);

            return new ScaContext(identifierFormat, mandatoryPrintln, mandatoryReadInput);
        } catch (Exception e) {
            return new ScaContext();
        }
    }

    private static String getString(
            Map<String, Object> map, String key1, String key2, String defaultValue) {
        if (map.containsKey(key1) && map.get(key1) != null) {
            return map.get(key1).toString();
        }
        if (key2 != null && map.containsKey(key2) && map.get(key2) != null) {
            return map.get(key2).toString();
        }
        return defaultValue;
    }

    private static boolean getBoolean(
            Map<String, Object> map, String key1, String key2, boolean defaultValue) {
        if (map.containsKey(key1)) {
            return parseBoolean(map.get(key1), defaultValue);
        }
        if (key2 != null && map.containsKey(key2)) {
            return parseBoolean(map.get(key2), defaultValue);
        }
        return defaultValue;
    }

    private static boolean parseBoolean(Object val, boolean defaultValue) {
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return defaultValue;
    }
}
