package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.parser.ut.UnitDataDeclaration;

public interface JSFunctionState {
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo);
	public JSExpr resolveMock(UnitDataDeclaration udd);
}
