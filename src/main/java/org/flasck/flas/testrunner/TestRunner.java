package org.flasck.flas.testrunner;

import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.parsedForm.Scope;

public interface TestRunner {
	void prepareScript(ScriptCompiler compiler, Scope scope);
	void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception;
	void createCardAs(String cardType, String bindVar);
}
