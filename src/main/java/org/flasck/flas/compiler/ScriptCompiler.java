package org.flasck.flas.compiler;

import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;

public interface ScriptCompiler {

	CompileResult createJVM(String pkg, String flas) throws IOException, ErrorResultException;

}
