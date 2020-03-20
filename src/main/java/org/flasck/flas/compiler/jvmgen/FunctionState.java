package org.flasck.flas.compiler.jvmgen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.CMSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ContractDeclDir;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.Var.AVar;
import org.zinutils.exceptions.NotImplementedException;

public class FunctionState {
	final MethodDefiner meth;
	final IExpr fcx;
	final IExpr container;
	final Var fargs;
	private int nextVar = 1;
	private Map<String, AVar> vars = new HashMap<>();
	private Map<UnitDataDeclaration, IExpr> mocks = new TreeMap<UnitDataDeclaration, IExpr>();
	public Var evalRet;
	private Set<UnitDataDeclaration> globalMocks = new HashSet<UnitDataDeclaration>();
	private final IExpr runner;
	public IExpr stateObj;

	public FunctionState(MethodDefiner meth, IExpr fcx, IExpr container, Var fargs, IExpr runner, Set<UnitDataDeclaration> globalMocks) {
		this.meth = meth;
		this.fcx = fcx;
		this.container = container;
		this.fargs = fargs;
		this.runner = runner;
		if (globalMocks != null)
			this.globalMocks.addAll(globalMocks);
	}
	
	public void provideStateObject(IExpr expr) {
		this.stateObj = expr;
	}

	public String nextVar(String pfx) {
		return pfx + nextVar++;
	}

	public void bindVar(List<IExpr> block, String var, Slot s, IExpr from) {
		IExpr in;
		AVar avar;
		if (s instanceof ArgSlot) {
			int k = ((ArgSlot)s).argpos();
			in = meth.arrayItem(J.OBJECT, fargs, k);
			avar = new Var.AVar(meth, J.OBJECT, "head_" + k);
		} else if (s instanceof CMSlot) {
			in = from;
			avar = new Var.AVar(meth, J.OBJECT, "var_" + s.id());
		} else
			throw new NotImplementedException();
		block.add(meth.assign(avar, meth.callInterface(J.OBJECT, fcx, "head", in)));
		vars.put(var, avar);
	}

	public AVar boundVar(String var) {
		return vars.get(var);
	}
	
	public void addMock(UnitDataDeclaration udd, Var v) {
		mocks.put(udd, v);
	}

	public IExpr resolveMock(UnitDataDeclaration udd) {
		if (mocks.containsKey(udd))
			return mocks.get(udd);
		else if (globalMocks.contains(udd)) {
			ContractDeclDir cdd = (ContractDeclDir)udd.ofType.defn();
			IExpr mc = meth.callInterface(J.OBJECT, fcx, "mockContract", meth.classConst(cdd.name().javaClassName()));
			Var v = meth.avar(J.OBJECT, nextVar("v"));
			meth.assign(v, mc).flush();
			mocks.put(udd, v);
			return v;
		} else
			throw new NotImplementedException("There is no mock for " + udd);
	}
}
