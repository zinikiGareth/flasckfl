package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public interface JSFunctionState {
	public JSExpr container();
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo);
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd);
	public void addIntroduction(IntroduceVar var, JSExpr jsv);
	JSExpr resolveIntroduction(IntroduceVar var);
}
