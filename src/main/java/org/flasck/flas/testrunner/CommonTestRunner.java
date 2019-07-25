package org.flasck.flas.testrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.Configuration;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

public abstract class CommonTestRunner implements TestRunner {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected final Configuration config;
	protected final String compiledPkg;
	protected final IScope compiledScope;
	protected final String testPkg;
	protected final Map<String, CardDefinition> cdefns = new TreeMap<>();
	protected final List<Expectation> expectations = new ArrayList<>();
	protected final List<Invocation> invocations = new ArrayList<>();
	protected final List<String> errors = new ArrayList<>();
	private final Repository repository;

	@Deprecated // the old one for compile()
	public CommonTestRunner(CompileResult cr) {
        this.compiledScope = cr.getScope();
		compiledPkg = cr.getPackage().uniqueName();
		testPkg = compiledPkg + ".script";
		this.config = null;
		this.repository = null;
	}

	public CommonTestRunner(String compiledPkg, IScope scope, String testPkg) {
		this.compiledPkg = compiledPkg;
		this.compiledScope = scope;
		this.testPkg = testPkg;
		this.config = null;
		this.repository = null;
	}

	public CommonTestRunner(Configuration config, Repository repository) {
		this.config = config;
		this.repository = repository;
		this.testPkg = null;
		this.compiledPkg = null;
		this.compiledScope = null;
	}
	
	public void runAll() {
		Map<File, PrintWriter> writers = new HashMap<>();
		repository.traverse(new LeafAdapter() {
			@Override
			public void visitUnitTest(UnitTestCase e) {
				UnitTestName n = e.name;
				UnitTestFileName cont = (UnitTestFileName) n.container();
				String nn = cont.baseName().replace("_ut_", "");
				File f = new File(nn);
				run(writers, f, e);
			}
		});
		writers.values().forEach(w -> w.close());
	}

	private void run(Map<File, PrintWriter> writers, File f, UnitTestCase utc) {
		File out = new File(config.writeTestReportsTo(f), FileUtils.ensureExtension(f.getName(), ".tr"));
		PrintWriter pw = writers.get(f);
		try {
			if (pw == null) {
				pw = new PrintWriter(out);
				writers.put(f, pw);
			}
			runit(pw, utc);
			pw.flush();
		} catch (FileNotFoundException ex) {
			config.errors.message(((InputPosition)null), "cannot create output file " + out);
		}
	}

	protected abstract void runit(PrintWriter pw, UnitTestCase utc);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void expect(String cardVar, String ctr, String method, List<Integer> chkargs) {
		expectations.add(new Expectation(fullName(ctr), method, (List)chkargs));
	}

	protected String getFullContractNameForCard(String cardVar, String contractName, String methodName) {
		CardDefinition cd = cdefns.get(cardVar);
		String fullName = fullName(contractName);
		ContractImplements ctr = null;
		for (ContractImplements x : cd.contracts)
			if (x.name().equals(contractName) || x.name().equals(fullName))
				ctr = x;
		if (ctr == null)
			throw new UtilException("the card '" + cardVar + "' does not have the contract '" + contractName +"'");
	
		MethodCaseDefn meth = null;
		for (MethodCaseDefn q : ctr.methods) {
			if (q.methodName().name.equals(methodName))
				meth = q;
		}
		if (meth == null)
			throw new UtilException("the contract '" + contractName + "' does not have the method '" + methodName +"'");
		return fullName;
	}

	protected String fullName(String name) {
		return compiledScope.fullName(name);
	}

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
