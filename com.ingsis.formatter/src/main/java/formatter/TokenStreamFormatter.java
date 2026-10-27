/*
 * My Project
 */

package formatter;

import charstream.CharStream;
import charstream.StreamCharReader;
import formatter.rule.BracePositionRule;
import formatter.rule.FormattingRule;
import formatter.rule.IndentationRule;
import formatter.rule.LineBreakAfterStatementRule;
import formatter.rule.LinesBeforePrintlnRule;
import formatter.rule.SingleSpaceSeparationRule;
import formatter.rule.SpaceAfterColonRule;
import formatter.rule.SpaceAfterCommaRule;
import formatter.rule.SpaceAroundEqualsRule;
import formatter.rule.SpaceAroundOperatorsRule;
import formatter.rule.SpaceBeforeColonRule;
import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lexer.Lexer;
import node.Node;
import node.ProgramNode;
import position.Position;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

/**
 * Formats source code as a stream of tokens: each token is written as soon as it is read, so the
 * whole program never has to fit in memory. The whitespace between two tokens is rebuilt from their
 * positions and then rewritten by the {@link FormattingRule}s, applied in order.
 */
public class TokenStreamFormatter implements Formatter {
    private final List<FormattingRule> rules;
    private final FormatContext context;

    public TokenStreamFormatter(FormatContext context, List<FormattingRule> rules) {
        this.context = context != null ? context : new FormatContext();
        this.rules = rules;
    }

    public TokenStreamFormatter(FormatContext context) {
        this(
                context,
                List.of(
                        new SingleSpaceSeparationRule(),
                        new SpaceBeforeColonRule(),
                        new SpaceAfterColonRule(),
                        new SpaceAroundEqualsRule(),
                        new SpaceAroundOperatorsRule(),
                        new SpaceAfterCommaRule(),
                        new LineBreakAfterStatementRule(),
                        new LinesBeforePrintlnRule(),
                        new BracePositionRule()));
    }

    public TokenStreamFormatter() {
        this(new FormatContext());
    }

    @Override
    public Result<String> format(ProgramNode program) {
        return Result.failure("TokenStreamFormatter formats directly from input stream");
    }

    @Override
    public Result<String> formatNode(Node node) {
        return Result.failure("TokenStreamFormatter formats directly from input stream");
    }

    /** Convenience for small inputs: returns the formatted source as a string. */
    public Result<String> format(InputStream in) {
        StringWriter writer = new StringWriter();
        Result<String> res = format(in, writer);
        return res.isCorrect() ? Result.success(writer.toString()) : res;
    }

    public Result<String> format(InputStream in, Writer writer) {
        StreamCharReader reader =
                new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        return format(new Lexer(new CharStream(reader)), writer);
    }

    public Result<String> format(SafeIterator<Token> tokens, Writer writer) {
        try {
            return formatTokens(tokens, writer);
        } catch (IOException e) {
            return Result.failure("Formatting error: " + e.getMessage());
        }
    }

    private Result<String> formatTokens(SafeIterator<Token> tokens, Writer writer)
            throws IOException {
        FormattingState state = new FormattingState();
        Result<IterationStep<Token>> next = tokens.next();
        while (next instanceof CorrectResult<IterationStep<Token>>(var step)) {
            writeToken(step.value(), state, writer);
            next = step.<SafeIterator<Token>>nextStream().next();
        }
        if (next instanceof IncorrectResult<IterationStep<Token>>(String err)
                && !"EOF".equals(err)) {
            writer.flush();
            return Result.failure("Formatting error: " + err);
        }
        if (state.prev != null) {
            writer.write("\n");
        }
        writer.flush();
        return Result.success("Formatting completed");
    }

    private void writeToken(Token current, FormattingState state, Writer writer)
            throws IOException {
        if (state.prev != null) {
            String original = separatorBetween(state.prev, current);
            writer.write(computeSeparator(state.prev, current, original, state));
        }
        updateStateForToken(current, state);
        writer.write(current.value());
        state.prev = current;
    }

    /** Rebuilds the whitespace the lexer skipped between two tokens from their positions. */
    static String separatorBetween(Token prev, Token current) {
        Position end = prev.range().end();
        Position start = current.startPosition();
        int lineBreaks = start.line() - end.line();
        if (lineBreaks > 0) {
            return "\n".repeat(lineBreaks) + " ".repeat(Math.max(0, start.column() - 1));
        }
        return " ".repeat(Math.max(0, start.column() - end.column() - 1));
    }

    private String computeSeparator(
            Token prev, Token current, String originalSep, FormattingState state) {
        String sep = resolvePrintlnSeparator(originalSep, state);
        for (FormattingRule rule : rules) {
            if (rule.applies(prev, current, context)) {
                sep = rule.formatSeparator(prev, current, sep, context);
            }
        }
        if (state.indentRule.applies(prev, current, context)) {
            state.indentRule.setDepth(state.depth);
            sep = state.indentRule.formatSeparator(prev, current, sep, context);
        }
        return sep;
    }

    private String resolvePrintlnSeparator(String originalSep, FormattingState state) {
        if (state.justFinishedPrintln && context.lineBreaksAfterPrintln() != null) {
            state.justFinishedPrintln = false;
            return "\n".repeat(context.lineBreaksAfterPrintln() + 1);
        }
        return originalSep;
    }

    private void updateStateForToken(Token current, FormattingState state) {
        if (current.type() == TokenType.LBRACE) {
            state.depth++;
        } else if (current.type() == TokenType.RBRACE) {
            state.depth = Math.max(0, state.depth - 1);
        }
        if (current.type() == TokenType.PRINTLN) {
            state.stmtHasPrintln = true;
        }
        if (current.type() == TokenType.SEMICOLON && state.stmtHasPrintln) {
            state.justFinishedPrintln = true;
            state.stmtHasPrintln = false;
        }
    }

    private static class FormattingState {
        Token prev;
        int depth = 0;
        boolean justFinishedPrintln = false;
        boolean stmtHasPrintln = false;
        final IndentationRule indentRule = new IndentationRule();
    }
}
