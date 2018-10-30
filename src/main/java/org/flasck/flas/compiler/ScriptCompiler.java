package org.flasck.flas.compiler;

import java.io.File;
import java.io.IOException;

import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.Scope;

public interface ScriptCompiler {

	CompileResult createJVM(String pkg, String priorPackage, IScope priorScope, String flas) throws IOException, ErrorResultException;

	CompileResult createJVM(String pkg, String priorPackage, IScope priorScope, Scope flas) throws IOException, ErrorResultException;

	CompileResult createJS(String pkg, String priorPackage, IScope priorScope, Scope flas) throws IOException, ErrorResultException;

	void writeJSTo(File scriptTo);

}
