package org.flasck.flas.testrunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResultException;
import org.flasck.flas.parsedForm.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.utils.FileUtils;

public class UnitTestRunner {
	public final static Logger logger = LoggerFactory.getLogger("UnitTests");
	private final ErrorReporter errors;
	private final List<UnitTestResultHandler> handlers = new ArrayList<>();

	public UnitTestRunner(ErrorReporter errors) {
		this.errors = errors;
	}
	
	public void sendResultsTo(UnitTestResultHandler resultHandler) {
		handlers.add(resultHandler);
	}

	public TestScript prepare(TestRunner runner, String scriptPkg, Scope compiledScope, File f) throws ErrorResultException {
//		String scriptPkg = prior.getPackage().uniqueName() + ".script";
		TestScript script = convertScript(errors, compiledScope, scriptPkg, f);
		if (errors.hasErrors())
			throw new ErrorResultException(errors);
//		runner.prepareScript(scriptPkg, script.scope());
		return script;
	}

	public void run(TestRunner runner, TestScript script) {
		script.runAllTests(new TestCaseRunner() {
			@Override
			public void run(SingleTestCase tc) {
				try {
//					runner.prepareCase();
					runCase(runner, tc);
				} catch (FlasTestException ex) {
					logger.error("AssertFailed: " + ex.toString());
					for (UnitTestResultHandler h : handlers)
						h.testFailed(tc.getDescription(), runner.name(), ex.getExpected(), ex.getActual());
				} catch (MultiException ex) {
					logger.error("Exceptions raised: " + ex.toString());
					for (String s : ex.allErrors())
						for (UnitTestResultHandler h : handlers)
							h.testError(tc.getDescription(), runner.name(), s);
				} catch (Exception ex) {
					logger.error("Exceptions thrown: " + ex.toString());
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
	public static TestScript convertScript(ErrorReporter errors, Scope scope, String scriptPkg, File f) {
		TestScript script = new TestScript(errors, scope, scriptPkg);
		UnitTestConvertor c = new UnitTestConvertor(script);
		c.convert(FileUtils.readFileAsLines(f));
		return script;
	}
}
