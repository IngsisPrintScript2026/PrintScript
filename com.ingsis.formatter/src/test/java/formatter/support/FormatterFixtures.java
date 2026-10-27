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
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.Result;
import syntactic.Parser;
import syntactic.parser.ParserFactory;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

/** Wires the real pipeline (chars, lexer, parser, YAML loader) for integration tests. */
public final class FormatterFixtures {
    private FormatterFixtures() {}

    public static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    public static FormatContext context(String yaml) {
        return YamlFormatRulesLoader.loadFromYaml(stream(yaml));
    }

    /** Formats the source with a YAML configuration; returns the text or the failure message. */
    public static String format(String yaml, String source) {
        Result<String> result = new TokenStreamFormatter(context(yaml)).format(stream(source));
        return switch (result) {
            case CorrectResult<String>(String text) -> text;
            case result.IncorrectResult<String>(String error) -> error;
        };
    }

    /** Parses every statement of a PrintScript 1.1 source into a program node. */
    public static ProgramNode parse(String source) {
        Parser<Node> parser = ParserFactory.createParser(Version.V_1_1);
        TokenStream stream =
                new LazyTokenStream(
                        new Lexer(new CharStream(new StreamCharReader(new StringReader(source)))));
        List<Node> statements = new ArrayList<>();
        while (!stream.isEmpty()) {
            IterationStep<Node> step =
                    ((CorrectResult<IterationStep<Node>>) parser.parse(stream)).value();
            statements.add(step.value());
            stream = step.nextStream();
        }
        return new ProgramNode(statements, 1, 1);
    }
}
