package org.flasck.flas.testrunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.utils.FileUtils;

public class UnitTestRunner {
	public final static Logger logger = LoggerFactory.getLogger("UnitTests");
	private final ErrorResult errors;
	private final ScriptCompiler compiler;
	private final CompileResult prior;
	private final List<UnitTestResultHandler> handlers = new ArrayList<>();

	public UnitTestRunner(ErrorResult errors, ScriptCompiler compiler, CompileResult prior) {
		this.errors = errors;
		this.compiler = compiler;
		this.prior = prior;
	}
	
	public void sendResultsTo(UnitTestResultHandler resultHandler) {
		handlers.add(resultHandler);
	}
	
	public void run(File f, TestRunner runner) throws ClassNotFoundException, IOException, ErrorResultException {
		String scriptPkg = prior.getPackage().uniqueName() + ".script";
		TestScript script = convertScript(prior.getScope(), scriptPkg, f);
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
		runner.prepareScript(scriptPkg, compiler, script.scope());
		script.runAllTests(new TestCaseRunner() {
			@Override
			public void run(SingleTestCase tc) {
				try {
					runner.prepareCase();
					runCase(runner, tc);
				} catch (AssertFailed ex) {
					logger.error("AssertFailed: " + ex.getMessage());
					for (UnitTestResultHandler h : handlers)
						h.testFailed(tc.getDescription(), runner.name(), ex.expected, ex.actual);
				} catch (MultiException ex) {
					logger.error("Exceptions raised: " + ex.getMessage());
					for (String s : ex.allErrors())
						for (UnitTestResultHandler h : handlers)
							h.testError(tc.getDescription(), runner.name(), s);
				} catch (Exception ex) {
					logger.error("Exceptions thrown: " + ex.getMessage());
					for (UnitTestResultHandler h : handlers)
						h.testError(tc.getDescription(), runner.name(), ex);
				}
			}
		});
	}

	protected void runCase(TestRunner runner, SingleTestCase tc) throws Exception {
		logger.info("Running case " + tc.getDescription());
		tc.run(runner);
		for (UnitTestResultHandler h : handlers) {
			h.testPassed(tc.getDescription(), runner.name());
		}
	}

	// Convert f into a standard fl "program" with a set of functions
	//      and build a meta-repository of what's going on
	private TestScript convertScript(Scope scope, String scriptPkg, File f) {
		TestScript script = new TestScript(errors, scope, scriptPkg);
		UnitTestConvertor c = new UnitTestConvertor(script);
		c.convert(FileUtils.readFileAsLines(f));
		return script;
	}
}
