/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sca.ScaContext;
import sca.config.YamlScaRulesLoader;

class ScaContextUnitTest {

    @Test
    @DisplayName(
            "What: ScaContext | When: instantiated with rules | Then: properties return configured"
                    + " settings")
    void scaContext_whenInstantiated_thenReturnsConfiguredSettings() {
        // What: ScaContext configured with custom flags
        ScaContext context = new ScaContext("camel case", true, true);

        // When & Then: verify properties
        assertEquals("camel case", context.identifierFormat());
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertTrue(context.mandatoryLiteralOrIdentifierInReadInput());

        // Default constructor
        ScaContext defaultCtx = new ScaContext();
        assertFalse(defaultCtx.mandatoryLiteralOrIdentifierInPrintln());
        assertFalse(defaultCtx.mandatoryLiteralOrIdentifierInReadInput());
    }

    @Test
    @DisplayName(
            "What: YamlScaRulesLoader | When: loading YAML input stream | Then: populates"
                    + " ScaContext correctly")
    void yamlRulesLoader_whenLoadingYaml_thenPopulatesScaContext() {
        // What: YAML config string
        String yaml =
                """
                identifier_format: "snake case"
                mandatory-variable-or-literal-in-println: true
                mandatory-variable-or-literal-in-readInput: false
                """;

        // When: loading ScaContext from YAML
        ScaContext context =
                YamlScaRulesLoader.loadFromYaml(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        // Then: context attributes reflect YAML configuration
        assertNotNull(context);
        assertEquals("snake case", context.identifierFormat());
        assertTrue(context.mandatoryLiteralOrIdentifierInPrintln());
        assertFalse(context.mandatoryLiteralOrIdentifierInReadInput());
    }
}
