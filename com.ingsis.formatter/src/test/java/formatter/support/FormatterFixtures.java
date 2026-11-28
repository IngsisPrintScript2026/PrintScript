/*
 * My Project
 */

package formatter.support;

import charstream.CharStream;
import charstream.StreamCharReader;
import formatter.FormatContext;
import formatter.TokenStreamFormatter;
import formatter.config.YamlFormatRulesLoader;
import iterator.IterationStep;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lexer.Lexer;
import node.ProgramNode;
import node.StatementNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.ParserFactory;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

public final class FormatterFixtures {
    private FormatterFixtures() {}

    public static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    public static FormatContext context(String yaml) {
        return YamlFormatRulesLoader.loadFromYaml(stream(yaml));
    }

    public static String format(String yaml, String source) {
        Result<String> result = new TokenStreamFormatter(context(yaml)).format(stream(source));
        return switch (result) {
            case CorrectResult<String>(String text) -> text;
            case result.IncorrectResult<String>(String error) -> error;
        };
    }

    public static ProgramNode parse(String source) {
        Parser<StatementNode> parser = ParserFactory.createParser(Version.V_1_1);
        TokenStream stream =
                new LazyTokenStream(
                        new Lexer(new CharStream(new StreamCharReader(new StringReader(source)))));
        List<StatementNode> statements = new ArrayList<>();
        while (!stream.isEmpty()) {
            IterationStep<StatementNode> step =
                    ((CorrectResult<IterationStep<StatementNode>>) parser.parse(stream)).value();
            statements.add(step.value());
            stream = step.nextStream();
        }
        return new ProgramNode(statements, 1, 1);
    }
}
