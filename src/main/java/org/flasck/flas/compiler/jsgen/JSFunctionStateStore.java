package org.flasck.flas.compiler.jsgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.zinutils.exceptions.NotImplementedException;

public class JSFunctionStateStore implements JSFunctionState {
	public final Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();
	public final Map<IntroduceVar, JSExpr> introductions = new TreeMap<>(IntroduceVar.comparator);
	private final JSExpr container;
	private Map<String, JSExpr> templateObj;
	private final JSMethodCreator meth;
	private JSExpr ocret;

	public JSFunctionStateStore(JSMethodCreator meth, JSExpr container) {
		this.meth = meth;
		this.container = container;
	}

	@Override
	public JSMethodCreator meth() {
		return meth;
	}
	
	@Override
	public JSExpr container() {
		return container;
	}
	
	public void provideTemplateObject(Map<String, JSExpr> tc) {
		this.templateObj = tc;
	}

	@Override
	public Map<String, JSExpr> templateObj() {
		return templateObj;
	}

	@Override
	public void objectCtor(JSExpr ocret) {
		this.ocret = ocret;
	}
	
	@Override
	public JSExpr ocret() {
		return ocret;
	}

	@Override
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo) {
		if (mocks.containsKey(udd))
			throw new NotImplementedException("Duplicate mock " + udd.name.uniqueName());
		mocks.put(udd, resolvesTo);
	}

	@Override
	public void addIntroduction(IntroduceVar var, JSExpr jsv) {
		if (introductions.containsKey(var))
			throw new NotImplementedException("Duplicate introduction " + var.name().uniqueName());
		introductions.put(var, jsv);
	}

	@Override
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd) {
		if (mocks.containsKey(udd))
			return mocks.get(udd);
		else
			throw new RuntimeException("No mock for " + udd);
	}

	@Override
	public JSExpr resolveIntroduction(IntroduceVar var) {
		if (introductions.containsKey(var))
			return introductions.get(var);
		else
			throw new RuntimeException("No introduction for " + var);
	}
}
