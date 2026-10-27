/*
 * My Project
 */

package service;

import formatter.FormatContext;
import formatter.TokenStreamFormatter;
import formatter.config.YamlFormatRulesLoader;
import java.io.InputStream;
import java.io.Writer;
import progress.ProgressListener;
import result.Result;
import version.Version;

/** Streams the formatted source to the writer, configured by an optional YAML file. */
public class FormatService {
    private final ProgressListener progress;

    public FormatService(ProgressListener progress) {
        this.progress = progress;
    }

    public FormatService() {
        this(ProgressListener.NONE);
    }

    public Result<String> format(
            Version version, InputStream in, InputStream config, Writer writer) {
        FormatContext context =
                (config != null) ? YamlFormatRulesLoader.loadFromYaml(config) : new FormatContext();
        TokenStreamFormatter formatter = new TokenStreamFormatter(context);
        return formatter.format(SourceStreams.lexer(in, progress), writer);
    }

    public Result<String> format(Version version, InputStream in, Writer writer) {
        return format(version, in, null, writer);
    }
}
