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
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

class SyntacticParserTest {

    @Test
    void testParseLetDeclaration() {
        Position pos = new Position(1, 1);
        List<Token> tokens =
                List.of(
                        new Token(TokenType.LET, "let", pos, pos),
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.COLON, ":", pos, pos),
                        new Token(TokenType.NUMBER, "number", pos, pos),
                        new Token(TokenType.EQUAL, "=", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "42", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));

        TokenStreamAdapter tokenStream = new TokenStreamAdapter(tokens, 0);
        SyntacticParser parser = new SyntacticParser(Version.V_1_0);

        Result<IterationStep<ProgramNode>> result = parser.parse(tokenStream);

        assertTrue(result.isCorrect(), "El parseo del programa debería ser exitoso");
        ProgramNode programNode =
                ((CorrectResult<IterationStep<ProgramNode>>) result).value().value();
        assertEquals(1, programNode.statements().size(), "El programa debe contener 1 sentencia");

        assertTrue(programNode.statements().getFirst() instanceof DeclarationKeywordNode);
        DeclarationKeywordNode decl = (DeclarationKeywordNode) programNode.statements().getFirst();
        assertEquals("x", decl.identifierNode().name());
        assertTrue(decl.expressionNode() instanceof NumberLiteralNode);
        assertEquals(new BigDecimal("42"), ((NumberLiteralNode) decl.expressionNode()).rawValue());
    }

    @Test
    void testParseProgramDirectAST() {
        Position pos = new Position(1, 1);
        List<Token> tokens =
                List.of(
                        new Token(TokenType.LET, "let", pos, pos),
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.COLON, ":", pos, pos),
                        new Token(TokenType.NUMBER, "number", pos, pos),
                        new Token(TokenType.EQUAL, "=", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "42", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));

        TokenStreamAdapter tokenStream = new TokenStreamAdapter(tokens, 0);
        SyntacticParser parser = new SyntacticParser(Version.V_1_0);

        Result<ProgramNode> result = parser.parseProgram(tokenStream);

        assertTrue(result.isCorrect(), "El parseo directo a ProgramNode debería ser exitoso");
        ProgramNode programNode = ((CorrectResult<ProgramNode>) result).value();
        assertEquals(1, programNode.statements().size());
    }

    @Test
    void testSyntacticParserErrorHandling() {
        SyntacticParser parser = new SyntacticParser(Version.V_1_0);

        // Null and empty stream
        assertFalse(parser.parseStatement(null).isCorrect());
        assertFalse(parser.parseStatement(new TokenStreamAdapter(List.of(), 0)).isCorrect());

        // Invalid statement in stream
        Position pos = new Position(1, 1);
        List<Token> invalidTokens =
                List.of(
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        assertFalse(parser.parseProgram(new TokenStreamAdapter(invalidTokens, 0)).isCorrect());
        assertFalse(parser.parse(new TokenStreamAdapter(invalidTokens, 0)).isCorrect());
    }
}
