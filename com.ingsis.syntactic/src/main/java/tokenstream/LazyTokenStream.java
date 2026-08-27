package tokenstream;

import iterator.IterationStep;
import iterator.SafeIterator;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

import java.util.function.Predicate;

public final class LazyTokenStream implements TokenStream {

    private static class Node {
        final Token token;
        final String error;
        final boolean isEof;
        private SafeIterator<Token> lexer;
        private Node next;

        Node(Token token, SafeIterator<Token> lexer) {
            this.token = token;
            this.lexer = lexer;
            this.error = null;
            this.isEof = false;
        }

        Node(String error, boolean isEof) {
            this.token = null;
            this.lexer = null;
            this.error = error;
            this.isEof = isEof;
        }

        synchronized Node nextNode() {
            if (next == null) {
                if (lexer == null) {
                    next = new Node("EOF: Se alcanzó el fin del flujo de tokens.", true);
                } else {
                    Result<IterationStep<Token>> res = lexer.next();
                    switch (res) {
                        case CorrectResult<IterationStep<Token>>(IterationStep<Token> step) -> {
                            next = new Node(step.value(), step.nextStream());
                        }
                        case IncorrectResult<IterationStep<Token>>(String err) -> {
                            boolean eof = "EOF".equalsIgnoreCase(err) || err.contains("EOF");
                            next = new Node(err, eof);
                        }
                    }
                    lexer = null;
                }
            }
            return next;
        }
    }

    private final Node currentNode;
    private final int pointer;

    public LazyTokenStream(SafeIterator<Token> lexer) {
        this(new Node(null, lexer).nextNode(), 0);
    }

    private LazyTokenStream(Node currentNode, int pointer) {
        this.currentNode = currentNode;
        this.pointer = pointer;
    }

    @Override
    public Result<IterationStep<Token>> next() {
        return consume();
    }

    @Override
    public Result<IterationStep<Token>> consume() {
        if (currentNode == null || currentNode.token == null) {
            if (currentNode != null && currentNode.error != null && !currentNode.isEof) {
                return Result.failure("Lexical error: " + currentNode.error);
            }
            return Result.failure("EOF: Se alcanzó el fin del flujo de tokens.");
        }
        Token currentToken = currentNode.token;
        TokenStream nextStream = new LazyTokenStream(currentNode.nextNode(), pointer + 1);
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
        if (offset < 0) {
            return Result.failure("Índice fuera de los límites del stream: " + offset);
        }
        Node curr = currentNode;
        for (int i = 0; i < offset; i++) {
            if (curr == null || curr.isEof) {
                return Result.failure("Índice fuera de los límites del stream: " + offset);
            }
            curr = curr.nextNode();
        }
        if (curr == null || curr.token == null) {
            if (curr != null && curr.error != null && !curr.isEof) {
                return Result.failure("Lexical error: " + curr.error);
            }
            return Result.failure("Índice fuera de los límites del stream: " + offset);
        }
        return Result.success(curr.token);
    }

    @Override
    public boolean isEmpty() {
        return currentNode == null || currentNode.token == null;
    }

    @Override
    public int pointer() {
        return pointer;
    }
}



