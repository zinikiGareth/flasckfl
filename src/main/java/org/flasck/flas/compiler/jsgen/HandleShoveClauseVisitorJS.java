package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.zinutils.exceptions.NotImplementedException;

public class HandleShoveClauseVisitorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;
	private JSExpr root;
	private UnresolvedVar slot;
	private JSExpr value;

	public HandleShoveClauseVisitorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		sv.push(this);
	}
	
	@Override
	public void visitShoveSlot(UnresolvedVar v) {
		if (this.root == null) {
			// v should be an instance of UDD
			UnitDataDeclaration udd = (UnitDataDeclaration) v.defn();
			this.root = state.resolveMock(block, udd);
		} else {
			if (this.slot != null) {
				// Incorporate the existing slot into the expression for root
				// I think this involves _probe_state the first time, then some kind of object traversal
				// but it probably depends on the field types
				throw new NotImplementedException("Following the path through the shoved object");
			}
			this.slot = v;
		}
	}
	
	@Override
	public void visitShoveExpr(Expr e) {
		new ExprGeneratorJS(state, sv, block, false);
	}

	@Override
	public void result(Object r) {
		value = (JSExpr) r;
	}

	@Override
	public void leaveUnitTestShove(UnitTestShove s) {
		block.assertable(runner, "shove", this.root, block.string(slot.var), value);
		sv.result(null);
	}
}
