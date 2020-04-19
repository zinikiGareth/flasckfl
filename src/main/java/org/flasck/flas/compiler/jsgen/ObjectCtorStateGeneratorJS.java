package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class ObjectCtorStateGeneratorJS extends LeafAdapter implements ResultAware {

	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private JSExpr fieldValue;

	public ObjectCtorStateGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, false);
	}

	@Override
	public void result(Object r) {
		fieldValue = (JSExpr) r;
	}
	
	@Override
	public void leaveStructField(StructField sf) {
		if (fieldValue != null) {
			block.storeField(state.ocret(), sf.name, fieldValue);
			this.fieldValue = null;
		}
	}

	@Override
	public void leaveStateDefinition(StateDefinition sd) {
		sv.result(null);
	}
}
