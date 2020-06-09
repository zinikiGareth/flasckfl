package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class DoUTEventGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private final IExpr runner;
	private final MethodDefiner meth;
	private final JVMBlockCreator block;
	private final List<IExpr> args = new ArrayList<>();

	public DoUTEventGenerator(StackVisitor sv, FunctionState fs, IExpr runner, JVMBlockCreator block) {
		this.sv = sv;
		this.fs = fs;
		this.runner = runner;
		this.block = block;
		this.meth = fs.meth;
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(fs, sv, block, false).visitExpr(expr, nArgs);
	}
	
	@Override
	public void result(Object r) {
		IExpr expr = (IExpr) r;
		this.args.add(expr);
	}
	
	@Override
	public void leaveUnitTestEvent(UnitTestEvent e) {
		if (args.size() != 2)
			throw new RuntimeException("expected card & event");
		IExpr eventZone = makeSelector(this.block, meth, e.targetZone);
		this.block.add(this.meth.callInterface("void", runner, "event", fs.fcx, args.get(0), eventZone, args.get(1)));
		sv.result(null);
	}

	public static IExpr makeSelector(JVMBlockCreator block, MethodDefiner meth, TargetZone targetZone) {
		Var eventZone = meth.avar(List.class.getName(), "ez");
		block.add(meth.assign(eventZone, meth.makeNew(ArrayList.class.getName())));
		
		for (int i=0;i<targetZone.fields.size();i++) {
			block.add(meth.voidExpr(meth.callInterface("boolean", eventZone, "add", meth.as(meth.makeNew(J.EVENTZONE, meth.stringConst(targetZone.types().get(i).toString().toLowerCase()), makeEventZone(meth, targetZone.fields.get(i))), J.OBJECT))));
		}
		return eventZone;
	}

	public static IExpr makeEventZone(MethodDefiner meth, Object o) {
		if (o instanceof String)
			return meth.as(meth.stringConst((String)o), J.OBJECT);
		else
			return meth.as(meth.box(meth.intConst((Integer)o)), J.OBJECT);
	}
}
