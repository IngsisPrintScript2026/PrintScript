/*
 * My Project
 */

package engine.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import engine.CliEngine;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class CliEngineReplAndStdinIntegrationTest {

    @Test
    @DisplayName(
            "What: CliEngine REPL and STDIN | When: feeding input through System.in | Then:"
                    + " processes lines, REPL loop, and commands correctly")
    void cliEngine_whenFeedingSystemIn_thenProcessesReplAndCommandsCorrectly() throws Exception {
        // What: original System.in saved
        InputStream origIn = System.in;
        try {
            // When: running interactive REPL with custom stream where available() returns 0
            String replInput = "let a: number = 10;\n\nlet b number = 20;\n\nexit\n";
            InputStream replStream =
                    new InputStream() {
                        private final ByteArrayInputStream inner =
                                new ByteArrayInputStream(
                                        replInput.getBytes(StandardCharsets.UTF_8));

                        @Override
                        public int read() {
                            return inner.read();
                        }

                        @Override
                        public int available() {
                            return 0;
                        }
                    };
            System.setIn(replStream);
            CliEngine cli = new CliEngine();
            int exitCode = cli.call();

            // Then: REPL exits successfully with code 0
            assertEquals(0, exitCode);

            // When: running command with piped STDIN
            String code = "let x: number = 42;\nprintln(x);\n";
            System.setIn(new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)));
            CommandLine cmd = new CommandLine(new CliEngine());
            int exitCodeStdin = cmd.execute("Execution", "-v", "1.0");

            // Then: command executes successfully from STDIN with code 0
            assertEquals(0, exitCodeStdin);
        } finally {
            // Restore System.in
            System.setIn(origIn);
        }
    }
}
