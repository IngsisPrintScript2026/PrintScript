/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import java.math.BigDecimal;
import java.util.List;
import node.ProgramNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import syntactic.SyntacticParser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

class ExpressionParserIntegrationTest {

    @Test
    @DisplayName(
            "What: ExpressionParser in SyntacticParser | When: parsing binary expression with"
                    + " precedence (5 + 3 * 2) | Then: builds OperatorNode tree with '*' evaluated"
                    + " before '+'")
    void
            expressionParser_whenParsingBinaryExpressionWithPrecedence_thenBuildsOperatorTreeRespectingPrecedence() {
        // What: tokens for "let x: number = 5 + 3 * 2;"
        Position pos = new Position(1, 1);
        List<Token> tokens =
                List.of(
                        new Token(TokenType.LET, "let", pos, pos),
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.COLON, ":", pos, pos),
                        new Token(TokenType.NUMBER, "number", pos, pos),
                        new Token(TokenType.EQUAL, "=", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "5", pos, pos),
                        new Token(TokenType.PLUS, "+", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "3", pos, pos),
                        new Token(TokenType.STAR, "*", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "2", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));

        TokenStreamAdapter tokenStream = new TokenStreamAdapter(tokens, 0);
        SyntacticParser parser = new SyntacticParser(Version.V_1_0);

        // When: parsing token stream into ProgramNode
        Result<IterationStep<ProgramNode>> result = parser.parse(tokenStream);

        // Then: parsing succeeds and creates correct AST respecting precedence
        assertTrue(result.isCorrect(), "Parsing expression with operators should succeed");
        ProgramNode programNode =
                ((CorrectResult<IterationStep<ProgramNode>>) result).value().value();
        assertEquals(1, programNode.statements().size());

        DeclarationKeywordNode decl = (DeclarationKeywordNode) programNode.statements().getFirst();
        assertTrue(
                decl.expressionNode() instanceof OperatorNode,
                "Expression must be a binary OperatorNode");

        OperatorNode rootOp = (OperatorNode) decl.expressionNode();
        assertEquals("+", rootOp.symbol(), "Root operator must be '+' due to '*' precedence");
        assertEquals(OperatorType.PLUS, rootOp.operatorType());

        assertTrue(rootOp.left() instanceof NumberLiteralNode);
        assertEquals(new BigDecimal("5"), ((NumberLiteralNode) rootOp.left()).rawValue());

        assertTrue(rootOp.right() instanceof OperatorNode);
        OperatorNode rightOp = (OperatorNode) rootOp.right();
        assertEquals("*", rightOp.symbol());
        assertEquals(OperatorType.STAR, rightOp.operatorType());
    }
}
