package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import formatter.ASTFormatter;
import formatter.Formatter;
import iterator.IterationStep;
import iterator.SafeIterator;
import lexer.Lexer;
import node.Node;
import node.ProgramNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import token.Token;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FormatService {

    public Result<String> format(Version version, InputStream in, InputStream config, Writer writer) {
        Formatter customFormatter = (config != null) ? ASTFormatter.fromYamlConfig(config) : new ASTFormatter();
        return format(version, in, writer, customFormatter);
    }

    public Result<String> format(Version version, InputStream in, Writer writer) {
        return format(version, in, writer, new ASTFormatter());
    }

    public Result<String> format(Version version, InputStream in, Writer writer, Formatter formatter) {
        try {
            StreamCharReader reader = new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);
            TokenStream currentStream = new LazyTokenStream(lexer);

            syntactic.Parser<Node> statementParser = syntactic.parser.ParserFactory.createParser(version);

            List<Node> statements = new ArrayList<>();

            while (!currentStream.isEmpty()) {
                Result<IterationStep<Node>> parseResult = statementParser.parse(currentStream);
                if (!parseResult.isCorrect()) {
                    String err = ((IncorrectResult<IterationStep<Node>>) parseResult).error();
                    if ("EOF".equalsIgnoreCase(err) || err.contains("EOF")) {
                        break;
                    }
                    return Result.failure(err.startsWith("Syntactic error:") ? err : "Syntactic error: " + err);
                }

                IterationStep<Node> step = ((CorrectResult<IterationStep<Node>>) parseResult).value();
                Node statement = step.value();
                currentStream = (TokenStream) step.next();

                statements.add(statement);
            }

            ProgramNode programNode = new ProgramNode(statements, 1, 1);
            Result<String> formatResult = formatter.format(programNode);

            if (formatResult.isCorrect()) {
                String formattedCode = ((CorrectResult<String>) formatResult).value();
                if (writer != null && !(writer instanceof OutputStreamWriter)) {
                    writer.write(formattedCode);
                    writer.flush();
                }
                return new CorrectResult<>(formattedCode);
            }
            return formatResult;

        } catch (Exception e) {
            return Result.failure("Formatting error: " + e.getMessage());
        }
    }
}
