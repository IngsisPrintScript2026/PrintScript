/*
 * My Project
 */

package sca.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sca.fake.ScaNodes.call;
import static sca.fake.ScaNodes.let;
import static sca.fake.ScaNodes.number;
import static sca.fake.ScaNodes.sumAt;
import static sca.fake.ScaNodes.variable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import node.Node;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.BooleanLiteralNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ASTSca;
import sca.ScaContext;
import sca.fake.StubScaNodeHandler;
import sca.handler.ScaNodeHandler;
import semantic.environment.SemanticEnvironment;

class ASTScaUnitTest {
    private static final ScaContext SNAKE = new ScaContext("snake case", false, false);

    private static List<String> violations(Result<List<String>> result) {
        return ((CorrectResult<List<String>>) result).value();
    }

    private static ProgramNode program(Node... statements) {
        return new ProgramNode(List.of(statements), 1, 1);
    }

    @Test
    @DisplayName(
            "Given: ASTSca | When: analyzing a null program | Then: fails saying the program"
                    + " cannot be null")
    void astSca_whenProgramIsNull_thenFails() {
        // Given
        ASTSca sca = new ASTSca();

        // When
        Result<List<String>> result = sca.analyze(null, new SemanticEnvironment());

        // Then
        assertFalse(result.isCorrect());
        assertTrue(((IncorrectResult<List<String>>) result).error().contains("cannot be null"));
    }

    @Test
    @DisplayName(
            "Given: ASTSca | When: analyzing a null node | Then: fails saying the node cannot be"
                    + " null")
    void astSca_whenNodeIsNull_thenFails() {
        // Given
        ASTSca sca = new ASTSca();

        // When
        Result<List<String>> result = sca.analyzeNode(null, new SemanticEnvironment());

        // Then
        assertFalse(result.isCorrect());
        assertTrue(((IncorrectResult<List<String>>) result).error().contains("cannot be null"));
    }

    @Test
    @DisplayName(
            "Given: ASTSca with snake case | When: analyzing a program with one bad name | Then:"
                    + " succeeds with that single violation")
    void astSca_whenProgramHasBadName_thenReturnsViolation() {
        // Given
        ASTSca sca = new ASTSca(SNAKE);

        // When
        Result<List<String>> result =
                sca.analyze(program(let("good_one", 1, 5), let("badOne", 2, 5)), null);

        // Then
        assertTrue(result.isCorrect());
        assertEquals(1, violations(result).size());
        assertTrue(violations(result).get(0).contains("'badOne'"));
    }

    @Test
    @DisplayName(
            "Given: ASTSca with snake case | When: analyzing a clean program | Then: succeeds with"
                    + " no violations")
    void astSca_whenProgramIsClean_thenReturnsEmptyList() {
        // Given
        ASTSca sca = new ASTSca(SNAKE);

        // When
        Result<List<String>> result =
                sca.analyze(program(let("good_one", 1, 5)), new SemanticEnvironment());

        // Then
        assertTrue(result.isCorrect());
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: ASTSca with snake case | When: analyzing with an explicit camel case context |"
                    + " Then: the explicit context wins")
    void astSca_whenExplicitContextGiven_thenOverridesDefault() {
        // Given
        ASTSca sca = new ASTSca(SNAKE);
        ScaContext camel = new ScaContext("camel case", false, false);

        // When
        Result<List<String>> result =
                sca.analyze(program(let("goodName", 1, 5)), new SemanticEnvironment(), camel);

        // Then
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: ASTSca with a default context | When: analyzing with a null context | Then: the"
                    + " default context is used")
    void astSca_whenContextIsNull_thenUsesDefaultContext() {
        // Given
        StubScaNodeHandler<ProgramNode> stub =
                new StubScaNodeHandler<>(ProgramNode.class, List.of());
        ASTSca sca = new ASTSca(List.of(stub), SNAKE);

        // When
        sca.analyze(program(), new SemanticEnvironment(), null);

        // Then
        assertSame(SNAKE, stub.receivedContexts().get(0));
    }

