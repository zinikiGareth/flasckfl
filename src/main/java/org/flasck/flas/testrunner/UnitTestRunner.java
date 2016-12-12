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
import org.flasck.flas.parsedForm.Scope;
import org.zinutils.bytecode.BCEClassLoader;
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
				} catch (AssertFailed ex) {
					for (UnitTestResultHandler h : handlers)
						h.testFailed(tc.getDescription(), ex.expected, ex.actual);
				} catch (Exception ex) {
					for (UnitTestResultHandler h : handlers)
						h.testError(tc.getDescription(), ex);
				}
			}
		});
	}

	protected void runCase(String scriptPkg, SingleTestCase tc) throws Exception {
		tc.run(loader, scriptPkg);
		for (UnitTestResultHandler h : handlers) {
			h.testPassed(tc.getDescription());
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
		} catch (ErrorResultException ex) {
			ex.errors.showTo(new PrintWriter(System.err), 0);
			fail("Errors compiling test script");
		}
		// 3. Load the class(es) into memory
		loader.add(tcr.bce);
	}
}
