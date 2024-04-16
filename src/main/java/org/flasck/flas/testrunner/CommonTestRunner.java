package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.st.SystemTest;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.testrunner.CommonTestRunner.CommonState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

public abstract class CommonTestRunner<T extends CommonState> {
	public static class CommonState {
		int failed;
	}

	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected final Configuration config;
	protected final String compiledPkg;
	protected final String testPkg;
	protected final Map<String, CardDefinition> cdefns = new TreeMap<>();
	protected final List<Expectation> expectations = new ArrayList<>();
	protected final List<Invocation> invocations = new ArrayList<>();
	protected final List<String> errors = new ArrayList<>();
	protected final Repository repository;

	public CommonTestRunner(Configuration config, Repository repository) {
		this.config = config;
		this.repository = repository;
		this.testPkg = null;
		this.compiledPkg = null;
	}
	
	public void runAllUnitTests(Map<File, TestResultWriter> writers) {
		repository.traverse(new LeafAdapter() {
			TestResultWriter pw;
			
			@Override
			public void visitUnitTestPackage(UnitTestPackage e) {
				String nn = e.name().baseName().replace("_ut_", "");
				File f = new File(nn);
				this.pw = writers.get(f);
				if (pw == null) {
					File trd = config.writeTestReportsTo();
					if (trd == null) {
						pw = new TestResultWriter(false, System.out);
					} else {
						FileUtils.assertDirectory(trd);
						File out = new File(trd, FileUtils.ensureExtension(f.getName(), ".tr"));
						try {
							pw = new TestResultWriter(true, out);
						} catch (FileNotFoundException ex) {
							config.errors.message(((InputPosition)null), "cannot create output file " + out);
						}
					}
					if (pw != null)
						writers.put(f, pw);
				}
			}

			@Override
			public void visitUnitTest(UnitTestCase e) {
				if (pw != null)
					runUnitTest(pw, e);
			}
		});
	}

	public void runAllSystemTests(Map<File, TestResultWriter> writers) {
		repository.traverse(new LeafAdapter() {
			TestResultWriter pw;
			
			@Override
			public void visitSystemTest(SystemTest e) {
				String nn = e.name().baseName().replaceFirst(".*_st_", "");
				File f = new File(nn);
				this.pw = writers.get(f);
				if (pw == null) {
					File trd = config.writeTestReportsTo();
					if (trd == null) {
						pw = new TestResultWriter(false, System.out);
					} else {
						FileUtils.assertDirectory(trd);
						File out = new File(trd, FileUtils.ensureExtension(f.getName(), ".tr"));
						try {
							pw = new TestResultWriter(true, out);
						} catch (FileNotFoundException ex) {
							config.errors.message(((InputPosition)null), "cannot create output file " + out);
						}
					}
					if (pw != null)
						writers.put(f, pw);
				}
				if (pw != null)
					runSystemTest(pw, e);
			}
		});
	}

	public void reportErrors(ErrorReporter reporter) {
		for (String s : errors) {
			reporter.message((InputPosition)null, s);
		}
	}

	public abstract void runUnitTest(TestResultWriter pw, UnitTestCase utc);

	public void runSystemTest(TestResultWriter pw, SystemTest st) {
		logger.info(this.getClass().getSimpleName() + " running system test " + st);
//		if (this.getClass().getSimpleName().equals("JVMRunner")) {
//			logger.error("Ha! Not!");
//			return;
//		}
		T state = createSystemTest(pw, st);
		if (state == null)
			return;
		if (st.configure != null)
			runSystemTestStage(pw, state, st, st.configure);
		for (SystemTestStage e : st.stages) {
			runSystemTestStage(pw, state, st, e);
		}
		if (st.cleanup != null)
			runSystemTestStage(pw, state, st, st.cleanup);
		cleanupSystemTest(pw, state, st);
	}

	protected abstract T createSystemTest(TestResultWriter pw, SystemTest st);

	protected abstract void runSystemTestStage(TestResultWriter pw, T state, SystemTest st, SystemTestStage e);

	protected abstract void cleanupSystemTest(TestResultWriter pw, T state, SystemTest st);

	protected void assertAllInvocationsCalled() {
		for (Invocation ii : invocations)
			System.out.println("Should have expected: " + ii);
		invocations.clear();
		
		for (Expectation ii : expectations)
			System.out.println("Expected, not called: " + ii);
		if (!expectations.isEmpty())
			throw new UtilException("Not all expectations happened");
	}
}
