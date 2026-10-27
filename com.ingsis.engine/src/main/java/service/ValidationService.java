/*
 * My Project
 */

package service;

import java.io.InputStream;
import pipeline.StatementAction;
import pipeline.StatementPipeline;
import progress.ProgressListener;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import version.Version;

/** Checks syntax and semantics statement by statement, without executing anything. */
public class ValidationService {
    static final String SUCCESS_MESSAGE = "Validation successful: Syntax and semantics are valid.";

    private final ProgressListener progress;

    public ValidationService(ProgressListener progress) {
        this.progress = progress;
    }

    public ValidationService() {
        this(ProgressListener.NONE);
    }

    public Result<String> validate(Version version, InputStream in) {
        try {
            Result<SemanticEnvironment> res =
                    new StatementPipeline(version)
                            .run(
                                    SourceStreams.tokens(in, progress),
                                    new SemanticEnvironment(),
                                    StatementAction.NONE);
            return switch (res) {
                case IncorrectResult<SemanticEnvironment>(String err) -> Result.failure(err);
                default -> Result.success(SUCCESS_MESSAGE);
            };
        } catch (RuntimeException e) {
            return Result.failure("Validation error: " + e.getMessage());
        }
    }
}
