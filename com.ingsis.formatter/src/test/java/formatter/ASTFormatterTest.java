/*
 * My Project
 */

package formatter;

import static org.junit.jupiter.api.Assertions.*;

import formatter.config.YamlFormatRulesLoader;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import node.ProgramNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
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
import token.Token;
import token.TokenType;

public class ASTFormatterTest {

    @Test
    void testFormatDeclarationAndAssignDefault() {
        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(42), 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));

        AssignNode assign =
                NodeFactory.createAssign(
                        new IdentifierNode("x", 1, 1),
                        new NumberLiteralNode(BigDecimal.valueOf(100), 1, 1),
                        new Token(TokenType.EQUAL, "=", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(decl, assign));
        Formatter formatter = new ASTFormatter();
        Result<String> result = formatter.format(program);

        assertTrue(result.isCorrect(), "Formatting should succeed");
        String formatted = ((CorrectResult<String>) result).value();
        assertTrue(formatted.contains("let x: number = 42;"));
        assertTrue(formatted.contains("x = 100;"));
    }

    @Test
    void testYamlRulesConsigna1And2() {
        String yamlConfig =
                """
                space-before-colon: true
                space-after-colon: true
                space-around-equals: false
                indent-inside-if: 2
                if-brace-same-line: false
                """;

        ByteArrayInputStream configStream =
                new ByteArrayInputStream(yamlConfig.getBytes(StandardCharsets.UTF_8));
        FormatContext context = YamlFormatRulesLoader.loadFromYaml(configStream);
        ASTFormatter formatter = new ASTFormatter(context);

        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        DeclarationType.CONST,
                        new IdentifierNode("flag", 1, 1),
                        new BooleanLiteralNode(true, 1, 1),
                        new Token(TokenType.CONST, "const", new Position(1, 1)));

        CallFunctionNode callThen =
                NodeFactory.createCall(
                        "println",
                        List.of(new StringLiteralNode("Yes", 2, 5)),
                        new Token(TokenType.IDENTIFIER, "println", new Position(2, 5)));

        CallFunctionNode callElse =
                NodeFactory.createCall(
                        "println",
                        List.of(new StringLiteralNode("No", 3, 5)),
                        new Token(TokenType.IDENTIFIER, "println", new Position(3, 5)));

        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        new IdentifierNode("flag", 1, 1),
                        List.of(callThen),
                        List.of(callElse),
                        new Token(TokenType.IF, "if", new Position(1, 1)));

        ProgramNode program = NodeFactory.createProgram(List.of(decl, ifNode));
        Result<String> result = formatter.format(program);

        assertTrue(result.isCorrect());
        String formatted = ((CorrectResult<String>) result).value();

        assertTrue(
                formatted.contains("const flag : boolean=true;"),
                "Should respect space-before-colon and no space-around-equals");
        assertTrue(
                formatted.contains("if (flag)\n{"),
                "Should put brace below line when if-brace-same-line is false");
        assertTrue(
                formatted.contains("else\n{"),
                "Should put else brace below line when if-brace-same-line is false");
        assertTrue(
                formatted.contains("  println(\"Yes\");"),
                "Should use 2 spaces indentation inside if");
        assertTrue(
                formatted.contains("  println(\"No\");"),
                "Should use 2 spaces indentation inside else");
    }

    @Test
    void testIfWithSameLineBraceAndAssignNoSpaces() {
        FormatContext ctx =
                new FormatContext(
                        0,
                        4,
                        false, // spaceBeforeColon
                        true, // spaceAfterColon
                        false, // spaceAroundEquals
                        true, // spaceAroundOperators
                        true, // lineBreakAfterStatement
                        1, // lineBreaksAfterPrintln
                        false, // singleSpaceSeparation
                        true, // ifBraceSameLine
                        false // ifBraceBelowLine
                        );
        ASTFormatter formatter = new ASTFormatter(ctx);

        CallFunctionNode callThen =
                NodeFactory.createCall(
                        "println",
                        List.of(new StringLiteralNode("Yes", 1, 1)),
                        new Token(TokenType.IDENTIFIER, "println", new Position(1, 1)));

        CallFunctionNode callElse =
                NodeFactory.createCall(
                        "println",
                        List.of(new StringLiteralNode("No", 1, 1)),
                        new Token(TokenType.IDENTIFIER, "println", new Position(1, 1)));

        IfKeywordNode ifNode =
                NodeFactory.createIf(
                        new BooleanLiteralNode(true, 1, 1),
                        List.of(callThen),
                        List.of(callElse),
                        new Token(TokenType.IF, "if", new Position(1, 1)));

        String formatted = formatter.formatStatement(ifNode, ctx);
        assertTrue(formatted.contains("if (true) {\n"));
        assertTrue(formatted.contains("} else {\n"));

        AssignNode assign =
                new AssignNode(
                        new IdentifierNode("a", 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        String formattedAssign = formatter.formatStatement(assign, ctx);
        assertEquals("a=10;", formattedAssign);
    }

    @Test
    void testASTFormatterExpressionsAndEdgeCases() {
        ASTFormatter formatter = new ASTFormatter();

        assertFalse(formatter.format(null).isCorrect());
        assertFalse(formatter.formatNode(null).isCorrect());

        // Format single node
        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("num", 1, 1),
                        new NumberLiteralNode(null, 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));
        Result<String> nodeRes = formatter.formatNode(decl);
        assertTrue(nodeRes.isCorrect());
        assertEquals("let num: number = 0;", ((CorrectResult<String>) nodeRes).value());

        // Expression formatting
        FormatContext ctx = new FormatContext();
        OperatorNode op =
                new OperatorNode(
                        OperatorType.PLUS,
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        assertEquals("1 + 10", formatter.formatExpression(op));

        CallFunctionNode callMultiArgs =
                new CallFunctionNode(
                        new IdentifierNode("customFunc", 1, 1),
                        List.of(
                                new StringLiteralNode("arg1", 1, 1),
                                new StringLiteralNode("arg2", 1, 1)),
                        1,
                        1);
        assertEquals("customFunc(\"arg1\", \"arg2\")", formatter.formatExpression(callMultiArgs));

        // Default visit
        assertEquals("customFunc(\"arg1\", \"arg2\");", formatter.visitDefault(callMultiArgs, ctx));

        // fromYamlConfig
        String yaml = "space-before-colon: true";
        ASTFormatter yamlFormatter =
                ASTFormatter.fromYamlConfig(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(yamlFormatter);
    }
}
