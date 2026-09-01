/*
 * My Project
 */

package syntactic;

import static org.junit.jupiter.api.Assertions.*;

import iterator.IterationStep;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.StringLiteralNode;
import node.keyword.AssignNode;
import node.keyword.DeclarationKeywordNode;
import node.keyword.IfKeywordNode;
import node.keyword.declaration.DeclarationType;
import org.junit.jupiter.api.Test;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.parser.ParserFactory;
import syntactic.parser.literal.BooleanLiteralParser;
import syntactic.parser.literal.IdentifierParser;
import syntactic.parser.literal.NumberLiteralParser;
import syntactic.parser.literal.StringLiteralParser;
import syntactic.parser.root.AssignParser;
import syntactic.parser.root.ConditionalParser;
import syntactic.parser.root.FunctionParser;
import syntactic.parser.root.LineExpressionParser;
import syntactic.strategy.EmptyDeclarationSymbolStrategy;
import syntactic.strategy.WithElseStrategy;
import syntactic.strategy.WithoutElseStrategy;
import syntactic.util.ArgumentsParserUtils;
import syntactic.util.BlockParserUtils;
import syntactic.version.Version10Strategy;
import syntactic.version.Version11Strategy;
import syntactic.version.VersionStrategyRegistry;
import token.SymbolType;
import token.Token;
import token.TokenType;
import tokenstream.TokenStream;
import tokenstream.TokenStreamAdapter;
import tokenstream.rules.TokenMatchers;
import tokenstream.version.GrammarRules;
import version.Version;

class SyntacticGrammarAndParsersTest {

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
    void testVersionStrategiesAndFactory() {
        VersionStrategyRegistry registry = new VersionStrategyRegistry();
        assertNotNull(registry.getStrategy(Version.V_1_0));
        assertNotNull(registry.getStrategy(Version.V_1_1));

        Parser<Node> parser10 = ParserFactory.createParser(Version.V_1_0);
        assertNotNull(parser10);
        Parser<Node> parser11 = ParserFactory.createParser(Version.V_1_1);
        assertNotNull(parser11);

        Version10Strategy v10 = new Version10Strategy();
        assertEquals(Version.V_1_0, v10.version());
        assertNotNull(v10.declarationKeywords());
        assertNotNull(v10.supportedDataTypes());
        assertNotNull(
                v10.primaryParsers(
                        new NumberLiteralParser(),
                        new StringLiteralParser(),
                        new BooleanLiteralParser(),
                        new FunctionParser(
                                new IdentifierParser(), asExprParser(new NumberLiteralParser())),
                        new IdentifierParser()));

        Version11Strategy v11 = new Version11Strategy();
        assertEquals(Version.V_1_1, v11.version());
        assertNotNull(v11.declarationKeywords());
        assertNotNull(v11.supportedDataTypes());
        assertNotNull(
                v11.primaryParsers(
                        new NumberLiteralParser(),
                        new StringLiteralParser(),
                        new BooleanLiteralParser(),
                        new FunctionParser(
                                new IdentifierParser(), asExprParser(new NumberLiteralParser())),
                        new IdentifierParser()));
    }

