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

    public record Delimiters(
            SymbolType openSymbol, SymbolType closeSymbol, SymbolType separatorSymbol) {}

    public static <T extends Node> Result<IterationStep<List<T>>> parseSeparatedList(
            TokenStream stream, Parser<T> itemParser, Delimiters delimiters) {
        Result<TokenStream> openRes = consumeSymbol(stream, delimiters.openSymbol());
        if (!openRes.isCorrect()) {
            return Result.failure(((IncorrectResult<TokenStream>) openRes).error());
        }
        TokenStream currentStream = ((CorrectResult<TokenStream>) openRes).value();
        if (isNextSymbol(currentStream, delimiters.closeSymbol())) {
            return consumeClose(currentStream, delimiters.closeSymbol(), new ArrayList<>());
        }
        return parseItems(currentStream, itemParser, delimiters, new ArrayList<>());
    }

    private static <T extends Node> Result<IterationStep<List<T>>> parseItems(
            TokenStream stream, Parser<T> parser, Delimiters delims, List<T> items) {
        Result<IterationStep<T>> itemRes = parser.parse(stream);
        if (!itemRes.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<T>>) itemRes).error());
        }
        IterationStep<T> step = ((CorrectResult<IterationStep<T>>) itemRes).value();
        items.add(step.value());
        TokenStream nextStream = (TokenStream) step.next();
        if (isNextSymbol(nextStream, delims.closeSymbol())) {
            return consumeClose(nextStream, delims.closeSymbol(), items);
        }
        if (isNextSymbol(nextStream, delims.separatorSymbol())) {
            return parseItems(
                    advanceAfterSeparator(nextStream, delims.separatorSymbol()),
                    parser,
                    delims,
                    items);
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
