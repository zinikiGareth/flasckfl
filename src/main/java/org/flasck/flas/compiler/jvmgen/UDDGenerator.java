package org.flasck.flas.compiler.jvmgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.NamedType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class UDDGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private final MethodDefiner meth;
	private final JVMBlockCreator currentBlock;
	private boolean assigning;
	private IExpr assigned;

	public UDDGenerator(StackVisitor sv, FunctionState fs, JVMBlockCreator currentBlock) {
		this.sv = sv;
		this.fs = fs;
		this.meth = fs.meth;
		this.currentBlock = currentBlock;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(fs, sv, currentBlock, false);
	}
	
	@Override
	public void visitUnitDataField(Assignment assign) {
		this.assigning = true;
	}
	
	@Override
	public void result(Object r) {
		if (!assigning)
			assigned = (IExpr) r;
	}
	
	@Override
	public void leaveUnitDataDeclaration(UnitDataDeclaration udd) {
		IExpr value;
		if (assigned != null) {
			value = assigned;
		} else {
			NamedType objty = udd.ofType.defn();
			value = meth.callStatic(objty.name().javaName(), J.OBJECT, "eval", this.fs.fcx);
		}
		Var v = meth.avar(J.OBJECT, fs.nextVar("v"));
		IExpr sm = meth.callInterface(J.OBJECT, fs.fcx, "storeMock", meth.as(value, J.OBJECT));
		currentBlock.add(meth.assign(v, sm));
//		currentBlock.convert().flush();
		fs.addMock(udd, v);
		sv.result(null);
	}

}
