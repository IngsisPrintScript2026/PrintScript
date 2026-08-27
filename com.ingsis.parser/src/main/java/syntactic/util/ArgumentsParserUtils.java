/*
 * My Project
 */

package syntactic.util;

import iterator.IterationStep;
import java.util.ArrayList;
import java.util.List;
import node.Node;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import token.Token;
import tokenstream.TokenStream;

public final class ArgumentsParserUtils {
    private ArgumentsParserUtils() {}

    public static <T extends Node> Result<IterationStep<List<T>>> parseSeparatedList(
            TokenStream stream,
            Parser<T> itemParser,
            SymbolType openSymbol,
            SymbolType closeSymbol,
            SymbolType separatorSymbol) {
        Result<TokenStream> openRes = consumeSymbol(stream, openSymbol);
        if (!openRes.isCorrect())
            return Result.failure(((IncorrectResult<TokenStream>) openRes).error());
        TokenStream currentStream = ((CorrectResult<TokenStream>) openRes).value();
        if (isNextSymbol(currentStream, closeSymbol))
            return consumeClose(currentStream, closeSymbol, new ArrayList<>());
        return parseItems(
                currentStream, itemParser, closeSymbol, separatorSymbol, new ArrayList<>());
    }

    private static <T extends Node> Result<IterationStep<List<T>>> parseItems(
            TokenStream stream, Parser<T> parser, SymbolType close, SymbolType sep, List<T> items) {
        Result<IterationStep<T>> itemRes = parser.parse(stream);
        if (!itemRes.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<T>>) itemRes).error());
        }
        IterationStep<T> step = ((CorrectResult<IterationStep<T>>) itemRes).value();
        items.add(step.value());
        TokenStream nextStream = (TokenStream) step.next();
        if (isNextSymbol(nextStream, close)) {
            return consumeClose(nextStream, close, items);
        }
        if (isNextSymbol(nextStream, sep)) {
            return parseItems(advanceAfterSeparator(nextStream, sep), parser, close, sep, items);
        }
        return Result.failure("Expected ',' or ')' in argument list");
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

    private static <T extends Node> Result<IterationStep<List<T>>> consumeClose(
            TokenStream stream, SymbolType closeSymbol, List<T> items) {
        Result<TokenStream> closeRes = consumeSymbol(stream, closeSymbol);
        if (!closeRes.isCorrect())
            return Result.failure(((IncorrectResult<TokenStream>) closeRes).error());
        return Result.success(
                new IterationStep<>(items, ((CorrectResult<TokenStream>) closeRes).value()));
    }

    private static TokenStream advanceAfterSeparator(TokenStream stream, SymbolType sep) {
        Result<IterationStep<Token>> res = stream.consume(sep.tokenType());
        return (TokenStream) ((CorrectResult<IterationStep<Token>>) res).value().next();
    }
}
