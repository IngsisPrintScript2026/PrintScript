/*
 * My Project
 */

package syntactic.util;

import iterator.IterationStep;
import java.util.ArrayList;
import java.util.List;
import node.StatementNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import token.Token;
import tokenstream.TokenStream;

public final class BlockParserUtils {
    private BlockParserUtils() {}

    public static Result<IterationStep<List<StatementNode>>> parseBlock(
            TokenStream stream,
            Parser<StatementNode> statementParser,
            SymbolType openBrace,
            SymbolType closeBrace) {
        Result<TokenStream> openRes = consumeSymbol(stream, openBrace);
        if (!openRes.isCorrect()) {
            return Result.failure(((IncorrectResult<TokenStream>) openRes).error());
        }
        return parseBlockStatements(
                ((CorrectResult<TokenStream>) openRes).value(),
                statementParser,
                closeBrace,
                new ArrayList<>());
    }

    private static Result<IterationStep<List<StatementNode>>> parseBlockStatements(
            TokenStream stream,
            Parser<StatementNode> parser,
            SymbolType closeBrace,
            List<StatementNode> statements) {
        if (isNextSymbol(stream, closeBrace)) {
            return finishBlock(stream, closeBrace, statements);
        }
        if (stream.isEmpty()) {
            return Result.failure("Expected '" + closeBrace.symbol() + "' at end of block");
        }
        Result<IterationStep<StatementNode>> stmtRes = parser.parse(stream);
        if (!stmtRes.isCorrect()) {
            return Result.failure(
                    ((IncorrectResult<IterationStep<StatementNode>>) stmtRes).error());
        }
        IterationStep<StatementNode> step =
                ((CorrectResult<IterationStep<StatementNode>>) stmtRes).value();
        statements.add(step.value());
        return parseBlockStatements((TokenStream) step.next(), parser, closeBrace, statements);
    }

    private static Result<TokenStream> consumeSymbol(TokenStream stream, SymbolType symbol) {
        Result<IterationStep<Token>> res = stream.consume(symbol.tokenType());
        if (!res.isCorrect())
            return Result.failure(((IncorrectResult<IterationStep<Token>>) res).error());
        return Result.success(
                (TokenStream) ((CorrectResult<IterationStep<Token>>) res).value().next());
    }

    private static boolean isNextSymbol(TokenStream stream, SymbolType symbol) {
        Result<Token> peek = stream.peek(0);
        return peek.isCorrect()
                && SymbolType.isSymbol(((CorrectResult<Token>) peek).value(), symbol);
    }

    private static Result<IterationStep<List<StatementNode>>> finishBlock(
            TokenStream stream, SymbolType closeBrace, List<StatementNode> statements) {
        Result<TokenStream> closeRes = consumeSymbol(stream, closeBrace);
        if (!closeRes.isCorrect())
            return Result.failure(((IncorrectResult<TokenStream>) closeRes).error());
        return Result.success(
                new IterationStep<>(statements, ((CorrectResult<TokenStream>) closeRes).value()));
    }
}
