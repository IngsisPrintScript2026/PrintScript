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

public final class BlockParserUtils {
    private BlockParserUtils() {}

    public static Result<IterationStep<List<Node>>> parseBlock(
            TokenStream stream,
            Parser<Node> statementParser,
            SymbolType openBrace,
            SymbolType closeBrace) {

        Result<IterationStep<Token>> openResult = stream.consume(openBrace.tokenType());
        if (!openResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) openResult).error());
        }

        IterationStep<Token> openStep = ((CorrectResult<IterationStep<Token>>) openResult).value();
        TokenStream currentStream = (TokenStream) openStep.next();
        List<Node> statements = new ArrayList<>();

        while (true) {
            Result<Token> peekResult = currentStream.peek(0);
            if (peekResult.isCorrect() && SymbolType.isSymbol(((CorrectResult<Token>) peekResult).value(), closeBrace)) {
                break;
            }
            if (currentStream.isEmpty()) {
                return Result.failure("Expected '" + closeBrace.symbol() + "' at end of block");
            }
            Result<IterationStep<Node>> stmtResult = statementParser.parse(currentStream);
            if (!stmtResult.isCorrect()) {
                return Result.failure(((IncorrectResult<IterationStep<Node>>) stmtResult).error());
            }
            IterationStep<Node> stmtStep = ((CorrectResult<IterationStep<Node>>) stmtResult).value();
            statements.add(stmtStep.value());
            currentStream = (TokenStream) stmtStep.next();
        }

        Result<IterationStep<Token>> closeResult = currentStream.consume(closeBrace.tokenType());
        if (!closeResult.isCorrect()) {
            return Result.failure(((IncorrectResult<IterationStep<Token>>) closeResult).error());
        }

        IterationStep<Token> closeStep = ((CorrectResult<IterationStep<Token>>) closeResult).value();
        return Result.success(new IterationStep<>(statements, (TokenStream) closeStep.next()));
    }
}
