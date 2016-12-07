package org.flasck.flas.testrunner;

import java.io.File;
import java.util.List;

import org.flasck.flas.compiler.ScriptCompiler;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestRunner {

	public UnitTestRunner(MultiTextEmitter results, ScriptCompiler compiler, File f) {
		// I think:
		// 1. Convert f into a standard fl "program" with a set of functions
		//      and build a meta-repository of what's going on
		UnitTestConvertor c = new UnitTestConvertor();
		String pkg = f.getParentFile().getName();
		TestScript script = c.convert(pkg, FileUtils.readFileAsLines(f));

		// 2. Compile this to JVM bytecodes using the regular compiler
		// - should only have access to exported things
		List<Class<?>> classes = compiler.createJVM(pkg, script.flas);
		
		// 3. Load the class(es) into memory
		// 4. store the emitter
	}

	public void run() {
		// 5. Execute all the relevant functions & compare the results
	}

}
