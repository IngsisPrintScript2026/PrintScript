/*
 * My Project
 */

package parser.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.StringLiteralNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.ParserFactory;
import syntactic.parser.literal.BooleanLiteralParser;
import syntactic.parser.literal.NumberLiteralParser;
import syntactic.parser.literal.StringLiteralParser;
import syntactic.parser.root.LineExpressionParser;
import syntactic.strategy.EmptyDeclarationSymbolStrategy;
import syntactic.util.ArgumentsParserUtils;
import syntactic.util.BlockParserUtils;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.TokenStreamAdapter;
import tokenstream.rules.TokenMatchers;
import tokenstream.version.GrammarRules;
import version.Version;

class LiteralAndSymbolParsersUnitTest {

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
            "What: Literal parsers | When: parsing boolean and string tokens | Then: parses correct"
                    + " literal nodes")
    void literalParsers_whenParsingTokens_thenParsesCorrectLiteralNodes() {
        // What: Boolean and String literal tokens
        BooleanLiteralParser boolParser = new BooleanLiteralParser();
        TokenStreamAdapter boolStream =
                new TokenStreamAdapter(
                        List.of(new Token(TokenType.BOOLEAN_LITERAL, "true", pos, pos)), 0);

        // When: parsing boolean token
        Result<IterationStep<BooleanLiteralNode>> boolRes = boolParser.parse(boolStream);

        // Then: boolean node has value true
        assertTrue(boolRes.isCorrect());
        assertEquals(
                true,
                ((CorrectResult<IterationStep<BooleanLiteralNode>>) boolRes)
                        .value()
                        .value()
                        .rawValue());

        // What: String literal with double quotes
        StringLiteralParser strParser = new StringLiteralParser();
        TokenStreamAdapter strStream =
                new TokenStreamAdapter(
                        List.of(new Token(TokenType.STRING_LITERAL, "\"hello\"", pos, pos)), 0);

        // When: parsing double quoted string
        Result<IterationStep<StringLiteralNode>> strRes = strParser.parse(strStream);

        // Then: string node has rawValue "hello"
        assertTrue(strRes.isCorrect());
        assertEquals(
                "hello",
                ((CorrectResult<IterationStep<StringLiteralNode>>) strRes)
                        .value()
                        .value()
                        .rawValue());

        // What: String literal with single quotes
        StringLiteralParser strSingleQuoteParser = new StringLiteralParser();
        TokenStreamAdapter strSingleQuoteStream =
                new TokenStreamAdapter(
                        List.of(new Token(TokenType.STRING_LITERAL, "'world'", pos, pos)), 0);

        // When: parsing single quoted string
        Result<IterationStep<StringLiteralNode>> singleRes =
                strSingleQuoteParser.parse(strSingleQuoteStream);

        // Then: string node has rawValue "world"
        assertTrue(singleRes.isCorrect());
        assertEquals(
                "world",
                ((CorrectResult<IterationStep<StringLiteralNode>>) singleRes)
                        .value()
                        .value()
                        .rawValue());

        // What: LineExpressionParser wrapping string parser
        LineExpressionParser lineExprParser = new LineExpressionParser(asExprParser(strParser));
        TokenStreamAdapter lineStream =
                new TokenStreamAdapter(
                        List.of(
                                new Token(TokenType.STRING_LITERAL, "\"line\"", pos, pos),
                                new Token(TokenType.SEMICOLON, ";", pos, pos)),
                        0);

        // When & Then: line expression parser succeeds
        assertTrue(lineExprParser.parse(lineStream).isCorrect());
    }

    @Test
    @DisplayName(
            "What: EmptyDeclarationSymbolStrategy | When: parsing declaration ended with semicolon"
                    + " | Then: emits DeclarationKeywordNode")
    void emptyDeclarationStrategy_whenParsingUnassignedDeclaration_thenEmitsNode() {
        // What: EmptyDeclarationSymbolStrategy and tokens for declaration
        EmptyDeclarationSymbolStrategy emptyStrategy = new EmptyDeclarationSymbolStrategy();
        assertEquals(SymbolType.SEMICOLON, emptyStrategy.targetSymbol());

        Token keyTok = new Token(TokenType.LET, "let", pos, pos);
        IdentifierNode idNode = new IdentifierNode("x", 1, 1);
        TokenStream stream =
                new TokenStreamAdapter(List.of(new Token(TokenType.SEMICOLON, ";", pos, pos)), 0);

        // When: parsing empty declaration
        Result<IterationStep<DeclarationKeywordNode>> res =
                emptyStrategy.parse(
                        keyTok,
                        DeclarationType.LET,
                        idNode,
                        DataType.NUMBER,
                        stream,
                        asExprParser(new NumberLiteralParser()));

        // Then: parses successfully with variable name "x"
        assertTrue(res.isCorrect());
        DeclarationKeywordNode node =
                ((CorrectResult<IterationStep<DeclarationKeywordNode>>) res).value().value();
        assertEquals("x", node.identifierNode().name());
    }

    @Test
    @DisplayName(
            "What: GrammarRules and TokenMatchers | When: checking tokens | Then: matches expected"
                    + " predicates")
    void grammarRulesAndTokenMatchers_whenCheckingTokens_thenMatchesExpectedPredicates() {
        // What: GrammarRules and sample tokens
        GrammarRules r10 = GrammarRules.fromVersion(Version.V_1_0);
        assertNotNull(r10);
        GrammarRules r11 = GrammarRules.fromVersion(Version.V_1_1);
        assertNotNull(r11);

        Token let = new Token(TokenType.LET, "let", pos, pos);
        Token semi = new Token(TokenType.SEMICOLON, ";", pos, pos);

        // When & Then: TokenMatchers predicates
        assertTrue(TokenMatchers.isType(TokenType.LET).test(let));
        assertFalse(TokenMatchers.isType(TokenType.LET).test(semi));
        assertTrue(TokenMatchers.isTypeAndValue(TokenType.LET, "let").test(let));
        assertFalse(TokenMatchers.isTypeAndValue(TokenType.LET, "other").test(let));
        assertTrue(TokenMatchers.isOneOf(TokenType.LET, TokenType.CONST).test(let));
        assertFalse(TokenMatchers.isOneOf(TokenType.LET, TokenType.CONST).test(semi));
    }

    @Test
    @DisplayName(
            "What: ArgumentsParserUtils and BlockParserUtils | When: parsing separated list and"
                    + " block | Then: extracts elements correctly")
    void argumentsAndBlockParserUtils_whenParsingStreams_thenExtractsElementsCorrectly() {
        // What: parsers and tokens for empty and multi argument lists
        Parser<StringLiteralNode> strParser = new StringLiteralParser();
        Parser<Node> stmtParser = ParserFactory.createParser(Version.V_1_0);

        List<Token> emptyArgs =
                List.of(
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos));

        // When: parsing empty args list
        Result<IterationStep<List<StringLiteralNode>>> emptyRes =
                ArgumentsParserUtils.parseSeparatedList(
                        new TokenStreamAdapter(emptyArgs, 0),
                        strParser,
                        SymbolType.LPAREN,
                        SymbolType.RPAREN,
                        SymbolType.COMMA);

        // Then: returns empty list
        assertTrue(emptyRes.isCorrect());
        assertTrue(
                ((CorrectResult<IterationStep<List<StringLiteralNode>>>) emptyRes)
                        .value()
                        .value()
                        .isEmpty());

        // What: multi argument tokens ("a", "b")
        List<Token> multiArgs =
                List.of(
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"a\"", pos, pos),
                        new Token(TokenType.COMMA, ",", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"b\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos));

        // When: parsing separated list
        Result<IterationStep<List<StringLiteralNode>>> multiRes =
                ArgumentsParserUtils.parseSeparatedList(
                        new TokenStreamAdapter(multiArgs, 0),
                        strParser,
                        SymbolType.LPAREN,
                        SymbolType.RPAREN,
                        SymbolType.COMMA);

        // Then: returns 2 arguments
        assertTrue(multiRes.isCorrect());
        assertEquals(
                2,
                ((CorrectResult<IterationStep<List<StringLiteralNode>>>) multiRes)
                        .value()
                        .value()
                        .size());

        // What: block tokens { let x: number = 5; }
        List<Token> blockTokens =
                List.of(
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.LET, "let", pos, pos),
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.COLON, ":", pos, pos),
                        new Token(TokenType.NUMBER, "number", pos, pos),
                        new Token(TokenType.EQUAL, "=", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "5", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos));

        // When: parsing code block
        Result<IterationStep<List<Node>>> blockRes =
                BlockParserUtils.parseBlock(
                        new TokenStreamAdapter(blockTokens, 0),
                        stmtParser,
                        SymbolType.LBRACE,
                        SymbolType.RBRACE);

        // Then: block yields 1 statement node
        assertTrue(blockRes.isCorrect());
        assertEquals(
                1, ((CorrectResult<IterationStep<List<Node>>>) blockRes).value().value().size());
    }
}
