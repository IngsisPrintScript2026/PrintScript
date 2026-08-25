package tokenstream;

import iterator.IterationStep;
import iterator.SafeIterator;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class LazyTokenStream implements TokenStream {

    private SafeIterator<Token> currentLexer;
    private final List<Token> buffer;
    private final int pointer;
    private boolean isEof;
    private String lexerError;

    public LazyTokenStream(SafeIterator<Token> lexer) {
        this(lexer, new ArrayList<>(), 0, false, null);
    }

    private LazyTokenStream(
            SafeIterator<Token> currentLexer,
            List<Token> buffer,
            int pointer,
            boolean isEof,
            String lexerError) {
        this.currentLexer = currentLexer;
        this.buffer = buffer;
        this.pointer = pointer;
        this.isEof = isEof;
        this.lexerError = lexerError;
    }

    private boolean fillBufferUpTo(int index) {
        while (buffer.size() <= index && !isEof && currentLexer != null) {
            Result<IterationStep<Token>> result = currentLexer.next();
            switch (result) {
                case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                    buffer.add(step.value());
                    currentLexer = step.nextStream();
                }
                case IncorrectResult<IterationStep<Token>>(String err) -> {
                    if (!"EOF".equalsIgnoreCase(err)) {
                        lexerError = err;
                    }
                    isEof = true;
                }
            }
            if (isEof) {
                break;
            }
        }
        return buffer.size() > index;
    }

    @Override
    public Result<IterationStep<Token>> next() {
        return consume();
    }

    @Override
    public Result<IterationStep<Token>> consume() {
        if (!fillBufferUpTo(pointer)) {
            if (lexerError != null) {
                return Result.failure("Lexical error: " + lexerError);
            }
            return Result.failure("EOF: Se alcanzó el fin del flujo de tokens.");
        }
        Token currentToken = buffer.get(pointer);
        TokenStream nextStream = new LazyTokenStream(currentLexer, buffer, pointer + 1, isEof, lexerError);
        return Result.success(new IterationStep<>(currentToken, nextStream));
    }

    @Override
    public Result<IterationStep<Token>> consume(TokenType expectedType) {
        return consume(token -> token != null && token.type() == expectedType);
    }

    @Override
    public Result<IterationStep<Token>> consume(Predicate<Token> matcher) {
        return switch (peek(0)) {
            case IncorrectResult<Token>(String err) -> Result.failure(err);
            case CorrectResult<Token>(Token token) -> matcher.test(token)
                    ? consume()
                    : Result.failure("Token inesperado: " + token);
        };
    }

    @Override
    public Result<Token> peek(int offset) {
        int targetIndex = pointer + offset;
        if (targetIndex < 0) {
            return Result.failure("Índice fuera de los límites del stream: " + targetIndex);
        }
        if (!fillBufferUpTo(targetIndex)) {
            if (lexerError != null) {
                return Result.failure("Lexical error: " + lexerError);
            }
            return Result.failure("Índice fuera de los límites del stream: " + targetIndex);
        }
        return Result.success(buffer.get(targetIndex));
    }

    @Override
    public boolean isEmpty() {
        return !fillBufferUpTo(pointer);
    }

    @Override
    public int pointer() {
        return pointer;
    }
}
