package org.flasck.flas.compiler.jvmgen;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.IntroduceVar;
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
	private Map<IntroduceVar, Var> ivars = new TreeMap<>(IntroduceVar.comparator);
	public Var evalRet;
	public IExpr stateObj;
	public Map<String, IExpr> templateObj;
	private Var renderTree;
	private Var ocret;
	private Var ocmsgs;
	public int ignoreSpecial;

	public FunctionState(MethodDefiner meth, IExpr fcx, IExpr container, Var fargs, IExpr runner) {
		this.meth = meth;
		this.fcx = fcx;
		this.container = container;
		this.fargs = fargs;
	}
	
	public void provideStateObject(IExpr expr) {
		this.stateObj = expr;
	}

	public void provideTemplateObject(Map<String, IExpr> tom) {
		this.templateObj = tom;
	}

	public void provideRenderTree(Var var) {
		this.renderTree = var;
	}

	public void provideOcret(Var ocret, Var ocmsgs) {
		this.ocret = ocret;
		this.ocmsgs = ocmsgs;
	}

	public Var renderTree() {
		return renderTree;
	}
	
	public String nextVar(String pfx) {
		return pfx + nextVar++;
	}

	public void bindVar(JVMBlockCreator currentBlock, String var, Slot s, IExpr from) {
//		IExpr in;
//		AVar avar;
//		if (s instanceof ArgSlot) {
//			ArgSlot as = (ArgSlot)s;
//			if (as.isContainer()) {
//				ignoreContainer = 1;
//				return;
//			}
//			int k = as.argpos() - ignoreContainer;
//			in = meth.arrayItem(J.OBJECT, fargs, k);
//			avar = new Var.AVar(meth, J.OBJECT, "head_" + k);
//		} else if (s instanceof CMSlot || s instanceof HLSlot) {
//			in = from;
//			avar = new Var.AVar(meth, J.OBJECT, "var_" + s.id());
//		} else
//			throw new NotImplementedException();
//		block.add(meth.assign(avar, meth.callInterface(J.OBJECT, fcx, "head", in)));
		if (!(from instanceof AVar)) {
			AVar tmp = new Var.AVar(meth, J.OBJECT, s.toString());
			currentBlock.add(meth.assign(tmp, from));
			from = tmp;
		}
		vars.put(var, (AVar)from);
	}

	public AVar boundVar(String var) {
		return vars.get(var);
	}
	
	public void addMock(UnitDataDeclaration udd, IExpr v) {
		mocks.put(udd, v);
	}

	public IExpr resolveMock(UnitDataDeclaration udd) {
		if (mocks.containsKey(udd))
			return mocks.get(udd);
		else
			throw new NotImplementedException("There is no mock for " + udd);
	}

	public void addIntroduction(IntroduceVar var, Var v) {
		if (ivars.containsKey(var))
			throw new NotImplementedException("Duplicate introduction " + var.name().uniqueName());
		ivars.put(var, v);
	}

	public IExpr resolveIntroduction(IntroduceVar defn) {
		if (ivars.containsKey(defn))
			return ivars.get(defn);
		else
			throw new NotImplementedException("Duplicate introduction " + defn.name().uniqueName());
	}

	public Var ocret() {
		return ocret;
	}

	public Var ocmsgs() {
		return ocmsgs;
	}
}
