package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.ScriptCompiler;
import org.flasck.flas.parsedForm.Scope;

public interface TestRunner {
	void prepareScript(ScriptCompiler compiler, Scope scope);
	void prepareCase();
	void assertCorrectValue(int exprId) throws ClassNotFoundException, Exception;
	void createCardAs(CardName cardType, String bindVar);
	void send(String cardVar, String contractName, String methodName, List<Object> args);
	void match(WhatToMatch what, String selector, String contents) throws NotMatched;
}
