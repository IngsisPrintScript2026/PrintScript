/*
 * My Project
 */

package syntactic;

import static org.junit.jupiter.api.Assertions.*;

import iterator.IterationStep;
import java.math.BigDecimal;
import java.util.List;
import node.ProgramNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.operator.OperatorNode;
import node.expression.operator.OperatorType;
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

class ExpressionParserTest {

    @Test
    void testParseBinaryExpressionPrecedence() {
        Position pos = new Position(1, 1);
        // let x: number = 5 + 3 * 2;
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

        Result<IterationStep<ProgramNode>> result = parser.parse(tokenStream);

        assertTrue(result.isCorrect(), "El parseo del programa con operaciones debe ser exitoso");
        ProgramNode programNode =
                ((CorrectResult<IterationStep<ProgramNode>>) result).value().value();
        assertEquals(1, programNode.statements().size());

        DeclarationKeywordNode decl = (DeclarationKeywordNode) programNode.statements().getFirst();
        assertTrue(
                decl.expressionNode() instanceof OperatorNode,
                "La expresión debe ser un OperatorNode binario");

        OperatorNode rootOp = (OperatorNode) decl.expressionNode();
        assertEquals("+", rootOp.symbol(), "La raíz debe ser '+' debido a la precedencia de '*'");
        assertEquals(OperatorType.PLUS, rootOp.operatorType());

        assertTrue(rootOp.left() instanceof NumberLiteralNode);
        assertEquals(new BigDecimal("5"), ((NumberLiteralNode) rootOp.left()).rawValue());

        assertTrue(rootOp.right() instanceof OperatorNode);
        OperatorNode rightOp = (OperatorNode) rootOp.right();
        assertEquals("*", rightOp.symbol());
        assertEquals(OperatorType.STAR, rightOp.operatorType());
    }
}
