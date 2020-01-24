package org.flasck.flas.testrunner;

import java.util.List;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.jvm.FLEvalContext;
import org.ziniki.ziwsh.intf.IdempotentHandler;

public interface TestRunner {
	String name();
//	void prepareScript(String scriptPkg, Scope scope);
//	void prepareCase();
	void assertCorrectValue(int exprId) throws Exception;
	void createCardAs(CardName cardType, String bindVar) throws Exception;
	void invoke(FLEvalContext cx, Object sendExpr) throws Exception;
	void send(FLEvalContext cx, IdempotentHandler ih, String cardVar, String contractName, String methodName, List<Integer> posns) throws Exception;
	void event(String cardVar, String methodName) throws Exception;
	void match(HTMLMatcher matcher, String selector) throws NotMatched;
	void expect(String cardVar, String ctr, String method, List<Integer> chkargs);
	void click(String selector);
}
