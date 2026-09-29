/*
 * My Project
 */

package engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import service.ExecuteService;
import service.ExecutionContext;
import service.ValidationService;
import version.Version;

@Command(
        name = "cli-engine",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "CLI wrapper around the Engine")
public class CliEngine implements Callable<Integer>, Engine {

    private final ExecuteService executeService = new ExecuteService();
    private final ValidationService validationService = new ValidationService();

    @Parameters(
            index = "0",
            arity = "0..1",
            description = "Operation: Validation, Execution, Formatting, Analyzing")
    private String operation;

    @Option(
            names = {"-i", "--input"},
            description = "Input file (defaults to STDIN)")
    private File inputFile;

    @Option(
            names = {"-c", "--config"},
            description = "Config file (optional)")
    private File configFile;

    @Option(
            names = {"-o", "--output"},
            description = "Output file (defaults to STDOUT)")
    private File outputFile;

    @Option(
            names = {"-v", "--version"},
            description = "Version")
    private String versionString = "1.0";

    @Override
    public Integer call() throws Exception {
        Version version = Version.fromString(versionString);
        try (InputStream in = inputFile != null ? new FileInputStream(inputFile) : System.in;
                InputStream config = configFile != null ? new FileInputStream(configFile) : null;
                Writer writer =
                        outputFile != null
                                ? new FileWriter(outputFile)
                                : new OutputStreamWriter(System.out)) {
            OutputEmitter emitter = System.out::println;
            CliStreams streams = new CliStreams(in, config, writer);
            if (inputFile == null && System.in.available() == 0) {
                return runRepl(version, emitter) ? 0 : 1;
            }
            Result<String> result = executeOperation(streams, version, emitter);
            return handleResult(result, writer) ? 0 : 1;
        }
    }

    private boolean runRepl(Version version, OutputEmitter emitter) {
        System.out.println(
                "Entering CLI Engine REPL. Type empty line to execute and 'exit' to quit.");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            runReplLoop(version, emitter, reader);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void runReplLoop(Version version, OutputEmitter emitter, BufferedReader reader)
            throws IOException {
        ReplState state =
                new ReplState(
                        new semantic.environment.SemanticEnvironment(),
                        new environment.Environment());
        StringBuilder buffer = new StringBuilder();
        String line;
        while (true) {
            System.out.print("> ");
            line = reader.readLine();
            if (line == null || line.equalsIgnoreCase("exit")) break;
            if (line.trim().isEmpty()) {
                state = processReplCommand(buffer.toString(), version, emitter, state);
                buffer.setLength(0);
            } else {
                buffer.append(line).append("\n");
            }
        }
    }

    private ReplState processReplCommand(
            String code, Version version, OutputEmitter emitter, ReplState state) {
        if (code.isBlank()) {
            return state;
        }
        ExecutionContext ctx =
                new ExecutionContext(
                        version, emitter, prompt -> "", new ByteArrayInputStream(code.getBytes()));
        Result<semantic.environment.SemanticEnvironment> res =
                executeService.execute(ctx, state.semEnv(), state.runEnv());
        if (res.isCorrect()) {
            System.out.println("Program executed successfully");
            var ok = (CorrectResult<semantic.environment.SemanticEnvironment>) res;
            return new ReplState(ok.value(), state.runEnv());
        }
        String err = ((IncorrectResult<semantic.environment.SemanticEnvironment>) res).error();
        System.out.println("Error: " + err);
        System.out.flush();
        return state;
    }

    private Result<String> executeOperation(
            CliStreams streams, Version version, OutputEmitter emitter) {
        if (isExecutionOp()) {
            InputSupplier inputSupplier = prompt -> readLineFromStdin();
            return interpret(version, emitter, inputSupplier, streams.in());
        } else if (isValidationOp()) {
            return validate(version, streams.in());
        } else if (isFormattingOperation()) {
            return format(version, streams.in(), streams.config(), streams.writer());
        } else if (isAnalyzingOp()) {
            return analyze(version, streams.in(), streams.config());
        }
        return new IncorrectResult<>("Unknown operation: " + operation);
    }

    private String readLineFromStdin() {
        try {
            return new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
            return "";
        }
    }

    private boolean isExecutionOp() {
        return operation == null
                || operation.equalsIgnoreCase("Execution")
                || operation.equalsIgnoreCase("interpret")
                || operation.equalsIgnoreCase("exec");
    }

    private boolean isValidationOp() {
        return operation != null
                && (operation.equalsIgnoreCase("Validation")
                        || operation.equalsIgnoreCase("validate"));
    }

    private boolean isAnalyzingOp() {
        return operation != null
                && (operation.equalsIgnoreCase("Analyzing")
                        || operation.equalsIgnoreCase("analyze")
                        || operation.equalsIgnoreCase("lint"));
    }

    private boolean handleResult(Result<String> result, Writer writer) {
        try {
            if (result.isCorrect()) {
                writeSuccessResult(result, writer);
                return true;
            }
            writeErrorResult(result);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void writeSuccessResult(Result<String> result, Writer writer) throws IOException {
        if (isFormattingOperation()) {
            return;
        }
        String value = ((CorrectResult<String>) result).value();
        if (value != null) {
            writer.write(value);
            writer.write("\n");
            writer.flush();
        }
    }

    private void writeErrorResult(Result<String> result) {
        String error = ((IncorrectResult<String>) result).error();
        System.out.println("Error: " + error);
        System.out.flush();
    }

    private boolean isFormattingOperation() {
        return operation != null
                && (operation.equalsIgnoreCase("Formatting")
                        || operation.equalsIgnoreCase("format")
                        || operation.equalsIgnoreCase("fmt"));
    }

    private record CliStreams(InputStream in, InputStream config, Writer writer) {}

    private record ReplState(
            semantic.environment.SemanticEnvironment semEnv, environment.Environment runEnv) {}

    @Override
    public Result<String> validate(Version version, InputStream in) {
        return validationService.validate(version, in);
    }

    @Override
    public Result<String> interpret(
            Version version, OutputEmitter emitter, InputSupplier supplier, InputStream in) {
        if (emitter == null) emitter = System.out::println;
        return executeService.execute(version, emitter, supplier, in);
    }

    @Override
    public Result<String> format(
            Version version, InputStream in, InputStream config, Writer writer) {
        return executeService.format(version, in, config, writer);
    }

    @Override
    public Result<String> analyze(Version version, InputStream in, InputStream config) {
        return executeService.analyze(version, in, config);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CliEngine()).execute(args);
        System.exit(exitCode);
    }
}
