package service;

import formatter.FormatContext;
import formatter.TokenStreamFormatter;
import formatter.config.YamlFormatRulesLoader;
import result.Result;
import version.Version;

import java.io.InputStream;
import java.io.Writer;

public class FormatService {

    public Result<String> format(Version version, InputStream in, InputStream config, Writer writer) {
        FormatContext context = (config != null) ? YamlFormatRulesLoader.loadFromYaml(config) : new FormatContext();
        TokenStreamFormatter formatter = new TokenStreamFormatter(context);
        return formatter.format(in, writer);
    }

    public Result<String> format(Version version, InputStream in, Writer writer) {
        return format(version, in, null, writer);
    }
}

