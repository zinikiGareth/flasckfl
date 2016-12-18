package org.flasck.flas.testrunner;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.parsedForm.Scope;

public interface TestRunner {
	void prepareScript(ScriptCompiler compiler, Scope scope);
	void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception;
	void createCardAs(CardName cardType, String bindVar);
	void send();
	void match(WhatToMatch what, String selector, String contents) throws NotMatched;
}
