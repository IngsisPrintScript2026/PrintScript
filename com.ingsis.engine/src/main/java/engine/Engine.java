/*
 * My Project
 */

package engine;

import java.io.InputStream;
import java.io.Writer;
import result.Result;
import version.Version;

public interface Engine {
    Result<String> validate(Version version, InputStream in);

    Result<String> interpret(
            Version version, OutputEmitter emitter, InputSupplier supplier, InputStream in);

    Result<String> format(Version version, InputStream in, InputStream config, Writer writer);

    Result<String> analyze(Version version, InputStream in, InputStream config);
}