    @Test
    @DisplayName(
            "Given: ASTSca built with a null default context | When: analyzing a program | Then:"
                    + " no rule is enforced")
    void astSca_whenDefaultContextIsNull_thenUsesEmptyRules() {
        // Given
        ASTSca sca = new ASTSca((ScaContext) null);

        // When
        Result<List<String>> result = sca.analyze(program(let("Bad_Name", 1, 5)), null);

        // Then
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: ASTSca with snake case | When: analyzing a single declaration node | Then:"
                    + " returns its violation")
    void astSca_whenAnalyzingSingleNode_thenReturnsItsViolations() {
        // Given
        ASTSca sca = new ASTSca(SNAKE);

        // When
        Result<List<String>> result = sca.analyzeNode(let("badName", 7, 5), null);

        // Then
        assertEquals(1, violations(result).size());
        assertTrue(violations(result).get(0).contains("line 7, column 5"));
    }

    @Test
    @DisplayName(
            "Given: default ASTSca | When: analyzing an assignment | Then: reports no violation"
                    + " since assignments have no handler")
    void astSca_whenAnalyzingAssignment_thenNoViolation() {
        // Given
        ASTSca sca = new ASTSca(new ScaContext("snake case", true, true));
        AssignNode assign = new AssignNode(new IdentifierNode("badName", 1, 1), sumAt(1, 9), 1, 1);

        // When
        Result<List<String>> result = sca.analyzeNode(assign, new SemanticEnvironment());

        // Then
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: default ASTSca | When: analyzing a bare expression node | Then: the default"
                    + " visit reports no violation")
    void astSca_whenAnalyzingExpressionNode_thenNoViolation() {
        // Given
        ASTSca sca = new ASTSca(new ScaContext("snake case", true, true));

        // When
        Result<List<String>> result = sca.analyzeNode(variable("badName"), null);

        // Then
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: ASTSca with only a declaration handler | When: analyzing a println with an"
                    + " expression | Then: reports no violation because no handler applies")
    void astSca_whenHandlerIsMissing_thenNoViolation() {
        // Given
        List<ScaNodeHandler<?>> handlers = List.of(new sca.handler.DeclarationScaHandler());
        ASTSca sca = new ASTSca(handlers, new ScaContext(null, true, true));

        // When
        Result<List<String>> result = sca.analyzeNode(call("println", sumAt(1, 9)), null);

        // Then
        assertTrue(violations(result).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: ASTSca with no handlers at all | When: analyzing every kind of statement |"
                    + " Then: reports no violation")
    void astSca_whenNoHandlersRegistered_thenEveryNodeIsClean() {
        // Given
        ASTSca sca = new ASTSca(List.of(), SNAKE);
        IfKeywordNode ifNode =
                new IfKeywordNode(new BooleanLiteralNode(true, 1, 1), List.of(), List.of(), 1, 1);
        AssignNode assign = new AssignNode(variable("x"), number(1), 1, 1);

        // When
        List<Node> nodes = List.of(let("badName", 1, 1), ifNode, assign, call("println"));
        long withViolations =
                nodes.stream().filter(n -> !violations(sca.analyzeNode(n, null)).isEmpty()).count();

        // Then
        assertEquals(0, withViolations);
        assertTrue(violations(sca.analyze(program(), null)).isEmpty());
    }

    @Test
    @DisplayName(
            "Given: ASTSca with a stub assignment handler | When: analyzing an assignment | Then:"
                    + " returns what the handler reports")
    void astSca_whenCustomHandlerRegistered_thenDelegatesToIt() {
        // Given
        StubScaNodeHandler<AssignNode> stub =
                new StubScaNodeHandler<>(AssignNode.class, List.of("custom violation"));
        ASTSca sca = new ASTSca(List.of(stub), SNAKE);

        // When
        Result<List<String>> result =
                sca.analyzeNode(new AssignNode(variable("x"), number(1), 1, 1), null);

        // Then
        assertEquals(List.of("custom violation"), violations(result));
    }

    @Test
    @DisplayName(
            "Given: a YAML with snake case | When: building ASTSca from it | Then: camel case names"
                    + " are reported")
    void astSca_whenBuiltFromYaml_thenAppliesItsRules() {
        // Given
        ByteArrayInputStream yaml =
                new ByteArrayInputStream(
                        "identifier_format: \"snake case\"\n".getBytes(StandardCharsets.UTF_8));

        // When
        ASTSca sca = ASTSca.fromYamlConfig(yaml);
        DeclarationKeywordNode decl = let("camelName", 1, 5);

        // Then
        assertEquals(1, violations(sca.analyzeNode(decl, null)).size());
    }
}
