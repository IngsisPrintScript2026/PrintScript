package formatter;

import charstream.CharStream;
import charstream.StreamCharReader;
import formatter.rule.*;
import iterator.IterationStep;
import iterator.SafeIterator;
import lexer.Lexer;
import result.CorrectResult;
import result.Result;
import token.Token;
import token.TokenType;

import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TokenStreamFormatter implements Formatter {

    private final List<FormattingRule> rules;
    private final FormatContext context;

    public TokenStreamFormatter(FormatContext context, List<FormattingRule> rules) {
        this.context = context != null ? context : new FormatContext();
        this.rules = rules;
    }

    public TokenStreamFormatter(FormatContext context) {
        this(context, List.of(
                new SpaceAfterColonRule(),
                new SpaceBeforeColonRule(),
                new SpaceAroundEqualsRule(),
                new SpaceAroundOperatorsRule(),
                new LineBreakAfterStatementRule(),
                new SingleSpaceSeparationRule(),
                new BracePositionRule()
        ));
    }

    public TokenStreamFormatter() {
        this(new FormatContext());
    }

    @Override
    public Result<String> format(node.ProgramNode program) {
        return Result.failure("TokenStreamFormatter formats directly from input stream");
    }

    @Override
    public Result<String> formatNode(node.Node node) {
        return Result.failure("TokenStreamFormatter formats directly from input stream");
    }

    public Result<String> format(InputStream in, Writer writer) {
        try {
            byte[] bytes = in.readAllBytes();
            String sourceCode = new String(bytes, StandardCharsets.UTF_8);

            StreamCharReader reader = new StreamCharReader(new StringReader(sourceCode));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);

            List<Token> tokens = new ArrayList<>();
            while (true) {
                Result<IterationStep<Token>> res = lexer.next();
                if (!res.isCorrect()) break;
                IterationStep<Token> step = ((CorrectResult<IterationStep<Token>>) res).value();
                tokens.add(step.value());
                lexer = step.nextStream();
            }

            if (tokens.isEmpty()) {
                if (writer != null) writer.write(sourceCode);
                return Result.success(sourceCode);
            }

            int[] startOffsets = new int[tokens.size()];
            int[] endOffsets = new int[tokens.size()];

            int currentOffset = 0;
            for (int i = 0; i < tokens.size(); i++) {
                Token t = tokens.get(i);
                int found = sourceCode.indexOf(t.value(), currentOffset);
                if (found == -1) {
                    found = currentOffset;
                }
                startOffsets[i] = found;
                endOffsets[i] = found + t.value().length();
                currentOffset = endOffsets[i];
            }

            StringBuilder sb = new StringBuilder();
            sb.append(sourceCode, 0, startOffsets[0]);

            int depth = 0;
            boolean justFinishedPrintln = false;
            boolean stmtHasPrintln = false;

            IndentationRule indentRule = new IndentationRule();

            for (int i = 0; i < tokens.size(); i++) {
                Token current = tokens.get(i);
                Token prev = (i > 0) ? tokens.get(i - 1) : null;

                if (i > 0) {
                    String originalSep = sourceCode.substring(endOffsets[i - 1], startOffsets[i]);
                    String sep = originalSep;

                    if (justFinishedPrintln && context.lineBreaksAfterPrintln() != null) {
                        int count = context.lineBreaksAfterPrintln();
                        sep = "\n".repeat(count + 1);
                        justFinishedPrintln = false;
                    }

                    for (FormattingRule rule : rules) {
                        if (rule.applies(prev, current, context)) {
                            sep = rule.formatSeparator(prev, current, sep, context);
                        }
                    }

                    if (indentRule.applies(prev, current, context)) {
                        indentRule.setDepth(depth);
                        sep = indentRule.formatSeparator(prev, current, sep, context);
                    }

                    sb.append(sep);
                }

                if (current.type() == TokenType.LBRACE) {
                    depth++;
                } else if (current.type() == TokenType.RBRACE) {
                    depth = Math.max(0, depth - 1);
                }

                if (current.type() == TokenType.PRINTLN || "println".equals(current.value())) {
                    stmtHasPrintln = true;
                }

                if (current.type() == TokenType.SEMICOLON) {
                    if (stmtHasPrintln) {
                        justFinishedPrintln = true;
                        stmtHasPrintln = false;
                    }
                }

                sb.append(current.value());
            }

            if (endOffsets[tokens.size() - 1] < sourceCode.length()) {
                sb.append(sourceCode.substring(endOffsets[tokens.size() - 1]));
            }

            String result = sb.toString();
            if (writer != null) {
                writer.write(result);
                writer.flush();
            }
            return Result.success(result);

        } catch (Exception e) {
            return Result.failure("Formatting error: " + e.getMessage());
        }
    }
}
