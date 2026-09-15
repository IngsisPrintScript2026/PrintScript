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
import formatter.rule.SingleSpaceSeparationRule;
import formatter.rule.SpaceAfterColonRule;
import formatter.rule.SpaceAroundEqualsRule;
import formatter.rule.SpaceAroundOperatorsRule;
import formatter.rule.SpaceBeforeColonRule;
import iterator.IterationStep;
import iterator.SafeIterator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lexer.Lexer;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

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
                        new SpaceAfterColonRule(),
                        new SpaceBeforeColonRule(),
                        new SpaceAroundEqualsRule(),
                        new SpaceAroundOperatorsRule(),
                        new LineBreakAfterStatementRule(),
                        new SingleSpaceSeparationRule(),
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

    public Result<String> format(InputStream in, Writer writer) {
        try {
            String sourceCode = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<Token> tokens = tokenize(sourceCode);
            if (tokens.isEmpty()) {
                return writeResult(sourceCode, writer);
            }
            String formatted = applyFormatting(sourceCode, tokens);
            return writeResult(formatted, writer);
        } catch (Exception e) {
            return Result.failure("Formatting error: " + e.getMessage());
        }
    }

    private List<Token> tokenize(String sourceCode) {
        List<Token> tokens = new ArrayList<>();
        try (StringReader sr = new StringReader(sourceCode);
                StreamCharReader reader = new StreamCharReader(sr)) {
            SafeIterator<Token> lexer = new Lexer(new CharStream(reader));
            collectTokens(lexer, tokens);
        } catch (Exception ignored) {
        }
        return tokens;
    }

    private void collectTokens(SafeIterator<Token> lexer, List<Token> tokens) {
        SafeIterator<Token> current = lexer;
        while (true) {
            Result<IterationStep<Token>> res = current.next();
            if (!res.isCorrect()) {
                break;
            }
            IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) res).value();
            tokens.add(step.value());
            current = step.nextStream();
        }
    }

    private int[][] computeOffsets(List<Token> tokens, String sourceCode) {
        int[] starts = new int[tokens.size()];
        int[] ends = new int[tokens.size()];
        int currentOffset = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            int found = sourceCode.indexOf(t.value(), currentOffset);
            starts[i] = (found == -1) ? currentOffset : found;
            ends[i] = starts[i] + t.value().length();
            currentOffset = ends[i];
        }
        return new int[][] {starts, ends};
    }

    private String applyFormatting(String sourceCode, List<Token> tokens) {
        int[][] offsets = computeOffsets(tokens, sourceCode);
        int[] starts = offsets[0];
        int[] ends = offsets[1];
        StringBuilder sb = new StringBuilder();
        sb.append(sourceCode, 0, starts[0]);

        FormattingState state = new FormattingState();
        for (int i = 0; i < tokens.size(); i++) {
            formatTokenStep(sb, sourceCode, tokens, i, starts, ends, state);
        }
        appendTrailingSource(sb, sourceCode, ends[tokens.size() - 1]);
        return sb.toString();
    }

    private void formatTokenStep(
            StringBuilder sb,
            String source,
            List<Token> tokens,
            int i,
            int[] starts,
            int[] ends,
            FormattingState state) {
        Token current = tokens.get(i);
        if (i > 0) {
            Token prev = tokens.get(i - 1);
            String sep = source.substring(ends[i - 1], starts[i]);
            sb.append(computeSeparator(prev, current, sep, state));
        }
        updateStateForToken(current, state);
        sb.append(current.value());
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
        if (current.type() == TokenType.PRINTLN || "println".equals(current.value())) {
            state.stmtHasPrintln = true;
        }
        if (current.type() == TokenType.SEMICOLON && state.stmtHasPrintln) {
            state.justFinishedPrintln = true;
            state.stmtHasPrintln = false;
        }
    }

    private void appendTrailingSource(StringBuilder sb, String source, int lastEndOffset) {
        if (lastEndOffset < source.length()) {
            sb.append(source.substring(lastEndOffset));
        }
    }

    private Result<String> writeResult(String output, Writer writer) throws IOException {
        if (writer != null) {
            writer.write(output);
            writer.flush();
        }
        return Result.success(output);
    }

    private static class FormattingState {
        int depth = 0;
        boolean justFinishedPrintln = false;
        boolean stmtHasPrintln = false;
        final IndentationRule indentRule = new IndentationRule();
    }
}
