package org.flasck.flas.testrunner;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestRunner {

	public UnitTestRunner(MultiTextEmitter results, ScriptCompiler compiler, File f) throws IOException {
		// I think:
		// 1. Convert f into a standard fl "program" with a set of functions
		//      and build a meta-repository of what's going on
		UnitTestConvertor c = new UnitTestConvertor();
		String pkg = f.getParentFile().getName();
		TestScript script = c.convert(pkg, FileUtils.readFileAsLines(f));

		// 2. Compile this to JVM bytecodes using the regular compiler
		// - should only have access to exported things
		try {
			CompileResult cr = compiler.createJVM(pkg, script.flas);
			System.out.println("cr = " + cr);
		} catch (ErrorResultException ex) {
			ex.errors.showTo(new PrintWriter(System.err), 0);
			fail("Errors compiling test script");
		}
		// 3. Load the class(es) into memory
		// 4. store the emitter
	}

	public void run() {
		// 5. Execute all the relevant functions & compare the results
	}

}
