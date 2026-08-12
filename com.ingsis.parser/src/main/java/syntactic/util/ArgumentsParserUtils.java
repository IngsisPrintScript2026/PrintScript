package syntactic.util;

import iterator.IterationStep;
import node.Node;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import syntactic.Parser;
import token.SymbolType;
import token.Token;
import tokenstream.TokenStream;

import java.util.ArrayList;
import java.util.List;

public final class ArgumentsParserUtils {
    private ArgumentsParserUtils() {}

    public static <T extends Node> Result<IterationStep<List<T>>> parseSeparatedList(
            TokenStream stream,
            Parser<T> itemParser,
            SymbolType openSymbol,
            SymbolType closeSymbol,
            SymbolType separatorSymbol) {

        Result<IterationStep<Token>> openResult = stream.consume(openSymbol.tokenType());
        if (!openResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) openResult).error());
        }

        IterationStep<Token> openStep = ((CorrectResult<IterationStep<Token>>) openResult).value();
        TokenStream currentStream = (TokenStream) openStep.next();
        List<T> items = new ArrayList<>();

        Result<Token> peek = currentStream.peek(0);
        if (peek.isCorrect() && SymbolType.isSymbol(((CorrectResult<Token>) peek).value(), closeSymbol)) {
            Result<IterationStep<Token>> closeResult = currentStream.consume(closeSymbol.tokenType());
            IterationStep<Token> closeStep = ((CorrectResult<IterationStep<Token>>) closeResult).value();
            return Result.success(new IterationStep<>(items, (TokenStream) closeStep.next()));
        }

        while (true) {
            Result<IterationStep<T>> itemResult = itemParser.parse(currentStream);
            if (!itemResult.isCorrect()) {
                return Result.failure(((IncorrectResult<IterationStep<T>>) itemResult).error());
            }
            IterationStep<T> itemStep = ((CorrectResult<IterationStep<T>>) itemResult).value();
            items.add(itemStep.value());
            currentStream = (TokenStream) itemStep.next();

            Result<Token> nextPeek = currentStream.peek(0);
            if (!nextPeek.isCorrect()) {
                return Result.failure("Unexpected EOF reading arguments");
            }

            Token nextToken = ((CorrectResult<Token>) nextPeek).value();
            if (SymbolType.isSymbol(nextToken, separatorSymbol)) {
                Result<IterationStep<Token>> sepRes = currentStream.consume(separatorSymbol.tokenType());
                currentStream = (TokenStream) ((CorrectResult<IterationStep<Token>>) sepRes).value().next();
            } else if (SymbolType.isSymbol(nextToken, closeSymbol)) {
                Result<IterationStep<Token>> closeRes = currentStream.consume(closeSymbol.tokenType());
                IterationStep<Token> closeStep = ((CorrectResult<IterationStep<Token>>) closeRes).value();
                return Result.success(new IterationStep<>(items, (TokenStream) closeStep.next()));
            } else {
                return Result.failure("Expected ',' or ')' in argument list");
            }
        }
    }
}
