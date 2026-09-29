/*
 * My Project
 */

package service;

import engine.InputSupplier;
import engine.OutputEmitter;
import java.io.InputStream;
import version.Version;

public record ExecutionContext(
        Version version, OutputEmitter emitter, InputSupplier supplier, InputStream in) {}
