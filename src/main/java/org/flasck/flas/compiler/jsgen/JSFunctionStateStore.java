package org.flasck.flas.compiler.jsgen;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public class JSFunctionStateStore implements JSFunctionState {
	public Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();
	private Set<UnitDataDeclaration> globalMocks;
	private StateLocation stateLoc;

	public JSFunctionStateStore(Set<UnitDataDeclaration> globalMocks, StateLocation loc) {
		this.globalMocks = globalMocks;
		this.stateLoc = loc;
	}

	@Override
	public void addMock(UnitDataDeclaration udd, JSExpr resolvesTo) {
		mocks.put(udd, resolvesTo);
	}

	@Override
	public JSExpr resolveMock(JSBlockCreator block, UnitDataDeclaration udd) {
		if (mocks.containsKey(udd))
			return mocks.get(udd);
		else if (globalMocks.contains(udd)) {
			JSExpr ret = block.mockContract(((ContractDecl) udd.ofType.defn()).name());
			mocks.put(udd, ret); // to share for subsequent refs
			return ret;
		} else
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
