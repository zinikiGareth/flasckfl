package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class UDDGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final JSMethodCreator meth;
	private final JSFunctionState state;
	private final JSBlockCreator block;
	private JSExpr assigned;
	private boolean assigning;

	public UDDGeneratorJS(NestedVisitor sv, JSMethodCreator meth, JSFunctionState state, JSBlockCreator block) {
		this.sv = sv;
		this.meth = meth;
		this.state = state;
		this.block = block;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, false);
	}
	
	@Override
	public void visitUnitDataField(Assignment assign) {
		assigning = true;
	}
	
	@Override
	public void result(Object r) {
		if (!assigning)
			this.assigned = (JSExpr) r;
	}

	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		JSExpr value;
		if (assigned != null) {
			value = assigned;
		} else {
			value = meth.createObject(udd.ofType.defn().name());
		}
		JSExpr newMock = block.storeMockObject(udd, value);
		// I think this is where we would then want to do the assigning of fields ...
		state.addMock(udd, newMock);
		
		sv.result(null);
	}
}