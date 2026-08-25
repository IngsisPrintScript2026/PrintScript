package sca;

import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.factory.NodeFactory;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;
import sca.config.YamlScaRulesLoader;
import semantic.environment.SemanticEnvironment;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ASTScaTest {

    @Test
    void testCamelCaseIdentifierFormatViolation() {
        String yamlRules = """
                identifier_format: "camel case"
                """;

        ScaContext context = YamlScaRulesLoader.loadFromYaml(new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        DeclarationKeywordNode badDecl = NodeFactory.createDeclaration(
                DeclarationType.LET,
                new IdentifierNode("my_variable_name", 1, 1),
                new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                new token.Token(token.TokenType.LET, "let", new position.Position(1, 1))
        );

        ProgramNode program = NodeFactory.createProgram(List.of(badDecl));
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("camel case"));
    }

    @Test
    void testPrintlnExpressionViolation() {
        String yamlRules = """
                mandatory-variable-or-literal-in-println: true
                """;

        ScaContext context = YamlScaRulesLoader.loadFromYaml(new ByteArrayInputStream(yamlRules.getBytes(StandardCharsets.UTF_8)));
        ASTSca sca = new ASTSca(context);

        OperatorNode expr = NodeFactory.createOperator(
                OperatorType.PLUS,
                new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                new token.Token(token.TokenType.PLUS, "+", new position.Position(1, 1))
        );

        node.expression.function.CallFunctionNode badCall = NodeFactory.createCall(
                "println",
                List.of(expr),
                new token.Token(token.TokenType.IDENTIFIER, "println", new position.Position(1, 1))
        );

        ProgramNode program = NodeFactory.createProgram(List.of(badCall));
        Result<List<String>> result = sca.analyze(program, new SemanticEnvironment());

        assertTrue(result.isCorrect());
        List<String> violations = ((CorrectResult<List<String>>) result).value();
        assertEquals(1, violations.size());
        assertTrue(violations.get(0).contains("must be a literal or variable"));
    }
}
