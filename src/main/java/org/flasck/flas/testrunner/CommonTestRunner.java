package org.flasck.flas.testrunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public abstract class CommonTestRunner implements TestRunner {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected final String compiledPkg;
	protected final IScope compiledScope;
	protected final String testPkg;
	protected final Map<String, CardDefinition> cdefns = new TreeMap<>();
	protected final List<Expectation> expectations = new ArrayList<>();
	protected final List<Invocation> invocations = new ArrayList<>();
	protected final List<String> errors = new ArrayList<>();

	@Deprecated // the old one for compile()
	public CommonTestRunner(CompileResult cr) {
        this.compiledScope = cr.getScope();
		compiledPkg = cr.getPackage().uniqueName();
		testPkg = compiledPkg + ".script";
	}

	public CommonTestRunner(String compiledPkg, IScope scope, String testPkg) {
		this.compiledPkg = compiledPkg;
		this.compiledScope = scope;
		this.testPkg = testPkg;
	}

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
