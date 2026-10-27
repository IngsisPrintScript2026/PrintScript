/*
 * My Project
 */

package engine.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.e2e.CliRunner.CliRun;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * End-to-end: a source file on disk, the CLI arguments of the assignment, and what the user sees.
 */
class CliEndToEndTest {
    @TempDir Path dir;

    private String file(String name, String content) throws IOException {
        Path path = dir.resolve(name);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path.toString();
    }

    private String program(String content) throws IOException {
        return file("program.ps", content);
    }

    @Test
    @DisplayName(
            "Given: example 1 of the assignment | When: running Execution | Then: prints 'Joe"
                    + " Doe' and exits with 0")
    void cli_whenExecutingExampleOne_thenPrintsFullName() throws IOException {
        // Given
        String source =
                program(
                        """
                        let name: string = "Joe";
                        let lastName: string = "Doe";

                        println(name + " " + lastName);
                        """);

        // When
        CliRun run = CliRunner.run("", "Execution", "-v", "1.0", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode());
        assertTrue(run.stdout().startsWith("Joe Doe\n"), run.stdout());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "example 2 (division into a new variable)|let c: number = a / b;"
                        + "println(\"Result: \" + c);",
                "example 3 (reassignment)|a = a / b;println(\"Result: \" + a);"
            })
    @DisplayName(
            "Given: examples 2 and 3 of the assignment | When: running Execution | Then: prints"
                    + " 'Result: 3'")
    void cli_whenExecutingDivisionExamples_thenPrintsResult(String name, String tail)
            throws IOException {
        // Given
        String source = program("let a: number = 12;\nlet b: number = 4;\n" + tail);

        // When
        CliRun run = CliRunner.run("", "Execution", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(run.stdout().startsWith("Result: 3\n"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: operations with decimals, precedence and a non-exact division | When:"
                    + " executing | Then: numbers print like TypeScript")
    void cli_whenExecutingArithmetic_thenPrintsNumbers() throws IOException {
        // Given
        String source =
                program(
                        "println(2 + 3 * 4 - 6 / 2);\nprintln(1.5 * 2);\nprintln(10 / 4);\n"
                                + "println(10 / 3);\nprintln('total: ' + 0.5);\n"
                                + "println(10 - 4 - 3);\nprintln('Sum: ' + 1 + 2);");

        // When
        CliRun run = CliRunner.run("", "Execution", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(
                run.stdout().startsWith("11\n3\n2.5\n3.333333333333333\ntotal: 0.5\n3\nSum: 12\n"),
                run.stdout());
    }

    @Test
    @DisplayName(
            "Given: a valid program | When: running Validation | Then: reports success without"
                    + " executing it")
    void cli_whenValidatingValidProgram_thenReportsSuccessWithoutRunning() throws IOException {
        // Given
        String source = program("let a: number = 1;\nprintln(\"should not print\");\n");

        // When
        CliRun run = CliRunner.run("", "Validation", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode());
        assertTrue(run.stdout().contains("Validation successful"));
        assertTrue(!run.stdout().contains("should not print"));
    }

    @Test
    @DisplayName(
            "Given: a declaration without ':' | When: running Validation | Then: exits with 1 and"
                    + " shows a syntactic error with the start and end of the bad token")
    void cli_whenValidatingSyntaxError_thenShowsTokenRange() throws IOException {
        // Given
        String source = program("let a: number = 1;\nlet b number = 2;\n");

        // When
        CliRun run = CliRunner.run("", "Validation", "-i", source, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("Syntactic error"), run.stdout());
        assertTrue(
                run.stdout().contains("'number' from line 2, column 7 to line 2, column 12"),
                run.stdout());
    }

    @Test
    @DisplayName(
            "Given: a variable used before being declared | When: running Validation | Then:"
                    + " exits with 1 and shows a semantic error with the statement range")
    void cli_whenValidatingUndeclaredVariable_thenShowsSemanticRange() throws IOException {
        // Given
        String source = program("let a: number = 1;\nprintln(b);\n");

        // When
        CliRun run = CliRunner.run("", "Validation", "-i", source, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("Semantic error"), run.stdout());
        assertTrue(run.stdout().contains("'b'"), run.stdout());
        assertTrue(
                run.stdout().contains("from line 2, column 1 to line 2, column 11"), run.stdout());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
            delimiter = '|',
            value = {
                "const|const x: number = 1;",
                "boolean|let b: boolean = true;",
                "if|if (true) { println(1); }",
                "readInput|let s: string = readInput(\"x\");",
                "readEnv|let s: string = readEnv(\"HOME\");"
            })
    @DisplayName(
            "Given: a 1.1 feature | When: validating it as version 1.0 | Then: it is rejected"
                    + " naming the feature, and accepted as version 1.1")
    void cli_whenUsingOneDotOneFeatures_thenOnlyVersionOneDotOneAccepts(String feature, String code)
            throws IOException {
        // Given
        String source = program(code);

        // When
        CliRun v10 = CliRunner.run("", "Validation", "-v", "1.0", "-i", source, "--no-progress");
        CliRun v11 = CliRunner.run("", "Validation", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(1, v10.exitCode(), feature + " should fail in 1.0: " + v10.stdout());
        assertTrue(v10.stdout().contains("'" + feature + "'"), v10.stdout());
        assertEquals(0, v11.exitCode(), feature + " should pass in 1.1: " + v11.stdout());
    }

    @Test
    @DisplayName(
            "Given: a constant | When: reassigning it in 1.1 | Then: exits with 1 and a semantic"
                    + " error")
    void cli_whenReassigningConstant_thenFails() throws IOException {
        // Given
        String source = program("const x: number = 1;\nx = 2;\n");

        // When
        CliRun run = CliRunner.run("", "Execution", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("Cannot reassign constant variable 'x'"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: an if/else on a boolean variable | When: executing in 1.1 | Then: only the"
                    + " matching branch runs")
    void cli_whenExecutingIfElse_thenRunsMatchingBranch() throws IOException {
        // Given
        String source =
                program(
                        """
                        let ready: boolean = false;
                        if (ready) {
                            println("then");
                        } else {
                            println("else");
                        }
                        """);

        // When
        CliRun run = CliRunner.run("", "Execution", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(run.stdout().startsWith("else\n"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: a non-boolean if condition | When: validating in 1.1 | Then: it is a semantic"
                    + " error")
    void cli_whenIfConditionIsNotBoolean_thenFails() throws IOException {
        // Given
        String source = program("let n: number = 1;\nif (n) { println(n); }\n");

        // When
        CliRun run = CliRunner.run("", "Validation", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("must be boolean"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: two readInput calls | When: the answers come through standard input, one per"
                    + " line | Then: each value is read in order and converted to its type")
    void cli_whenReadingInputsFromStdin_thenUsesEachLine() throws IOException {
        // Given
        String source =
                program(
                        "let a: number = readInput(\"a?\");\nlet b: number = readInput(\"b?\");\n"
                                + "println(a + b);\n");

        // When
        CliRun run =
                CliRunner.run("2\n40\n", "Execution", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(run.stdout().startsWith("a?\nb?\n42\n"), run.stdout());
    }

    @Test
    @DisplayName("Given: a boolean readInput | When: the user types 'Hola' | Then: execution fails")
    void cli_whenInputIsNotABoolean_thenFails() throws IOException {
        // Given
        String source = program("let ok: boolean = readInput(\"ok?\");\nprintln(ok);\n");

        // When
        CliRun run =
                CliRunner.run("Hola\n", "Execution", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("Runtime error"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: an environment variable that always exists (PATH) | When: reading it with"
                    + " readEnv | Then: its value is printed")
    void cli_whenReadingEnvironment_thenPrintsValue() throws IOException {
        // Given
        String source = program("let path: string = readEnv(\"PATH\");\nprintln(path);\n");

        // When
        CliRun run = CliRunner.run("", "Execution", "-v", "1.1", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(run.stdout().startsWith(System.getenv("PATH") + "\n"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: unformatted code and a YAML with every 1.0 rule | When: running Formatting"
                    + " to a file | Then: the file has the expected format")
    void cli_whenFormattingToFile_thenAppliesConfiguredRules() throws IOException {
        // Given
        String source = program("let a:number=1;   let b : number = a*2;\nprintln(b);");
        String config =
                file(
                        "rules.yaml",
                        """
                        enforce-spacing-before-colon-in-declaration: true
                        enforce-spacing-after-colon-in-declaration: true
                        enforce-no-spacing-around-equals: true
                        line-breaks-before-println: 2
                        """);
        String output = dir.resolve("out.ps").toString();

        // When
        CliRun run =
                CliRunner.run(
                        "",
                        "Formatting",
                        "-i",
                        source,
                        "-c",
                        config,
                        "-o",
                        output,
                        "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertEquals(
                "let a : number=1;\nlet b : number=a * 2;\n\n\nprintln(b);\n",
                Files.readString(Path.of(output)));
    }

    @Test
    @DisplayName(
            "Given: an if block without indentation and an indent of 2 | When: formatting in 1.1"
                    + " to standard output | Then: the block is indented and the brace stays on"
                    + " the if line")
    void cli_whenFormattingIfBlock_thenIndentsContent() throws IOException {
        // Given
        String source = program("let ok: boolean = true;\nif (ok)\n{\nprintln(\"yes\");\n}");
        String config = file("rules.yaml", "indent-inside-if: 2\n");

        // When
        CliRun run =
                CliRunner.run(
                        "", "Formatting", "-v", "1.1", "-i", source, "-c", config, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertEquals("let ok: boolean = true;\nif (ok) {\n  println(\"yes\");\n}\n", run.stdout());
    }

    @Test
    @DisplayName(
            "Given: snake_case names and expressions in println/readInput | When: running"
                    + " Analyzing with camel case and both rules on | Then: exits with 1 listing"
                    + " each violation with its position")
    void cli_whenAnalyzingWithViolations_thenListsThem() throws IOException {
        // Given
        String source =
                program(
                        "let my_name: string = readInput(\"a\" + \"b\");\n"
                                + "println(my_name + \"!\");\n");
        String config =
                file(
                        "sca.yaml",
                        """
                        identifier_format: "camel case"
                        mandatory-variable-or-literal-in-println: true
                        mandatory-variable-or-literal-in-readInput: true
                        """);

        // When
        CliRun run =
                CliRunner.run(
                        "", "Analyzing", "-v", "1.1", "-i", source, "-c", config, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("'my_name'") && run.stdout().contains("line 1, column 5"));
        assertTrue(run.stdout().contains("Function 'readInput'"), run.stdout());
        assertTrue(run.stdout().contains("Function 'println'"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: clean code under snake case | When: running Analyzing | Then: exits with 0"
                    + " and no violations")
    void cli_whenAnalyzingCleanCode_thenPasses() throws IOException {
        // Given
        String source = program("let my_name: string = \"x\";\nprintln(my_name);\n");
        String config = file("sca.yaml", "identifier_format: \"snake case\"\n");

        // When
        CliRun run = CliRunner.run("", "Analyzing", "-i", source, "-c", config, "--no-progress");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(run.stdout().contains("0 violations"));
    }

    @Test
    @DisplayName(
            "Given: a source file | When: running without --no-progress | Then: parsing progress"
                    + " goes to standard error and the result to standard output")
    void cli_whenProgressEnabled_thenShowsItOnStderr() throws IOException {
        // Given
        String source = program("let a: number = 1;\nprintln(a);\n");

        // When
        CliRun run = CliRunner.run("", "Execution", "-i", source);

        // Then
        assertEquals(0, run.exitCode());
        assertTrue(run.stderr().contains("Parsing: ") && run.stderr().contains("100%"));
        assertTrue(run.stdout().startsWith("1\n"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: --no-progress | When: validating | Then: nothing is written to standard"
                    + " error")
    void cli_whenProgressDisabled_thenStderrIsEmpty() throws IOException {
        // Given
        String source = program("let a: number = 1;\n");

        // When
        CliRun run = CliRunner.run("", "Validation", "-i", source, "--no-progress");

        // Then
        assertEquals(0, run.exitCode());
        assertEquals("", run.stderr());
    }

    @Test
    @DisplayName(
            "Given: the program in standard input instead of a file | When: running Execution |"
                    + " Then: it is executed")
    void cli_whenProgramComesFromStdin_thenExecutesIt() {
        // Given
        String code = "println(\"from stdin\");\n";

        // When
        CliRun run = CliRunner.run(code, "Execution");

        // Then
        assertEquals(0, run.exitCode(), run.stdout());
        assertTrue(run.stdout().startsWith("from stdin\n"), run.stdout());
    }

    @Test
    @DisplayName(
            "Given: an unknown operation name | When: running the CLI | Then: exits with 1 and"
                    + " names the operation")
    void cli_whenOperationIsUnknown_thenFails() throws IOException {
        // Given
        String source = program("let a: number = 1;\n");

        // When
        CliRun run = CliRunner.run("", "Compile", "-i", source, "--no-progress");

        // Then
        assertEquals(1, run.exitCode());
        assertTrue(run.stdout().contains("Unknown operation: Compile"));
    }

    @Test
    @DisplayName(
            "Given: a version that does not exist | When: running the CLI | Then: exits with a"
                    + " non-zero code")
    void cli_whenVersionIsUnknown_thenFails() throws IOException {
        // Given
        String source = program("let a: number = 1;\n");

        // When
        CliRun run = CliRunner.run("", "Validation", "-v", "2.0", "-i", source);

        // Then
        assertNotEquals(0, run.exitCode());
        assertTrue(run.stderr().contains("Unknown version: 2.0"), run.stderr());
    }
}
