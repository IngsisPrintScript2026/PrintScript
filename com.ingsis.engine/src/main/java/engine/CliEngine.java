package engine;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import service.ExecuteService;
import service.ValidationService;
import version.Version;

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

@Command(
        name = "cli-engine",
        mixinStandardHelpOptions = true,
        version = "1.0",
        description = "CLI wrapper around the Engine")
public class CliEngine implements Callable<Integer>, Engine {

    private final ExecuteService executeService = new ExecuteService();
    private final ValidationService validationService = new ValidationService();

    @Parameters(index = "0", arity = "0..1", description = "Operation: Validation, Execution, Formatting, Analyzing")
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

            if (inputFile == null) {
                if (System.in.available() == 0) {
                    return runRepl(version, writer, emitter) ? 0 : 1;
                } else {
                    Result<String> result = executeOperation(version, in, config, writer, emitter);
                    return handleResult(result, writer) ? 0 : 1;
                }
            }

            Result<String> result = executeOperation(version, in, config, writer, emitter);
            return handleResult(result, writer) ? 0 : 1;
        }
    }

    private boolean runRepl(Version version, Writer writer, OutputEmitter emitter) {
        semantic.environment.SemanticEnvironment semanticEnv = new semantic.environment.SemanticEnvironment();
        environment.Environment runtimeEnv = new environment.Environment();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            InputSupplier inputSupplier = prompt -> {
                try {
                    return reader.readLine();
                } catch (IOException e) {
                    return "";
                }
            };

            System.out.println(
                    "Entering CLI Engine REPL. Type empty line to execute and 'exit' to quit.");
            String line;
            StringBuilder buffer = new StringBuilder();
            while (true) {
                System.out.print("> ");
                line = reader.readLine();
                if (line == null || line.equalsIgnoreCase("exit")) break;
                if (line.trim().isEmpty()) {
                    if (!buffer.isEmpty()) {
                        Result<semantic.environment.SemanticEnvironment> result =
                                executeService.execute(
                                        version,
                                        emitter,
                                        inputSupplier,
                                        new ByteArrayInputStream(buffer.toString().getBytes()),
                                        semanticEnv,
                                        runtimeEnv);
                        if (result.isCorrect()) {
                            semanticEnv = ((CorrectResult<semantic.environment.SemanticEnvironment>) result).value();
                            System.out.println("Program executed successfully");
                        } else {
                            String error = ((IncorrectResult<semantic.environment.SemanticEnvironment>) result).error();
                            System.out.println("Error: " + error);
                            System.out.flush();
                        }
                        buffer.setLength(0);
                    }
                    continue;
                }
                buffer.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Result<String> executeOperation(
            Version version,
            InputStream in,
            InputStream config,
            Writer writer,
            OutputEmitter emitter) {
        if (operation == null || operation.equalsIgnoreCase("Execution") || operation.equalsIgnoreCase("interpret") || operation.equalsIgnoreCase("exec")) {
            InputSupplier inputSupplier = prompt -> {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    return br.readLine();
                } catch (IOException e) {
                    return "";
                }
            };
            return interpret(version, emitter, inputSupplier, in);
        } else if (operation.equalsIgnoreCase("Validation") || operation.equalsIgnoreCase("validate")) {
            return validate(version, in);
        } else if (operation.equalsIgnoreCase("Formatting") || operation.equalsIgnoreCase("format") || operation.equalsIgnoreCase("fmt")) {
            return format(version, in, config, writer);
        } else if (operation.equalsIgnoreCase("Analyzing") || operation.equalsIgnoreCase("analyze") || operation.equalsIgnoreCase("lint")) {
            return analyze(version, in, config);
        }
        return new IncorrectResult<>("Unknown operation: " + operation);
    }

    private boolean handleResult(Result<String> result, Writer writer) {
        try {
            if (result.isCorrect()) {
                String value = ((CorrectResult<String>) result).value();
                if (value != null) {
                    writer.write(value);
                    writer.write("\n");
                    writer.flush();
                }
                return true;
            } else {
                String error = ((IncorrectResult<String>) result).error();
                System.out.println("Error: " + error);
                System.out.flush();
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

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
    public Result<String> format(Version version, InputStream in, InputStream config, Writer writer) {
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
