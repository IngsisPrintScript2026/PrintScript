/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.keyword.AssignNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import sca.ASTSca;
import sca.ScaContext;
import semantic.environment.SemanticEnvironment;

class ASTScaUnitTest {

    @Test
    @DisplayName(
            "What: ASTSca | When: analyzing null nodes or programs | Then: returns failure result")
    void astSca_whenAnalyzingNullInput_thenReturnsFailure() {
        // What: default ASTSca
        ASTSca defaultSca = new ASTSca();

        // When & Then: null program and null node return failure
        assertFalse(defaultSca.analyze(null, new SemanticEnvironment()).isCorrect());
        assertFalse(defaultSca.analyzeNode(null, new SemanticEnvironment()).isCorrect());
    }

    @Test
    @DisplayName(
            "What: ASTSca | When: analyzing single node and default visitor | Then: returns empty"
                    + " violations list")
    void astSca_whenAnalyzingValidNodeAndDefaults_thenReturnsEmptyViolations() {
        // What: default ASTSca and valid AssignNode
        ASTSca defaultSca = new ASTSca();
        AssignNode assign =
                new AssignNode(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);

        // When: analyzing single assign node
        Result<List<String>> nodeRes = defaultSca.analyzeNode(assign, new SemanticEnvironment());

        // Then: succeeds with empty violations
        assertTrue(nodeRes.isCorrect());
        assertTrue(((CorrectResult<List<String>>) nodeRes).value().isEmpty());

        // When & Then: default visitor returns empty list
        assertEquals(
                List.of(),
                defaultSca.visitDefault(new StringLiteralNode("s", 1, 1), new ScaContext()));

        // fromYamlConfig helper
        String yaml = "identifier_format: \"camel case\"";
        ASTSca fromYaml =
                ASTSca.fromYamlConfig(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(fromYaml);
    }
}
