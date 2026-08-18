package engine;

import result.Result;
import version.Version;

import java.io.InputStream;
import java.io.Writer;

public interface Engine {
    Result<String> interpret(
            Version version, OutputEmitter emitter, InputSupplier supplier, InputStream in);
}
