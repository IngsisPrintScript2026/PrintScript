/*
 * My Project
 */

package sca;

import static org.junit.jupiter.api.Assertions.*;

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
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import sca.config.YamlScaRulesLoader;
import semantic.environment.SemanticEnvironment;
import token.Token;
import token.TokenType;

public class ASTScaTest {

    @Test
    void testCamelCaseIdentifierFormatViolation() {
        String yamlRules =
                """
                identifier_format: "camel case"
                """;

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
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("camel case"));
    }

    @Test
    void testSnakeCaseIdentifierFormatViolation() {
        String yamlRules =
                """
                identifier_format: "snake case"
                """;

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
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("snake case"));
    }

    @Test
    void testPrintlnExpressionViolation() {
        String yamlRules =
                """
                mandatory-variable-or-literal-in-println: true
                """;

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
                        "println",
                        List.of(expr),
                        new Token(TokenType.IDENTIFIER, "println", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(badCall));
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("must be a literal or variable"));
    }

    @Test
    void testReadInputExpressionViolation() {
        String yamlRules =
                """
                mandatory-variable-or-literal-in-readInput: true
                """;

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
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("must be a literal or variable"));
    }

    @Test
    void testIfScaHandler() {
        String yamlRules =
                """
                mandatory-variable-or-literal-in-println: true
                """;

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
                        "println",
                        List.of(expr),
                        new Token(TokenType.IDENTIFIER, "println", new Position(1, 1)));

        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        new StringLiteralNode("true", 1, 1),
                        List.of(badCall),
                        List.of(badCall),
                        new Token(TokenType.IF, "if", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(ifNode));
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(2, violations.size());
    }

    @Test
    void testASTScaEdgeCases() {
        ASTSca defaultSca = new ASTSca();
        assertFalse(defaultSca.analyze(null, new SemanticEnvironment()).isCorrect());
        assertFalse(defaultSca.analyzeNode(null, new SemanticEnvironment()).isCorrect());

        AssignNode assign =
                new AssignNode(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        Result<List<String>> nodeRes = defaultSca.analyzeNode(assign, new SemanticEnvironment());
        assertTrue(nodeRes.isCorrect());
        assertTrue(((CorrectResult<List<String>>) nodeRes).value().isEmpty());

        assertEquals(
                List.of(),
                defaultSca.visitDefault(new StringLiteralNode("s", 1, 1), new ScaContext()));

        // fromYamlConfig
        String yaml = "identifier_format: \"camel case\"";
        ASTSca fromYaml =
                ASTSca.fromYamlConfig(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(fromYaml);
    }
}
