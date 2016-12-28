package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public interface TestScriptBuilder {
	void error(InputPosition posn, String msg);

	void addAssert(InputPosition evalPos, Object eval, InputPosition pos, Object valueExpr);
	void addCreate(InputPosition at, String bindVar, String cardType);
	void addMatch(InputPosition posn, WhatToMatch what, String selector, String contents);
	void addSend(InputPosition posn, String card, String contract, String method, List<Object> exprs, List<Expectation> expecting);

	void addTestCase(String message);

	boolean hasErrors();
}
