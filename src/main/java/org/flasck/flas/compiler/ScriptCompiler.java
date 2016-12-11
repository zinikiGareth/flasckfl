package org.flasck.flas.compiler;

import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;

public interface ScriptCompiler {

	CompileResult createJVM(String pkg, CompileResult prior, String flas) throws IOException, ErrorResultException;

	CompileResult createJVM(String pkg, CompileResult prior, Scope flas) throws IOException, ErrorResultException;

}
