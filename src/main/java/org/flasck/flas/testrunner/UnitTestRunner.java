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
import org.flasck.flas.parsedForm.Scope;
import org.zinutils.bytecode.BCEClassLoader;
import org.zinutils.reflection.Reflection;
import org.zinutils.utils.FileUtils;
import org.zinutils.utils.MultiTextEmitter;

public class UnitTestRunner {
	private final MultiTextEmitter results;
	private final ScriptCompiler compiler;
	private final CompileResult prior;
	private final BCEClassLoader loader;

	public UnitTestRunner(MultiTextEmitter results, ScriptCompiler compiler, CompileResult prior) {
		this.results = results;
		this.compiler = compiler;
		this.prior = prior;
		loader = new BCEClassLoader(prior.bce);
	}
	
	public void considerResource(File file) {
		loader.addClassesFrom(file);
	}
	
	public void run(File f) throws ClassNotFoundException, IOException {
		String scriptPkg = prior.getPackage() + ".script";
		TestScript script = convertScript(scriptPkg, f);
		compileScope(scriptPkg, script.scope());
		List<Class<?>> toRun = new ArrayList<>();
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$expr1", false, loader));
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$value1", false, loader));
		// 5. Execute all the relevant functions & compare the results

		Class<?> fleval = Class.forName("org.flasck.jvm.FLEval", false, loader);
		Map<String, Object> evals = new TreeMap<String, Object>();
		for (Class<?> clz : toRun) {
			String key = clz.getSimpleName().replaceFirst(".*\\$", "");
			Object o = Reflection.callStatic(clz, "eval", new Object[] { new Object[] {} });
			o = Reflection.callStatic(fleval, "full", o);
			evals.put(key, o);
		}
		
		boolean passed = true;
		try {
			assertEquals(evals.get("value1"), evals.get("expr1"));
		} catch (AssertionError ex) {
			ex.printStackTrace();
			passed = false;
		}
		results.print(passed?"PASSED":"FAILED");
		results.println(":\tvalue x");
	}

	// Convert f into a standard fl "program" with a set of functions
	//      and build a meta-repository of what's going on
	private TestScript convertScript(String scriptPkg, File f) {
		TestScript script = new TestScript(scriptPkg);
		UnitTestConvertor c = new UnitTestConvertor(script);
		c.convert(FileUtils.readFileAsLines(f));
		return script;
	}

	// Compile this to JVM bytecodes using the regular compiler
	// - should only have access to exported things
	// - make the generated classes available to the loader
	private void compileScope(String scriptPkg, Scope scope) throws IOException {
		CompileResult tcr = null;
		try {
			tcr = compiler.createJVM(scriptPkg, prior, scope);
			System.out.println("cr = " + tcr.bce.all());
		} catch (ErrorResultException ex) {
			ex.errors.showTo(new PrintWriter(System.err), 0);
			fail("Errors compiling test script");
		}
		// 3. Load the class(es) into memory
		loader.add(tcr.bce);
	}
}
