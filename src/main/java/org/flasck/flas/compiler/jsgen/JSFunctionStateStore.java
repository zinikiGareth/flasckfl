package org.flasck.flas.compiler.jsgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.zinutils.exceptions.NotImplementedException;

public class JSFunctionStateStore implements JSFunctionState {
	public Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();
	public Map<IntroduceVar, JSExpr> introductions = new TreeMap<>(IntroduceVar.comparator);
	private final JSExpr container;

	public JSFunctionStateStore(JSExpr container) {
		this.container = container;
	}

	@Override
	public JSExpr container() {
		return container;
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
