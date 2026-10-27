/*
 * My Project
 */

package engine.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Black-box tests of the installed CLI distribution running on a clean JRE 21 container: the same
 * launcher a user would run, isolated from the developer's machine (no IDE, no Gradle, no local
 * environment variables).
 */
@Testcontainers(disabledWithoutDocker = true)
class CliContainerTest {
    private static final String DISTRIBUTION = System.getProperty("printscript.dist");

    @Container
    private static final GenericContainer<?> JRE =
            new GenericContainer<>(DockerImageName.parse("eclipse-temurin:21-jre"))
                    .withCommand("sleep", "infinity")
                    .withEnv("GREETING", "Hola desde Docker")
                    .withEnv("RETRIES", "3")
                    .withCopyFileToContainer(
                            MountableFile.forHostPath(Path.of(DISTRIBUTION)), "/app");

    private PrintScriptCli cli;

    @BeforeEach
    void setUp() {
        cli = new PrintScriptCli(JRE);
    }

    @Test
    @DisplayName(
            "Given: the first example of the assignment | When: executing it with the installed"
                    + " launcher | Then: prints the full name and exits with 0")
    void cli_whenExecutingAssignmentExample_thenPrintsFullName() throws Exception {
        // Given
        cli.withProgram(
                """
                let name: string = "Joe";
                let lastName: string = "Doe";

                println(name + " " + lastName);
                """);

        // When
        ExecResult result = cli.run("", "Execution", "-v", "1.0", "-i", PrintScriptCli.PROGRAM);

        // Then
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().startsWith("Joe Doe\n"), result.getStdout());
    }

    @Test
    @DisplayName(
            "Given: a program with a type error | When: validating it | Then: exits with 1 and"
                    + " reports the error with its start and end position")
    void cli_whenValidatingTypeError_thenReportsPositionAndFails() throws Exception {
        // Given
        cli.withProgram("let a: number = 1;\nlet b: number = \"x\";\n");

        // When
        ExecResult result = cli.run("", "Validation", "-i", PrintScriptCli.PROGRAM);

        // Then
        assertEquals(1, result.getExitCode());
        assertTrue(result.getStdout().contains("Semantic error"), result.getStdout());
        assertTrue(
                result.getStdout().contains("from line 2, column 1 to line 2, column 20"),
                result.getStdout());
    }

    @Test
    @DisplayName(
            "Given: a 1.1 program that reads a number | When: the value arrives through standard"
                    + " input | Then: it is converted to number before operating")
    void cli_whenReadInputFromStdin_thenConvertsToDeclaredType() throws Exception {
        // Given
        cli.withProgram("let n: number = readInput(\"Number:\");\nprintln(n * 2);\n");

        // When
        ExecResult result = cli.run("21\n", "Execution", "-v", "1.1", "-i", PrintScriptCli.PROGRAM);

        // Then
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().startsWith("Number:\n42\n"), result.getStdout());
    }

    @Test
    @DisplayName(
            "Given: environment variables defined on the container | When: a 1.1 program reads"
                    + " them with readEnv | Then: string and number values are used")
    void cli_whenReadEnv_thenUsesContainerEnvironment() throws Exception {
        // Given
        cli.withProgram(
                """
                let greeting: string = readEnv("GREETING");
                let retries: number = readEnv("RETRIES");
                println(greeting);
                println(retries + 1);
                """);

        // When
        ExecResult result = cli.run("", "Execution", "-v", "1.1", "-i", PrintScriptCli.PROGRAM);

        // Then
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().startsWith("Hola desde Docker\n4\n"), result.getStdout());
    }

    @Test
    @DisplayName(
            "Given: a program using readInput | When: executing it as version 1.0 | Then: the"
                    + " function is rejected because it does not exist in that version")
    void cli_whenUsingReadInputInVersionOne_thenRejectsIt() throws Exception {
        // Given
        cli.withProgram("let s: string = readInput(\"x\");\n");

        // When
        ExecResult result =
                cli.run("hola\n", "Execution", "-v", "1.0", "-i", PrintScriptCli.PROGRAM);

        // Then
        assertEquals(1, result.getExitCode());
        assertTrue(
                result.getStdout().contains("'readInput' is not supported in PrintScript 1.0"),
                result.getStdout());
    }

    @Test
    @DisplayName(
            "Given: unformatted code and a YAML config | When: formatting to an output file |"
                    + " Then: the file contains the code formatted with the configured rules")
    void cli_whenFormattingWithConfig_thenWritesFormattedFile() throws Exception {
        // Given
        cli.withProgram("let a:number=1;let b : number = a+2;\nprintln(b);")
                .withConfig(
                        "enforce-spacing-before-colon-in-declaration: false\n"
                                + "enforce-spacing-after-colon-in-declaration: true\n"
                                + "enforce-spacing-around-equals: true\n"
                                + "line-breaks-before-println: 1\n");

        // When
        ExecResult result =
                cli.run(
                        "",
                        "Formatting",
                        "-i",
                        PrintScriptCli.PROGRAM,
                        "-c",
                        PrintScriptCli.CONFIG,
                        "-o",
                        PrintScriptCli.OUTPUT);

        // Then
        assertEquals(0, result.getExitCode());
        assertEquals(
                "let a: number = 1;\nlet b: number = a + 2;\n\nprintln(b);\n",
                cli.readFile(PrintScriptCli.OUTPUT));
    }

    @Test
    @DisplayName(
            "Given: a snake_case identifier and an expression inside println | When: analyzing"
                    + " with camel case and the println rule on | Then: reports both violations")
    void cli_whenAnalyzing_thenReportsViolationsWithPositions() throws Exception {
        // Given
        cli.withProgram("let my_var: number = 1;\nprintln(my_var + 1);\n")
                .withConfig(
                        "identifier_format: \"camel case\"\n"
                                + "mandatory-variable-or-literal-in-println: true\n");

        // When
        ExecResult result =
                cli.run("", "Analyzing", "-i", PrintScriptCli.PROGRAM, "-c", PrintScriptCli.CONFIG);

        // Then
        assertEquals(1, result.getExitCode());
        assertTrue(result.getStdout().contains("'my_var'"), result.getStdout());
        assertTrue(result.getStdout().contains("line 1, column 5"), result.getStdout());
        assertTrue(result.getStdout().contains("Function 'println'"), result.getStdout());
    }

    @Test
    @DisplayName(
            "Given: a valid file | When: validating it without --no-progress | Then: the parsing"
                    + " progress is shown on standard error and ends at 100%")
    void cli_whenValidating_thenShowsParsingProgressOnStderr() throws Exception {
        // Given
        cli.withProgram("let a: number = 1;\nprintln(a);\n");

        // When
        ExecResult result = cli.run("", "Validation", "-i", PrintScriptCli.PROGRAM);

        // Then
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStderr().contains("Parsing: 100%"), result.getStderr());
        assertTrue(result.getStdout().contains("Validation successful"), result.getStdout());
    }
}
