package org.flasck.flas.compiler.jvmgen;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class ObjectCtorStateGenerator extends LeafAdapter implements ResultAware {
	private final FunctionState state;
	private final StackVisitor sv;
	private final List<IExpr> currentBlock;
	private Var ret;
	private IExpr fieldValue;

	public ObjectCtorStateGenerator(FunctionState fs, StackVisitor sv, ObjectDefn object, List<IExpr> currentBlock, Var ocret) {
		this.state = fs;
		this.sv = sv;
		this.currentBlock = currentBlock;
		this.ret = ocret;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(state, sv, currentBlock, false);
	}
	
	@Override
	public void result(Object r) {
		fieldValue = (IExpr) r;
	}

	@Override
	public void leaveStructField(StructField sf) {
		MethodDefiner meth = state.meth;
		if (fieldValue != null) {
			currentBlock.add(meth.callVirtual("void", ret, "set", meth.stringConst(sf.name), meth.as(fieldValue, J.OBJECT)));
			this.fieldValue = null;
		}
	}
	
	@Override
	public void leaveStateDefinition(StateDefinition state) {
		sv.result(null);
	}
}