    @Test
    void testAssignParser() {
        AssignParser assignParser =
                new AssignParser(new IdentifierParser(), asExprParser(new NumberLiteralParser()));

        // x = 10;
        List<Token> tokens =
                List.of(
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.EQUAL, "=", pos, pos),
                        new Token(TokenType.NUMBER_LITERAL, "10", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        TokenStreamAdapter stream = new TokenStreamAdapter(tokens, 0);

        Result<IterationStep<AssignNode>> result = assignParser.parse(stream);
        assertTrue(result.isCorrect());
        AssignNode node = ((CorrectResult<IterationStep<AssignNode>>) result).value().value();
        assertEquals("x", node.identifierNode().name());

        // Error: missing equal
        List<Token> badTokens =
                List.of(
                        new Token(TokenType.IDENTIFIER, "x", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        assertFalse(assignParser.parse(new TokenStreamAdapter(badTokens, 0)).isCorrect());
    }

    @Test
    void testFunctionParser() {
        FunctionParser functionParser =
                new FunctionParser(new IdentifierParser(), asExprParser(new StringLiteralParser()));

        // println("hello");
        List<Token> tokens =
                List.of(
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"hello\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        TokenStreamAdapter stream = new TokenStreamAdapter(tokens, 0);

        Result<IterationStep<CallFunctionNode>> result = functionParser.parse(stream);
        assertTrue(result.isCorrect());
        CallFunctionNode node =
                ((CorrectResult<IterationStep<CallFunctionNode>>) result).value().value();
        assertEquals("println", node.identifierNode().name());

        // Error: missing rparen
        List<Token> badTokens =
                List.of(
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"hello\"", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos));
        assertFalse(functionParser.parse(new TokenStreamAdapter(badTokens, 0)).isCorrect());
    }

    @Test
    void testConditionalParserWithAndWithoutElse() {
        Parser<Node> stmtParser = ParserFactory.createParser(Version.V_1_1);
        Parser<ExpressionNode> boolParser = asExprParser(new BooleanLiteralParser());
        ConditionalParser parserWithElse =
                new ConditionalParser(
                        boolParser,
                        stmtParser,
                        List.of(new WithElseStrategy(), new WithoutElseStrategy()));
        ConditionalParser parserWithoutElse = new ConditionalParser(boolParser, stmtParser);

        // if (true) { println("yes"); } else { println("no"); }
        List<Token> tokensWithElse =
                List.of(
                        new Token(TokenType.IF, "if", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.BOOLEAN_LITERAL, "true", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"yes\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos),
                        new Token(TokenType.ELSE, "else", pos, pos),
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"no\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos));

        Result<IterationStep<IfKeywordNode>> resWithElse =
                parserWithElse.parse(new TokenStreamAdapter(tokensWithElse, 0));
        assertTrue(resWithElse.isCorrect());
        IfKeywordNode node =
                ((CorrectResult<IterationStep<IfKeywordNode>>) resWithElse).value().value();
        assertEquals(1, node.thenBody().size());
        assertEquals(1, node.elseBody().size());

        // if (true) { println("yes"); }
        List<Token> tokensWithoutElse =
                List.of(
                        new Token(TokenType.IF, "if", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.BOOLEAN_LITERAL, "true", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.LBRACE, "{", pos, pos),
                        new Token(TokenType.PRINTLN, "println", pos, pos),
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"yes\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos),
                        new Token(TokenType.SEMICOLON, ";", pos, pos),
                        new Token(TokenType.RBRACE, "}", pos, pos));

        Result<IterationStep<IfKeywordNode>> resWithoutElse =
                parserWithoutElse.parse(new TokenStreamAdapter(tokensWithoutElse, 0));
        assertTrue(resWithoutElse.isCorrect());

        Result<IterationStep<IfKeywordNode>> resWithElseAbsent =
                parserWithElse.parse(new TokenStreamAdapter(tokensWithoutElse, 0));
        assertTrue(resWithElseAbsent.isCorrect());

        // Test WithoutElseStrategy matches directly
        WithoutElseStrategy withoutElse = new WithoutElseStrategy();
        assertTrue(withoutElse.matches(new TokenStreamAdapter(List.of(), 0)));
        assertTrue(withoutElse.matches(new TokenStreamAdapter(tokensWithoutElse, 0)));
        assertFalse(
                withoutElse.matches(
                        new TokenStreamAdapter(
                                List.of(new Token(TokenType.ELSE, "else", pos, pos)), 0)));
    }

    @Test
    void testLiteralParsers() {
        BooleanLiteralParser boolParser = new BooleanLiteralParser();
        TokenStreamAdapter boolStream =
                new TokenStreamAdapter(
                        List.of(new Token(TokenType.BOOLEAN_LITERAL, "true", pos, pos)), 0);
        Result<IterationStep<BooleanLiteralNode>> boolRes = boolParser.parse(boolStream);
        assertTrue(boolRes.isCorrect());
        assertEquals(
                true,
                ((CorrectResult<IterationStep<BooleanLiteralNode>>) boolRes)
                        .value()
                        .value()
                        .rawValue());

        StringLiteralParser strParser = new StringLiteralParser();
        TokenStreamAdapter strStream =
                new TokenStreamAdapter(
                        List.of(new Token(TokenType.STRING_LITERAL, "\"hello\"", pos, pos)), 0);
        Result<IterationStep<StringLiteralNode>> strRes = strParser.parse(strStream);
        assertTrue(strRes.isCorrect());
        assertEquals(
                "hello",
                ((CorrectResult<IterationStep<StringLiteralNode>>) strRes)
                        .value()
                        .value()
                        .rawValue());

        StringLiteralParser strSingleQuoteParser = new StringLiteralParser();
        TokenStreamAdapter strSingleQuoteStream =
                new TokenStreamAdapter(
                        List.of(new Token(TokenType.STRING_LITERAL, "'world'", pos, pos)), 0);
        Result<IterationStep<StringLiteralNode>> singleRes =
                strSingleQuoteParser.parse(strSingleQuoteStream);
        assertTrue(singleRes.isCorrect());
        assertEquals(
                "world",
                ((CorrectResult<IterationStep<StringLiteralNode>>) singleRes)
                        .value()
                        .value()
                        .rawValue());

        LineExpressionParser lineExprParser = new LineExpressionParser(asExprParser(strParser));
        TokenStreamAdapter lineStream =
                new TokenStreamAdapter(
                        List.of(
                                new Token(TokenType.STRING_LITERAL, "\"line\"", pos, pos),
                                new Token(TokenType.SEMICOLON, ";", pos, pos)),
                        0);
        assertTrue(lineExprParser.parse(lineStream).isCorrect());
    }

    @Test
    void testDeclarationStrategies() {
        EmptyDeclarationSymbolStrategy emptyStrategy = new EmptyDeclarationSymbolStrategy();
        assertEquals(SymbolType.SEMICOLON, emptyStrategy.targetSymbol());

        Token keyTok = new Token(TokenType.LET, "let", pos, pos);
        IdentifierNode idNode = new IdentifierNode("x", 1, 1);
        TokenStream stream =
                new TokenStreamAdapter(List.of(new Token(TokenType.SEMICOLON, ";", pos, pos)), 0);

        Result<IterationStep<DeclarationKeywordNode>> res =
                emptyStrategy.parse(
                        keyTok,
                        DeclarationType.LET,
                        idNode,
                        DataType.NUMBER,
                        stream,
                        asExprParser(new NumberLiteralParser()));
        assertTrue(res.isCorrect());
        DeclarationKeywordNode node =
                ((CorrectResult<IterationStep<DeclarationKeywordNode>>) res).value().value();
        assertEquals("x", node.identifierNode().name());
    }

    @Test
    void testGrammarRulesAndTokenMatchers() {
        GrammarRules r10 = GrammarRules.fromVersion(Version.V_1_0);
        assertNotNull(r10);
        GrammarRules r11 = GrammarRules.fromVersion(Version.V_1_1);
        assertNotNull(r11);

        Token let = new Token(TokenType.LET, "let", pos, pos);
        Token semi = new Token(TokenType.SEMICOLON, ";", pos, pos);

        assertTrue(TokenMatchers.isType(TokenType.LET).test(let));
        assertFalse(TokenMatchers.isType(TokenType.LET).test(semi));
        assertTrue(TokenMatchers.isTypeAndValue(TokenType.LET, "let").test(let));
        assertFalse(TokenMatchers.isTypeAndValue(TokenType.LET, "other").test(let));
        assertTrue(TokenMatchers.isOneOf(TokenType.LET, TokenType.CONST).test(let));
        assertFalse(TokenMatchers.isOneOf(TokenType.LET, TokenType.CONST).test(semi));
    }

    @Test
    void testArgumentsAndBlockParserUtils() {
        Parser<StringLiteralNode> strParser = new StringLiteralParser();
        Parser<Node> stmtParser = ParserFactory.createParser(Version.V_1_0);

        // Empty args: ()
        List<Token> emptyArgs =
                List.of(
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos));
        Result<IterationStep<List<StringLiteralNode>>> emptyRes =
                ArgumentsParserUtils.parseSeparatedList(
                        new TokenStreamAdapter(emptyArgs, 0),
                        strParser,
                        SymbolType.LPAREN,
                        SymbolType.RPAREN,
                        SymbolType.COMMA);
        assertTrue(emptyRes.isCorrect());
        assertTrue(
                ((CorrectResult<IterationStep<List<StringLiteralNode>>>) emptyRes)
                        .value()
                        .value()
                        .isEmpty());

        // Non-empty args: ("a", "b")
        List<Token> multiArgs =
                List.of(
                        new Token(TokenType.LPAREN, "(", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"a\"", pos, pos),
                        new Token(TokenType.COMMA, ",", pos, pos),
                        new Token(TokenType.STRING_LITERAL, "\"b\"", pos, pos),
                        new Token(TokenType.RPAREN, ")", pos, pos));
        Result<IterationStep<List<StringLiteralNode>>> multiRes =
                ArgumentsParserUtils.parseSeparatedList(
                        new TokenStreamAdapter(multiArgs, 0),
                        strParser,
                        SymbolType.LPAREN,
                        SymbolType.RPAREN,
                        SymbolType.COMMA);
        assertTrue(multiRes.isCorrect());
        assertEquals(
                2,
                ((CorrectResult<IterationStep<List<StringLiteralNode>>>) multiRes)
                        .value()
                        .value()
                        .size());

        // Block parser: { let x: number = 5; }
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
        Result<IterationStep<List<Node>>> blockRes =
                BlockParserUtils.parseBlock(
                        new TokenStreamAdapter(blockTokens, 0),
                        stmtParser,
                        SymbolType.LBRACE,
                        SymbolType.RBRACE);
        assertTrue(blockRes.isCorrect());
        assertEquals(
                1, ((CorrectResult<IterationStep<List<Node>>>) blockRes).value().value().size());
    }
}
