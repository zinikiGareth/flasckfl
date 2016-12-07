package org.flasck.flas.testrunner;

import java.io.File;

import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestRunner {

	public UnitTestRunner(MultiTextEmitter results, File f) {
		UnitTestConvertor c = new UnitTestConvertor();
		TestScript script = c.convert(f.getParentFile().getName(), FileUtils.readFileAsLines(f));
		// I think:
		// 1. Convert f into a standard fl "program" with a set of functions
		//      and build a meta-repository of what's going on
		// 2. Compile this to JVM bytecodes using the regular compiler
		// - should only have access to exported things
		// compile(script.input)
		// 3. Load the class into memory
		// 4. store the emitter
	}

	public void run() {
		// 5. Execute all the relevant functions & compare the results
	}

}
