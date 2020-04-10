package org.flasck.flas.compiler.jsgen;

import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.zinutils.exceptions.NotImplementedException;

public class JSFunctionStateStore implements JSFunctionState {
	public Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();
	private StateLocation stateLoc;

	public JSFunctionStateStore(StateLocation loc) {
		this.stateLoc = loc;
	}

	@Override
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo) {
		if (mocks.containsKey(udd))
			throw new NotImplementedException("Duplicate mock " + udd.name.uniqueName());
		mocks.put(udd, resolvesTo);
	}

	@Override
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd) {
		if (mocks.containsKey(udd))
			return mocks.get(udd);
		else
			throw new RuntimeException("No mock for " + udd);
	}

	@Override
	public void setStateLocation(StateLocation loc) {
		this.stateLoc = loc;
	}

	@Override
	public StateLocation stateLocation() {
		return stateLoc;
	}
}
