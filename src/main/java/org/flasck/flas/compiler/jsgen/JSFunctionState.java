package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public interface JSFunctionState {
	enum StateLocation { NONE, LOCAL, CARD };
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo);
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd);
	void setStateLocation(StateLocation loc);
	StateLocation stateLocation();
	public void addIntroduction(IntroduceVar var, JSExpr jsv);
	JSExpr resolveIntroduction(IntroduceVar var);
}
