/*
 * My Project
 */

package formatter.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import formatter.ASTFormatter;
import formatter.FormatContext;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.factory.NodeFactory;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

class ASTFormatterUnitTest {

    @Test
    @DisplayName("What: ASTFormatter | When: formatting null inputs | Then: returns failure result")
    void astFormatter_whenFormattingNullInputs_thenReturnsFailure() {
        // What: ASTFormatter
        ASTFormatter formatter = new ASTFormatter();

        // When & Then: format null program and null node fails
        assertFalse(formatter.format(null).isCorrect());
        assertFalse(formatter.formatNode(null).isCorrect());
    }

    @Test
    @DisplayName(
            "What: ASTFormatter | When: formatting single node and expressions | Then: outputs"
                    + " correctly formatted strings")
    void astFormatter_whenFormattingSingleNodeAndExpressions_thenFormatsCorrectly() {
        // What: ASTFormatter and AST nodes
        ASTFormatter formatter = new ASTFormatter();

        DeclarationKeywordNode decl =
                NodeFactory.createDeclaration(
                        DeclarationType.LET,
                        new IdentifierNode("num", 1, 1),
                        new NumberLiteralNode(null, 1, 1),
                        new Token(TokenType.LET, "let", new Position(1, 1)));

        // When: formatting uninitialized node
        Result<String> nodeRes = formatter.formatNode(decl);

        // Then: formats with default "0"
        assertTrue(nodeRes.isCorrect());
        assertEquals("let num: number = 0;", ((CorrectResult<String>) nodeRes).value());

        // When: formatting binary operator expression
        FormatContext ctx = new FormatContext();
        OperatorNode op =
                new OperatorNode(
                        OperatorType.PLUS,
                        new NumberLiteralNode(BigDecimal.ONE, 1, 1),
                        new NumberLiteralNode(BigDecimal.TEN, 1, 1),
                        1,
                        1);
        String opFormatted = formatter.formatExpression(op);

        // Then: formatted expression contains spaces around operator
        assertEquals("1 + 10", opFormatted);

        // When: formatting function call with multiple arguments
        CallFunctionNode callMultiArgs =
                new CallFunctionNode(
                        new IdentifierNode("customFunc", 1, 1),
                        List.of(
                                new StringLiteralNode("arg1", 1, 1),
                                new StringLiteralNode("arg2", 1, 1)),
                        1,
                        1);
        String callExpr = formatter.formatExpression(callMultiArgs);
        String callStmt = formatter.visitDefault(callMultiArgs, ctx);

        // Then: formatted properly with commas and semicolons
        assertEquals("customFunc(\"arg1\", \"arg2\")", callExpr);
        assertEquals("customFunc(\"arg1\", \"arg2\");", callStmt);

        FormatContext ctxNoSpace =
                new FormatContext(
                        0, 4, null, null, null, null, null, null, null, null, null, false);
        assertEquals(
                "customFunc(\"arg1\",\"arg2\")",
                formatter.formatExpression(callMultiArgs, ctxNoSpace));
        assertEquals(
                "customFunc(\"arg1\",\"arg2\");",
                formatter.visitDefault(callMultiArgs, ctxNoSpace));

        // fromYamlConfig
        String yaml = "space-before-colon: true";
        ASTFormatter yamlFormatter =
                ASTFormatter.fromYamlConfig(
                        new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(yamlFormatter);
    }
}
