/*
 * My Project
 */

package service;

import engine.InputSupplier;
import engine.OutputEmitter;
import environment.Environment;
import interpreter.DefaultInterpreter;
import interpreter.Interpreter;
import java.io.InputStream;
import java.io.Writer;
import progress.ProgressListener;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.SemanticChecker;
import semantic.environment.SemanticEnvironment;
import version.Version;

public class ExecuteService implements engine.Engine {
    private final ProgressListener progress;

    public ExecuteService(ProgressListener progress) {
        this.progress = progress;
    }

    public ExecuteService() {
        this(ProgressListener.NONE);
    }

    @Override
    public Result<String> validate(Version version, InputStream in) {
        return new ValidationService(progress).validate(version, in);
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
        return new FormatService(progress).format(version, in, config, writer);
    }

    @Override
    public Result<String> analyze(Version version, InputStream in, InputStream config) {
        return new LintService(progress).analyze(version, in, config);
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
            Interpreter interpreter = buildInterpreter(ctx);
            return interpreter.interpret(
                    SourceStreams.tokens(ctx.in(), progress), semanticEnv, runtimeEnv);
        } catch (Exception e) {
            return new IncorrectResult<>("Execution error: " + e.getMessage());
        }
    }

    private Interpreter buildInterpreter(ExecutionContext ctx) {
        builtin.provider.InputProvider inputProvider =
                (ctx.supplier() != null) ? ctx.supplier()::readInput : prompt -> "";
        return new DefaultInterpreter(
                syntactic.parser.ParserFactory.createParser(ctx.version()),
                new SemanticChecker(ctx.version()),
                msg -> {
                    if (ctx.emitter() != null) {
                        ctx.emitter().emit(msg);
                    }
                },
                new builtin.DefaultFunctionRegistry(inputProvider, System::getenv));
    }
}
