package org.flasck.flas.compiler.jsgen;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ContractDeclDir;
import org.flasck.flas.parser.ut.UnitDataDeclaration;

public class JSFunctionStateStore implements JSFunctionState {
	public Map<UnitDataDeclaration, JSExpr> mocks = new TreeMap<>();
	private Set<UnitDataDeclaration> globalMocks;

	public JSFunctionStateStore(Set<UnitDataDeclaration> globalMocks) {
		this.globalMocks = globalMocks;
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
			JSExpr ret = block.mockContract(((ContractDeclDir) udd.ofType.defn()).name());
			mocks.put(udd, ret); // to share for subsequent refs
			return ret;
		} else
			throw new RuntimeException("No mock for " + udd);
	}

}
