/*
 * My Project
 */

package service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import node.Node;
import pipeline.StatementPipeline;
import progress.ProgressListener;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ASTSca;
import sca.Sca;
import semantic.environment.SemanticEnvironment;
import version.Version;

/**
 * Runs the static code analyzer as each statement is parsed, keeping only the violations (never the
 * whole AST) in memory.
 */
public class LintService {
    private final ProgressListener progress;

    public LintService(ProgressListener progress) {
        this.progress = progress;
    }

    public LintService() {
        this(ProgressListener.NONE);
    }

    public Result<String> analyze(Version version, InputStream in, InputStream config) {
        Sca scaAnalyzer = (config != null) ? ASTSca.fromYamlConfig(config) : new ASTSca();
        return analyzeWithSca(version, in, scaAnalyzer);
    }

    public Result<String> analyze(Version version, InputStream in) {
        return analyzeWithSca(version, in, new ASTSca());
    }

    public Result<String> analyzeWithSca(Version version, InputStream in, Sca scaAnalyzer) {
        try {
            List<String> violations = new ArrayList<>();
            Result<SemanticEnvironment> res =
                    new StatementPipeline(version)
                            .run(
                                    SourceStreams.tokens(in, progress),
                                    new SemanticEnvironment(),
                                    (statement, env) ->
                                            collect(scaAnalyzer, statement, env, violations));
            if (res instanceof IncorrectResult<SemanticEnvironment>(String err)) {
                return Result.failure(err);
            }
            return report(violations);
        } catch (RuntimeException e) {
            return Result.failure("Analysis error: " + e.getMessage());
        }
    }

    private static Result<Void> collect(
            Sca sca, Node statement, SemanticEnvironment env, List<String> violations) {
        return switch (sca.analyzeNode(statement, env)) {
            case CorrectResult<List<String>>(List<String> found) -> {
                violations.addAll(found);
                yield Result.success(null);
            }
            case IncorrectResult<List<String>>(String err) -> Result.failure(err);
        };
    }

    private static Result<String> report(List<String> violations) {
        if (violations.isEmpty()) {
            return Result.success("SCA analysis passed with 0 violations");
        }
        return Result.failure(String.join("\n", violations));
    }
}
