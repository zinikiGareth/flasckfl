package org.flasck.flas.testrunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
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
import org.zinutils.exceptions.UtilException;
import org.zinutils.reflection.Reflection;

public class JVMRunner implements TestRunner {
	private final CompileResult prior;
	private final String scriptPkg;
	private final BCEClassLoader loader;

	public JVMRunner(CompileResult prior) {
		this.prior = prior;
		scriptPkg = prior.getPackage()+".script";
		loader = new BCEClassLoader(prior.bce);
	}

	public void considerResource(File file) {
		loader.addClassesFrom(file);
	}

	// Compile this to JVM bytecodes using the regular compiler
	// - should only have access to exported things
	// - make the generated classes available to the loader
	public void prepareScript(ScriptCompiler compiler, Scope scope) {
		CompileResult tcr = null;
		try {
			try {
				tcr = compiler.createJVM(scriptPkg, prior, scope);
			} catch (ErrorResultException ex) {
				ex.errors.showTo(new PrintWriter(System.err), 0);
				fail("Errors compiling test script");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new UtilException("Failed", ex);
		}
		// 3. Load the class(es) into memory
		loader.add(tcr.bce);
	}

	@Override
	public void assertCorrectValue(int exprId) throws Exception {
		List<Class<?>> toRun = new ArrayList<>();
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$expr" + exprId, false, loader));
		toRun.add(Class.forName(scriptPkg + ".PACKAGEFUNCTIONS$value" + exprId, false, loader));

		Class<?> fleval = Class.forName("org.flasck.jvm.FLEval", false, loader);
		Map<String, Object> evals = new TreeMap<String, Object>();
		for (Class<?> clz : toRun) {
			String key = clz.getSimpleName().replaceFirst(".*\\$", "");
			Object o = Reflection.callStatic(clz, "eval", new Object[] { new Object[] {} });
			o = Reflection.callStatic(fleval, "full", o);
			evals.put(key, o);
		}
		
		Object expected = evals.get("value" + exprId);
		Object actual = evals.get("expr" + exprId);
		try {
			assertEquals(expected, actual);
		} catch (AssertionError ex) {
			throw new AssertFailed(expected, actual);
		}
	}

}
