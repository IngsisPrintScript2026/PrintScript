/*
 * My Project
 */

package service;

import charstream.CharStream;
import charstream.StreamCharReader;
import engine.InputSupplier;
import engine.OutputEmitter;
import environment.Environment;
import interpreter.DefaultInterpreter;
import interpreter.Interpreter;
import iterator.SafeIterator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import lexer.Lexer;
import node.Node;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import token.Token;
import tokenstream.LazyTokenStream;
import version.Version;

public class ExecuteService implements engine.Engine {

    @Override
    public Result<String> validate(Version version, InputStream in) {
        return new ValidationService().validate(version, in);
    }

    @Override
    public Result<String> interpret(
            Version version,
            engine.OutputEmitter emitter,
            engine.InputSupplier supplier,
            InputStream in) {
        return execute(version, emitter, supplier, in);
    }

    @Override
    public Result<String> format(
            Version version, InputStream in, InputStream config, Writer writer) {
        return new FormatService().format(version, in, config, writer);
    }

    @Override
    public Result<String> analyze(Version version, InputStream in, InputStream config) {
        return new LintService().analyze(version, in, config);
    }

    public Result<String> execute(
            Version version, OutputEmitter emitter, InputSupplier supplier, InputStream in) {
        Result<SemanticEnvironment> res =
                execute(
                        version,
                        emitter,
                        supplier,
                        in,
                        new SemanticEnvironment(),
                        new Environment());
        if (res.isCorrect()) {
            return new CorrectResult<>("Program executed successfully");
        }
        return new IncorrectResult<>(((IncorrectResult<SemanticEnvironment>) res).error());
    }

    public Result<SemanticEnvironment> execute(
            Version version,
            OutputEmitter emitter,
            InputSupplier supplier,
            InputStream in,
            SemanticEnvironment semanticEnv,
            Environment runtimeEnv) {
        try {
            StreamCharReader reader =
                    new StreamCharReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            SafeIterator<Token> lexer = new Lexer(charStream);

            syntactic.Parser<Node> statementParser =
                    syntactic.parser.ParserFactory.createParser(version);
            SemanticChecker semanticChecker = new SemanticChecker();

            builtin.provider.InputProvider inputProvider =
                    (supplier != null) ? supplier::readInput : prompt -> "";
            Interpreter interpreter =
                    new DefaultInterpreter(
                            statementParser,
                            semanticChecker,
                            msg -> {
                                if (emitter != null) {
                                    emitter.emit(msg);
                                }
                            },
                            inputProvider,
                            System::getenv);

            return interpreter.interpret(new LazyTokenStream(lexer), semanticEnv, runtimeEnv);
        } catch (Exception e) {
            return new IncorrectResult<>("Execution error: " + e.getMessage());
        }
    }
}
