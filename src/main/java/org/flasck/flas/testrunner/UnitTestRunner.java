package org.flasck.flas.testrunner;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.reflection.Reflection;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestRunner {
	private final MultiTextEmitter results;
	private final List<Class<?>> toRun = new ArrayList<>();
	private final BCEClassLoader loader;

	public UnitTestRunner(MultiTextEmitter results, ScriptCompiler compiler, ByteCodeEnvironment bce, String pkg, File f) throws IOException {
		this.results = results;
		loader = new BCEClassLoader(bce);

		// I think:
		// 1. Convert f into a standard fl "program" with a set of functions
		//      and build a meta-repository of what's going on
		UnitTestConvertor c = new UnitTestConvertor();
		TestScript script = c.convert(pkg+".script", FileUtils.readFileAsLines(f));

		// 2. Compile this to JVM bytecodes using the regular compiler
		// - should only have access to exported things
		CompileResult cr = null;
		try {
			cr = compiler.createJVM(pkg+".script", script.flas);
			System.out.println("cr = " + cr.bce.all());
		} catch (ErrorResultException ex) {
			ex.errors.showTo(new PrintWriter(System.err), 0);
			fail("Errors compiling test script");
		}
		// 3. Load the class(es) into memory
		cr.bce.dumpAll(true);
		loader.add(cr.bce);
		loader.defineClass(pkg + ".script.PACKAGEFUNCTIONS");
		toRun.add(loader.defineClass(pkg + ".script.PACKAGEFUNCTIONS$expr1"));
		loader.defineClass(pkg + ".script.PACKAGEFUNCTIONS$value1");
		loader.defineClass(pkg + ".PACKAGEFUNCTIONS$x");
	}
	
	public void run() {
		// 5. Execute all the relevant functions & compare the results

		for (Class<?> clz : toRun) {
			System.out.println("Evaluating " + clz);
			Reflection.callStatic(clz, "eval", new Object[] { new Object[] {} });
		}
	}

}
