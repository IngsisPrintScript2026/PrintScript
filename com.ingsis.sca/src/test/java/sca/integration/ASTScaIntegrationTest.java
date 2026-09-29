/*
 * My Project
 */

package sca.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.factory.NodeFactory;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import sca.ASTSca;
import sca.ScaContext;
import sca.config.YamlScaRulesLoader;
import semantic.environment.SemanticEnvironment;
import token.Token;
import token.TokenType;

class ASTScaIntegrationTest {
    private static final String PRINTLN = "println";

    @Test
    @DisplayName(
            "What: ASTSca | When: program violates camel case convention | Then: reports camel case"
                    + " format violation")
    void astSca_whenProgramViolatesCamelCase_thenEmitsCamelCaseViolation() {
        // What: ASTSca loaded with camel case rule
        String yamlRules = "identifier_format: \"camel case\"\n";
        ScaContext context =
                YamlScaRulesLoader.loadFromYaml(
                        new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        DeclarationKeywordNode badDecl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("my_variable_name", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));
        ProgramNode program = NodeFactory.createProgram(List.of(badDecl));

        // When: analyzing program
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        // Then: emits 1 violation mentioning camel case
        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("camel case"));
    }

    @Test
    @DisplayName(
            "What: ASTSca | When: program violates snake case convention | Then: reports snake case"
                    + " format violation")
    void astSca_whenProgramViolatesSnakeCase_thenEmitsSnakeCaseViolation() {
        // What: ASTSca loaded with snake case rule
        String yamlRules = "identifier_format: \"snake case\"\n";
        ScaContext context =
                YamlScaRulesLoader.loadFromYaml(
                        new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        DeclarationKeywordNode badDecl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("myCamelCaseVar", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));
        DeclarationKeywordNode goodDecl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("my_snake_case_var", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(badDecl, goodDecl));

        // When: analyzing program
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        // Then: emits 1 violation for badDecl mentioning snake case
        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("snake case"));
    }

    @Test
    @DisplayName(
            "What: ASTSca | When: println contains complex expression | Then: reports"
                    + " literal-or-variable rule violation")
    void astSca_whenPrintlnContainsComplexExpression_thenEmitsPrintlnViolation() {
        // What: ASTSca with mandatory-variable-or-literal-in-println enabled
        String yamlRules = "mandatory-variable-or-literal-in-println: true\n";
        ScaContext context =
                YamlScaRulesLoader.loadFromYaml(
                        new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        OperatorNode expr =
                NodeFactory.createOperator(
                        OperatorType.PLUS,
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        new Token(TokenType.PLUS, "+", new Position(1, 1)));
        CallFunctionNode badCall =
                NodeFactory.createCall(
                        PRINTLN,
                        List.of(expr),
                        new Token(TokenType.IDENTIFIER, PRINTLN, new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(badCall));

        // When: analyzing program
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        // Then: emits violation mentioning literal or variable
        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("must be a literal or variable"));
    }

    @Test
    @DisplayName(
            "What: ASTSca | When: readInput contains complex expression | Then: reports"
                    + " literal-or-variable rule violation")
    void astSca_whenReadInputContainsComplexExpression_thenEmitsReadInputViolation() {
        // What: ASTSca with mandatory-variable-or-literal-in-readInput enabled
        String yamlRules = "mandatory-variable-or-literal-in-readInput: true\n";
        ScaContext context =
                YamlScaRulesLoader.loadFromYaml(
                        new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        OperatorNode expr =
                NodeFactory.createOperator(
                        OperatorType.PLUS,
                        new StringLiteralNode("Enter ", 1, 1),
                        new StringLiteralNode("name: ", 1, 1),
                        new Token(TokenType.PLUS, "+", new Position(1, 1)));
        CallFunctionNode badCall =
                NodeFactory.createCall(
                        "readInput",
                        List.of(expr),
                        new Token(TokenType.IDENTIFIER, "readInput", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(badCall));

        // When: analyzing program
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        // Then: emits violation mentioning literal or variable
        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("must be a literal or variable"));
    }

    @Test
    @DisplayName(
            "What: ASTSca | When: if statement branches contain violations | Then: collects"
                    + " violations from both branches")
    void astSca_whenIfBranchesContainViolations_thenCollectsViolationsFromBothBranches() {
        // What: ASTSca with println rule enabled
        String yamlRules = "mandatory-variable-or-literal-in-println: true\n";
        ScaContext context =
                YamlScaRulesLoader.loadFromYaml(
                        new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        OperatorNode expr =
                NodeFactory.createOperator(
                        OperatorType.PLUS,
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        new Token(TokenType.PLUS, "+", new Position(1, 1)));
        CallFunctionNode badCall =
                NodeFactory.createCall(
                        PRINTLN,
                        List.of(expr),
                        new Token(TokenType.IDENTIFIER, PRINTLN, new Position(1, 1)));

        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        new StringLiteralNode("true", 1, 1),
                        List.of(badCall),
                        List.of(badCall),
                        new Token(TokenType.IF, "if", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(ifNode));

        // When: analyzing program with if statement
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        // Then: 2 violations collected (one from thenBody, one from elseBody)
        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(2, violations.size());
    }
}
