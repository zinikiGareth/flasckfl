package org.flasck.flas.testrunner;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.CompileResult;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.UtilException;

public abstract class CommonTestRunner implements TestRunner {
	protected static Logger logger = LoggerFactory.getLogger("TestRunner");
	protected final CompileResult prior;
	protected final String testPkg;
	protected String spkg;
	protected final Map<String, CardDefinition> cdefns = new TreeMap<>();

	public CommonTestRunner(CompileResult cr) {
        this.prior = cr;
		testPkg = prior.getPackage().uniqueName();
	}

	protected String fullName(String name) {
		return prior.getScope().fullName(name);
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
}
