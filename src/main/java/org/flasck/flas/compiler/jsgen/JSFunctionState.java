package org.flasck.flas.compiler.jsgen;

import java.util.Map;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public interface JSFunctionState {
	public JSMethodCreator meth();
	public Map<UnitDataDeclaration, JSExpr> mocks();
	public boolean hasContainer(NameOfThing name);
	public void container(NameOfThing name, JSExpr expr);
	public JSExpr container(NameOfThing name);
	public void provideTemplateObject(Map<String, JSExpr> tom);
	public Map<String, JSExpr> templateObj();
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo);
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd);
	public void addIntroduction(IntroduceVar var, JSExpr jsv);
	JSExpr resolveIntroduction(IntroduceVar var);
	void objectCtor(JSExpr ocret, JSExpr ocmsgs);
	public JSExpr ocret();
	public JSExpr ocmsgs();
	public Map<IntroduceVar, JSExpr> introductions();
	public Map<NameOfThing, JSExpr> containers();
	public Map<String, JSExpr> applications();
	public void application(String name, JSExpr field);
	public JSExpr application(String asVar);
	public void cacheResult();
	public boolean shouldCacheResult();
}
