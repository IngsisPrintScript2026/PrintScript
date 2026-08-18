package syntactic;

import iterator.IterationStep;
import node.ProgramNode;
import node.keyword.DeclarationKeywordNode;
import node.expression.literal.NumberLiteralNode;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyntacticParserTest {

    @Test
    void testParseLetDeclaration() {
        Position pos = new Position(1, 1);
        List<Token> tokens = List.of(
                new Token(TokenType.LET, "let", pos, pos),
                new Token(TokenType.IDENTIFIER, "x", pos, pos),
                new Token(TokenType.COLON, ":", pos, pos),
                new Token(TokenType.NUMBER, "number", pos, pos),
                new Token(TokenType.EQUAL, "=", pos, pos),
                new Token(TokenType.NUMBER_LITERAL, "42", pos, pos),
                new Token(TokenType.SEMICOLON, ";", pos, pos)
        );

        TokenStreamAdapter tokenStream = new TokenStreamAdapter(tokens, 0);
        SyntacticParser parser = new SyntacticParser(Version.V_1_0);

        Result<IterationStep<ProgramNode>> result = parser.parse(tokenStream);

        assertTrue(result.isCorrect(), "El parseo del programa debería ser exitoso");
        ProgramNode programNode = ((CorrectResult<IterationStep<ProgramNode>>) result).value().value();
        assertEquals(1, programNode.statements().size(), "El programa debe contener 1 sentencia");

        assertTrue(programNode.statements().getFirst() instanceof DeclarationKeywordNode);
        DeclarationKeywordNode decl = (DeclarationKeywordNode) programNode.statements().getFirst();
        assertEquals("x", decl.identifierNode().name());
        assertTrue(decl.expressionNode() instanceof NumberLiteralNode);
        assertEquals(new BigDecimal("42"), ((NumberLiteralNode) decl.expressionNode()).rawValue());
    }
}
