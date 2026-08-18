package lexer;

import iterator.IterationStep;
import iterator.SafeIterator;
import metaChar.MetaCharacter;
import metaChar.MetaCharStringBuilder;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.tokenizer.Tokenizer;
import token.tokenize.TokenizeResult;

import java.util.ArrayList;
import java.util.List;

public class Lexer implements SafeIterator<Token> {
    private final SafeIterator<MetaCharacter> charIterator;
    private final Tokenizer tokenizer;

    public Lexer(SafeIterator<MetaCharacter> charIterator, Tokenizer tokenizer) {
        this.charIterator = charIterator;
        this.tokenizer = tokenizer;
    }

    public Lexer(SafeIterator<MetaCharacter> charIterator) {
        this(charIterator, new PrintScriptTokenizer());
    }

    @Override
    public Result<IterationStep<Token>> next() {
        return maximalMunchOf(charIterator);
    }

    private Result<IterationStep<Token>> maximalMunchOf(SafeIterator<MetaCharacter> stream) {
        SafeIterator<MetaCharacter> curr = stream;
        Result<IterationStep<MetaCharacter>> result = curr.next();

        boolean skippingWhitespace = true;
        while (skippingWhitespace) {
            switch (result) {
                case CorrectResult<IterationStep<MetaCharacter>>(IterationStep<MetaCharacter> value)
                        when Character.isWhitespace(value.value().character()) -> {
                    curr = (SafeIterator<MetaCharacter>) value.next();
                    result = curr.next();
                }
                default -> skippingWhitespace = false;
            }
        }

        return switch (result) {
            case IncorrectResult<IterationStep<MetaCharacter>> failure -> Result.failure("EOF");
            case CorrectResult<IterationStep<MetaCharacter>> success -> runMaximalMunchLoop(curr, result);
        };
    }

    private Result<IterationStep<Token>> runMaximalMunchLoop(
            SafeIterator<MetaCharacter> curr,
            Result<IterationStep<MetaCharacter>> initialResult) {

        MunchState state = executeMunch(initialResult);
        unreadLookaheadBuffer(curr, state.lookaheadBuffer);
        return buildResult(state.lastValidToken, state.nextStreamAfterToken);
    }

    private MunchState executeMunch(Result<IterationStep<MetaCharacter>> initialResult) {
        MunchState state = new MunchState();
        MetaCharStringBuilder sb = new MetaCharStringBuilder();
        Result<IterationStep<MetaCharacter>> result = initialResult;

        boolean processing = true;
        while (processing) {
            switch (result) {
                case CorrectResult<IterationStep<MetaCharacter>>(IterationStep<MetaCharacter> step) -> {
                    sb.append(step.value());
                    TokenizeResult tr = tokenizer.tokenize(sb);

                    if (processTokenizeStep(tr, step, state)) {
                        processing = false;
                    } else {
                        result = ((SafeIterator<MetaCharacter>) step.next()).next();
                    }
                }
                case IncorrectResult<IterationStep<MetaCharacter>> failure -> processing = false;
            }
        }
        return state;
    }

    private boolean processTokenizeStep(
            TokenizeResult tr,
            IterationStep<MetaCharacter> step,
            MunchState state) {

        return switch (tr) {
            case TokenizeResult.Complete(Token token) -> {
                state.lastValidToken = token;
                state.nextStreamAfterToken = (SafeIterator<MetaCharacter>) step.next();
                state.lookaheadBuffer.clear();
                yield false;
            }
            case TokenizeResult.Prefix() -> {
                state.lookaheadBuffer.add(step.value());
                yield false;
            }
            case TokenizeResult.Invalid invalid -> {
                state.lookaheadBuffer.add(step.value());
                yield true;
            }
        };
    }

    private void unreadLookaheadBuffer(SafeIterator<MetaCharacter> stream, List<MetaCharacter> buffer) {
        for (int i = buffer.size() - 1; i >= 0; i--) {
            stream.unread(buffer.get(i));
        }
    }

    private Result<IterationStep<Token>> buildResult(
            Token token,
            SafeIterator<MetaCharacter> nextStream) {
        if (token == null) {
            return Result.failure("Error léxico: Imposible reconocer un token válido");
        }
        return Result.success(new IterationStep<>(token, new Lexer(nextStream, tokenizer)));
    }
}
