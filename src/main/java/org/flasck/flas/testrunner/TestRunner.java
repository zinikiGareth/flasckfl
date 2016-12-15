package org.flasck.flas.testrunner;

import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.testrunner.MatchStep.WhatToMatch;

public interface TestRunner {
	void prepareScript(ScriptCompiler compiler, Scope scope);
	void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception;
	void createCardAs(String cardType, String bindVar);
	void match(WhatToMatch what, String cardVar, String selector, String contents);
}
