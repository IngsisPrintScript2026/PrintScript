/*
 * My Project
 */

package sca.integration;

import charstream.CharStream;
import charstream.StreamCharReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lexer.Lexer;
import node.Node;
import node.ProgramNode;
import pipeline.StatementPipeline;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import sca.ASTSca;
import sca.Sca;
import semantic.environment.SemanticEnvironment;
import tokenstream.LazyTokenStream;
import tokenstream.TokenStream;
import version.Version;

/** Wires the real pipeline: source → chars → lexer → parser + semantic checker → SCA. */
final class LintRunner {
    private LintRunner() {}

    static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    static TokenStream tokens(String source) {
        return new LazyTokenStream(
                new Lexer(new CharStream(new StreamCharReader(new StringReader(source)))));
    }

    /** Analyzes each statement as soon as it is parsed, like the CLI does. */
    static Result<List<String>> lint(String source, String yamlConfig) {
        Sca sca = ASTSca.fromYamlConfig(stream(yamlConfig));
        List<String> violations = new ArrayList<>();
        Result<SemanticEnvironment> run =
                new StatementPipeline(Version.V_1_1)
                        .run(
                                tokens(source),
                                new SemanticEnvironment(),
                                (statement, env) -> collect(sca, statement, env, violations));
        if (run instanceof IncorrectResult<SemanticEnvironment>(String error)) {
            return Result.failure(error);
        }
        return Result.success(violations);
    }

    /** Parses the whole source into a ProgramNode (statements are semantically checked). */
    static ProgramNode parseProgram(String source) {
        List<Node> statements = new ArrayList<>();
        new StatementPipeline(Version.V_1_1)
                .run(
                        tokens(source),
                        new SemanticEnvironment(),
                        (statement, env) -> {
                            statements.add(statement);
                            return Result.success(null);
                        });
        return new ProgramNode(statements, 1, 1);
    }

    private static Result<Void> collect(
            Sca sca, Node statement, SemanticEnvironment env, List<String> violations) {
        Result<List<String>> found = sca.analyzeNode(statement, env);
        if (found instanceof CorrectResult<List<String>>(List<String> list)) {
            violations.addAll(list);
            return Result.success(null);
        }
        return Result.failure(((IncorrectResult<List<String>>) found).error());
    }
}
