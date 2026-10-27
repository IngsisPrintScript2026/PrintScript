/*
 * My Project
 */

package interpreter.integration;

import builtin.DefaultFunctionRegistry;
import builtin.provider.EnvProvider;
import builtin.provider.InputProvider;
import charstream.CharStream;
import charstream.StreamCharReader;
import environment.Environment;
import interpreter.DefaultInterpreter;
import java.io.StringReader;
import java.util.function.Consumer;
import lexer.Lexer;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import syntactic.parser.ParserFactory;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

/** Runs real PrintScript source through lexer, parser, semantic checker and interpreter. */
final class PrintScriptRunner {
    private final Version version;
    private final InputProvider input;
    private final EnvProvider env;

    PrintScriptRunner(Version version, InputProvider input, EnvProvider env) {
        this.version = version;
        this.input = input;
        this.env = env;
    }

    Result<SemanticEnvironment> run(String source, Consumer<String> output) {
        DefaultInterpreter interpreter =
                new DefaultInterpreter(
                        ParserFactory.createParser(version),
                        new SemanticChecker(version),
                        output,
                        new DefaultFunctionRegistry(input, env));
        return interpreter.interpret(tokens(source), new SemanticEnvironment(), new Environment());
    }

    static TokenStream tokens(String source) {
        StreamCharReader reader = new StreamCharReader(new StringReader(source));
        return new LazyTokenStream(new Lexer(new CharStream(reader)));
    }
}
