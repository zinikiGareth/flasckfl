package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResultException;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.reflection.Reflection;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestRunner {
	private final MultiTextEmitter results;
	private final List<Class<?>> toRun = new ArrayList<>();
	private final BCEClassLoader loader;

	public UnitTestRunner(MultiTextEmitter results, ScriptCompiler compiler, CompileResult prior, File f) throws IOException, ClassNotFoundException {
		this.results = results;
		loader = new BCEClassLoader(prior.bce);

		// I think:
		// 1. Convert f into a standard fl "program" with a set of functions
		//      and build a meta-repository of what's going on
		String scriptPkg = prior.getPackage() + ".script";
		TestScript script = new TestScript(scriptPkg);
		UnitTestConvertor c = new UnitTestConvertor(script);
		c.convert(FileUtils.readFileAsLines(f));

		// 2. Compile this to JVM bytecodes using the regular compiler
		// - should only have access to exported things
		CompileResult tcr = null;
		try {
			tcr = compiler.createJVM(scriptPkg, prior, script.scope());
			System.out.println("cr = " + tcr.bce.all());
		} catch (ErrorResultException ex) {
			ex.errors.showTo(new PrintWriter(System.err), 0);
			fail("Errors compiling test script");
		}
		// 3. Load the class(es) into memory
		tcr.bce.dumpAll(true);
		loader.add(tcr.bce);
//		String pkg = prior.getPackage();
//		loader.defineClass(scriptPkg + ".PACKAGEFUNCTIONS");
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$expr1", false, loader));
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$value1", false, loader));
//		loader.defineClass(pkg + ".PACKAGEFUNCTIONS$x");
//		loader.defineClass(pkg + ".PACKAGEFUNCTIONS");
	}
	
	public void considerResource(File file) {
		loader.addClassesFrom(file);
	}
	
	public void run() throws ClassNotFoundException {
		// 5. Execute all the relevant functions & compare the results

		Map<String, Object> evals = new TreeMap<String, Object>();
		for (Class<?> clz : toRun) {
			String key = clz.getSimpleName().replaceFirst(".*\\$", "");
			System.out.println("Evaluating " + clz + " " + key);
			Object o = Reflection.callStatic(clz, "eval", new Object[] { new Object[] {} });
			evals.put(key, o);
		}
		
		Class<?> fleval = Class.forName("org.flasck.jvm.FLEval", false, loader);
		boolean passed = true;
		try {
			Object expected = Reflection.callStatic(fleval, "full", evals.get("value1"));
			Object actual = Reflection.callStatic(fleval, "full", evals.get("expr1"));
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			ex.printStackTrace();
			passed = false;
		}
		results.print(passed?"PASSED":"FAILED");
		results.println(":\tvalue x");
	}

}
