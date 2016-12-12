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

public class UnitTestRunner {
	private final ScriptCompiler compiler;
	private final CompileResult prior;
	private final BCEClassLoader loader;
	private final List<UnitTestResultHandler> handlers = new ArrayList<>();

	public UnitTestRunner(ScriptCompiler compiler, CompileResult prior) {
		this.compiler = compiler;
		this.prior = prior;
		loader = new BCEClassLoader(prior.bce);
	}
	
	public void considerResource(File file) {
		loader.addClassesFrom(file);
	}

	public void sendResultsTo(UnitTestResultHandler resultHandler) {
		handlers.add(resultHandler);
	}
	
	public void run(File f) throws ClassNotFoundException, IOException {
		String scriptPkg = prior.getPackage() + ".script";
		TestScript script = convertScript(scriptPkg, f);
		compileScope(scriptPkg, script.scope());
		script.runAllTests(new TestCaseRunner() {
			@Override
			public void run(SingleTestCase tc) {
				try {
					runCase(scriptPkg, tc);
				} catch (Exception ex) {
					// this should call handler.failed()
					ex.printStackTrace();
				}
			}
		});
	}

	protected void runCase(String scriptPkg, SingleTestCase tc) throws ClassNotFoundException {
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
		
		try {
			assertEquals(evals.get("value1"), evals.get("expr1"));
			for (UnitTestResultHandler h : handlers) {
				h.testPassed(tc.getDescription());
			}
		} catch (AssertionError ex) {
			ex.printStackTrace();
		}
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
