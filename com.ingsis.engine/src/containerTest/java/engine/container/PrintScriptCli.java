/*
 * My Project
 */

package engine.container;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;

/** Drives the PrintScript launcher installed inside a running container. */
final class PrintScriptCli {
    static final String PROGRAM = "/work/program.ps";
    static final String CONFIG = "/work/config.yaml";
    static final String OUTPUT = "/work/output.ps";
    private static final String STDIN = "/work/stdin.txt";
    private static final String LAUNCHER = "sh /app/bin/com.ingsis.engine";

    private final GenericContainer<?> container;

    PrintScriptCli(GenericContainer<?> container) {
        this.container = container;
    }

    PrintScriptCli withProgram(String source) {
        container.copyFileToContainer(Transferable.of(source), PROGRAM);
        return this;
    }

    PrintScriptCli withConfig(String yaml) {
        container.copyFileToContainer(Transferable.of(yaml), CONFIG);
        return this;
    }

    /** Runs the CLI with the given arguments, feeding {@code stdin} as standard input. */
    ExecResult run(String stdin, String... args) throws IOException, InterruptedException {
        container.copyFileToContainer(Transferable.of(stdin), STDIN);
        String command = LAUNCHER + " " + quote(args) + " < " + STDIN;
        return container.execInContainer("sh", "-c", command);
    }

    String readFile(String path) throws IOException, InterruptedException {
        return container.execInContainer("cat", path).getStdout();
    }

    private static String quote(String... args) {
        return Arrays.stream(args).map(arg -> "'" + arg + "'").collect(Collectors.joining(" "));
    }
}
