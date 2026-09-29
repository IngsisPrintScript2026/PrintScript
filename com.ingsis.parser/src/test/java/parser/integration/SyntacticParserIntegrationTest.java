/*
 * My Project
 */

package parser.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import java.math.BigDecimal;
import java.util.List;
import node.ProgramNode;
import node.expression.ExpressionNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.NumberLiteralNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.SyntacticParser;
import syntactic.parser.literal.IdentifierParser;
import syntactic.parser.literal.NumberLiteralParser;
import syntactic.parser.literal.StringLiteralParser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.FunctionParser;
import token.Token;
import token.TokenType;
import tokenstream.TokenStreamAdapter;
import version.Version;

class SyntacticParserIntegrationTest {

    private final Position pos = new Position(1, 1);

    private static <T extends ExpressionNode> Parser<ExpressionNode> asExprParser(
            Parser<T> parser) {
        return stream -> {
            Result<IterationStep<T>> res = parser.parse(stream);
            if (res.isCorrect()) {
                IterationStep<T> step = ((CorrectResult<IterationStep<T>>) res).value();
                return Result.success(new IterationStep<>(step.value(), step.next()));
            }
            return Result.failure(((IncorrectResult<IterationStep<T>>) res).error());
        };
    }

    @Test
    @DisplayName(
            "What: SyntacticParser | When: parsing let declaration | Then: emits ProgramNode with"
                    + " DeclarationKeywordNode")
    void syntacticParser_whenParsingLetDeclaration_thenEmitsProgramNodeWithDeclaration() {
        // What: tokens for "let x: number = 42;"
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

        // When: parsing token stream
        Result<IterationStep<ProgramNode>> result = parser.parse(tokenStream);

        // Then: program has 1 declaration statement with identifier "x" and value 42
        assertTrue(result.isCorrect());
        ProgramNode programNode =
                ((CorrectResult<IterationStep<ProgramNode>>) result).value().value();
        assertEquals(1, programNode.statements().size());

        assertTrue(programNode.statements().getFirst() instanceof DeclarationKeywordNode);
        DeclarationKeywordNode decl = (DeclarationKeywordNode) programNode.statements().getFirst();
        assertEquals("x", decl.identifierNode().name());
        assertTrue(decl.expressionNode() instanceof NumberLiteralNode);
        assertEquals(new BigDecimal("42"), ((NumberLiteralNode) decl.expressionNode()).rawValue());
    }

    @Test
    @DisplayName(
            "What: SyntacticParser | When: parsing direct program AST | Then: emits ProgramNode"
                    + " directly")
    void syntacticParser_whenParsingDirectProgramAST_thenEmitsProgramNode() {
        // What: tokens for "let x: number = 42;"
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

        // When: parsing program directly
        Result<ProgramNode> result = parser.parseProgram(tokenStream);

        // Then: returns ProgramNode directly
        assertTrue(result.isCorrect());
        ProgramNode programNode = ((CorrectResult<ProgramNode>) result).value();
        assertEquals(1, programNode.statements().size());
    }

    @Test
    @DisplayName(
            "What: AssignParser | When: parsing valid assignment and invalid assignment | Then:"
                    + " emits AssignNode or fails")
    void assignParser_whenParsingAssignment_thenEmitsAssignNodeOrFails() {
        // What: AssignParser and tokens for "x = 10;"
        AssignParser assignParser =
                new AssignParser(new IdentifierParser(), asExprParser(new NumberLiteralParser()));

        List<Token> tokens =
                List.of(
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.EQUAL, "=", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "10", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        TokenStreamAdapter stream = new TokenStreamAdapter(tokens, 0);

        // When: parsing valid assignment
        Result<IterationStep<AssignNode>> result = assignParser.parse(stream);

        // Then: emits AssignNode with name "x"
        assertTrue(result.isCorrect());
        AssignNode node = ((CorrectResult<IterationStep<AssignNode>>) result).value().value();
        assertEquals("x", node.identifierNode().name());

        // When & Then: invalid assignment without equal fails
        List<Token> badTokens =
                List.of(
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        assertFalse(assignParser.parse(new TokenStreamAdapter(badTokens, 0)).isCorrect());
    }

    @Test
    @DisplayName(
            "What: FunctionParser | When: parsing println call | Then: emits CallFunctionNode or"
                    + " fails on syntax error")
    void functionParser_whenParsingPrintlnCall_thenEmitsCallFunctionNodeOrFails() {
        // What: FunctionParser and tokens for println("hello");
        FunctionParser functionParser =
                new FunctionParser(new IdentifierParser(), asExprParser(new StringLiteralParser()));

        List<Token> tokens =
                List.of(
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"hello\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        TokenStreamAdapter stream = new TokenStreamAdapter(tokens, 0);

        // When: parsing valid function call
        Result<IterationStep<CallFunctionNode>> result = functionParser.parse(stream);

        // Then: emits CallFunctionNode with name "println"
        assertTrue(result.isCorrect());
        CallFunctionNode node =
                ((CorrectResult<IterationStep<CallFunctionNode>>) result).value().value();
        assertEquals("println", node.identifierNode().name());

        // When & Then: missing right parenthesis fails
        List<Token> badTokens =
                List.of(
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"hello\"", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        assertFalse(functionParser.parse(new TokenStreamAdapter(badTokens, 0)).isCorrect());
    }

    @Test
    @DisplayName(
            "What: SyntacticParser error handling | When: stream is null, empty, or contains"
                    + " invalid syntax | Then: fails with syntactic error")
    void syntacticParser_whenStreamIsInvalidOrEmpty_thenFailsGracefully() {
        // What: SyntacticParser
        SyntacticParser parser = new SyntacticParser(Version.V_1_0);

        // When & Then: null and empty stream fail
        assertFalse(parser.parseStatement(null).isCorrect());
        assertFalse(parser.parseStatement(new TokenStreamAdapter(List.of(), 0)).isCorrect());

        // When & Then: invalid consecutive semicolons fail
        List<Token> invalidTokens =
                List.of(
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        assertFalse(parser.parseProgram(new TokenStreamAdapter(invalidTokens, 0)).isCorrect());
        assertFalse(parser.parse(new TokenStreamAdapter(invalidTokens, 0)).isCorrect());
    }
}
