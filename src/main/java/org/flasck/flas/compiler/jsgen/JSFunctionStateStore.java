package org.flasck.flas.compiler.jsgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.parser.ut.UnitDataDeclaration;

public class JSFunctionStateStore implements JSFunctionState{
	public Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();

	@Override
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo) {
		mocks.put(udd, resolvesTo);
	}

	@Override
	public JSExpr resolveMock(UnitDataDeclaration udd) {
		return mocks.get(udd);
	}

}
