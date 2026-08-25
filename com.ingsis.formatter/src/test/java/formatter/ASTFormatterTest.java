package formatter;

import formatter.config.YamlFormatRulesLoader;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.factory.NodeFactory;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.Test;
import result.CorrectResult;
import result.Result;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ASTFormatterTest {

    @Test
    void testFormatDeclarationAndAssignDefault() {
        DeclarationKeywordNode decl = NodeFactory.createDeclaration(
                DeclarationType.LET,
                new IdentifierNode("x", 1, 1),
                new NumberLiteralNode(BigDecimal.valueOf(42), 1, 1),
                new token.Token(token.TokenType.LET, "let", new position.Position(1, 1))
        );

        ProgramNode program = NodeFactory.createProgram(List.of(decl));
        Formatter formatter = new ASTFormatter();
        Result<String> result = formatter.format(program);

        assertTrue(result.isCorrect(), "Formatting should succeed");
        assertEquals("let x: number = 42;", ((CorrectResult<String>) result).value().trim());
    }

    @Test
    void testYamlRulesConsigna1And2() {
        String yamlConfig = """
                space-before-colon: true
                space-after-colon: true
                space-around-equals: false
                indent-inside-if: 2
                if-brace-same-line: false
                """;

        ByteArrayInputStream configStream = new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
        FormatContext context = YamlFormatRulesLoader.loadFromYaml(configStream);
        ASTFormatter formatter = new ASTFormatter(context);

        DeclarationKeywordNode decl = NodeFactory.createDeclaration(
                DeclarationType.CONST,
                new IdentifierNode("flag", 1, 1),
                new BooleanLiteralNode(true, 1, 1),
                new token.Token(token.TokenType.CONST, "const", new position.Position(1, 1))
        );

        node.expression.function.CallFunctionNode callThen = NodeFactory.createCall(
                "println",
                List.of(new StringLiteralNode("Yes", 2, 5)),
                new token.Token(token.TokenType.IDENTIFIER, "println", new position.Position(2, 5))
        );

        node.keyword.IfKeywordNode ifNode = NodeFactory.createIf(
                new IdentifierNode("flag", 1, 1),
                List.of(callThen),
                List.of(),
                new token.Token(token.TokenType.IF, "if", new position.Position(1, 1))
        );

        ProgramNode program = NodeFactory.createProgram(List.of(decl, ifNode));
        Result<String> result = formatter.format(program);

        assertTrue(result.isCorrect());
        String formatted = ((CorrectResult<String>) result).value();

        assertTrue(formatted.contains("const flag : boolean=true;"), "Should respect space-before-colon and no space-around-equals");
        assertTrue(formatted.contains("if (flag)\n{"), "Should put brace below line when if-brace-same-line is false");
        assertTrue(formatted.contains("  println(\"Yes\");"), "Should use 2 spaces indentation inside if");
    }
}
