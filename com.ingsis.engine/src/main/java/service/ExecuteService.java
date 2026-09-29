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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import lexer.Lexer;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
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
        ExecutionContext ctx = new ExecutionContext(version, emitter, supplier, in);
        Result<SemanticEnvironment> res =
                execute(ctx, new SemanticEnvironment(), new Environment());
        if (res.isCorrect()) {
            return new CorrectResult<>("Program executed successfully");
        }
        return new IncorrectResult<>(((IncorrectResult<SemanticEnvironment>) res).error());
    }

    public Result<SemanticEnvironment> execute(
            ExecutionContext ctx, SemanticEnvironment semanticEnv, Environment runtimeEnv) {
        try {
            StreamCharReader reader =
                    new StreamCharReader(new InputStreamReader(ctx.in(), StandardCharsets.UTF_8));
            CharStream charStream = new CharStream(reader);
            Interpreter interpreter = buildInterpreter(ctx);
            return interpreter.interpret(
                    new LazyTokenStream(new Lexer(charStream)), semanticEnv, runtimeEnv);
        } catch (Exception e) {
            return new IncorrectResult<>("Execution error: " + e.getMessage());
        }
    }

    private Interpreter buildInterpreter(ExecutionContext ctx) {
        builtin.provider.InputProvider inputProvider =
                (ctx.supplier() != null) ? ctx.supplier()::readInput : prompt -> "";
        return new DefaultInterpreter(
                syntactic.parser.ParserFactory.createParser(ctx.version()),
                new SemanticChecker(),
                msg -> {
                    if (ctx.emitter() != null) {
                        ctx.emitter().emit(msg);
                    }
                },
                new builtin.DefaultFunctionRegistry(inputProvider, System::getenv));
    }
}
