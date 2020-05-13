package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestShove;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.exceptions.NotImplementedException;

public class HandleShoveClauseVisitor extends LeafAdapter implements ResultAware {

	private final StackVisitor sv;
	private final FunctionState fs;
	private final IExpr runner;
	private List<IExpr> block = new ArrayList<>();
	private IExpr root;
	private UnresolvedVar slot;
	private IExpr value;

	public HandleShoveClauseVisitor(StackVisitor sv, FunctionState fs, IExpr runner) {
		this.sv = sv;
		this.fs = fs;
		this.runner = runner;
		sv.push(this);
	}

	@Override
	public void visitShoveSlot(UnresolvedVar v) {
		if (this.root == null) {
			// v should be an instance of UDD
			UnitDataDeclaration udd = (UnitDataDeclaration) v.defn();
			this.root = fs.resolveMock(udd);
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
		new ExprGenerator(fs, sv, block, false);
	}

	@Override
	public void result(Object r) {
		value = (IExpr) r;
	}
	
	@Override
	public void leaveUnitTestShove(UnitTestShove s) {
		IExpr ret = fs.meth.callInterface("void", runner, "shove", fs.fcx, fs.meth.as(this.root, J.OBJECT), fs.meth.stringConst(slot.var), fs.meth.as(value, J.OBJECT));
		block.add(ret);
		JVMGenerator.makeBlock(fs.meth, block).flush();
		sv.result(null);
	}
}
