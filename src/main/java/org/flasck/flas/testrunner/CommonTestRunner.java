package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

public abstract class CommonTestRunner {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected final Configuration config;
	protected final String compiledPkg;
	protected final String testPkg;
	protected final Map<String, CardDefinition> cdefns = new TreeMap<>();
	protected final List<Expectation> expectations = new ArrayList<>();
	protected final List<Invocation> invocations = new ArrayList<>();
	protected final List<String> errors = new ArrayList<>();
	private final Repository repository;

	public CommonTestRunner(Configuration config, Repository repository) {
		this.config = config;
		this.repository = repository;
		this.testPkg = null;
		this.compiledPkg = null;
	}
	
	public void runAll(Map<File, PrintWriter> writers) {
		repository.traverse(new LeafAdapter() {
			PrintWriter pw;
			
			@Override
			public void visitUnitTestPackage(UnitTestPackage e) {
				String nn = e.name().baseName().replace("_ut_", "");
				File f = new File(nn);
				File out = new File(config.writeTestReportsTo(f), FileUtils.ensureExtension(f.getName(), ".tr"));
				this.pw = writers.get(f);
				try {
					if (pw == null) {
						pw = new PrintWriter(out);
						writers.put(f, pw);
					}
				} catch (FileNotFoundException ex) {
					config.errors.message(((InputPosition)null), "cannot create output file " + out);
				}
			}

			@Override
			public void visitUnitTest(UnitTestCase e) {
				if (pw != null)
					runit(pw, e);
			}
		});
	}

	public void reportErrors(ErrorReporter reporter) {
		for (String s : errors) {
			reporter.message((InputPosition)null, s);
		}
	}

	public abstract void runit(PrintWriter pw, UnitTestCase utc);

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
