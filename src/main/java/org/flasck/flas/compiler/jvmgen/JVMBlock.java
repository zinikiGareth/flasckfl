package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.jvm.J;
import org.zinutils.bytecode.ArrayOfExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodInvocation;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.UseAsType;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class JVMBlock implements JVMBlockCreator {
	private final List<IExpr> stmts = new ArrayList<>();
	private final NewMethodDefiner meth;
	private final FunctionState state;
	private final Map<String, Var> stashed;
	private final Map<IExpr, Var> closures;
	private boolean dead = false;
	
	public JVMBlock(NewMethodDefiner meth, FunctionState state) {
		this.meth = meth;
		this.state = state;
		this.stashed = new HashMap<String, Var>();
		this.closures = new HashMap<IExpr, Var>();
	}

	public JVMBlock(JVMBlockCreator currentBlock) {
		this.meth = currentBlock.method();
		this.state = currentBlock.state();
		this.stashed = new HashMap<>(currentBlock.stashed());
		this.closures = new HashMap<IExpr, Var>(currentBlock.closures());
	}

	@Override
	public NewMethodDefiner method() {
		return meth;
	}

	@Override
	public FunctionState state() {
		return state;
	}

	@Override
	public Map<String, Var> stashed() {
		return stashed;
	}

	@Override
	public Map<IExpr, Var> closures() {
		return closures;
	}

	@Override
	public void add(IExpr stmt) {
		if (dead)
			throw new CantHappenException("This block has been converted and may not be used further: " + this);
		stmts.add(stmt);
	}

	@Override
	public boolean isEmpty() {
		return stmts.isEmpty();
	}
	
	@Override
	public IExpr removeLast() {
		return stmts.remove(stmts.size()-1);
	}

	@Override
	public IExpr hasStashed(String myName) {
		return stashed.get(myName);
	}

	@Override
	public IExpr stash(String myName, IExpr e) {
		Var v = meth.avar(J.OBJECT, state.nextVar("stash"));
		stmts.add(meth.assign(v, e));
		stashed.put(myName, v);
		return v;
	}

	@Override
	public IExpr hasClosure(boolean wantObject, IExpr fn, IExpr args) {
		for (Entry<IExpr, Var> e : closures.entrySet()) {
			if (isSameAs(e.getKey(), wantObject, fn, args))
				return e.getValue();
		}
		return null;
	}

	private boolean isSameAs(IExpr curr, boolean wantObject, IExpr fn, IExpr args) {
		MethodInvocation mi = (MethodInvocation) curr;
		if (wantObject != mi.name().equals("oclosure"))
			return false;
		ArrayOfExpr al = (ArrayOfExpr) args;
		UseAsType uat = (UseAsType) mi.arguments()[0];
		IExpr cfn = uat.expr();
		if (cfn != fn)
			return false;
		ArrayOfExpr cae = (ArrayOfExpr) mi.arguments()[1];
		if (al.arguments().size() != cae.arguments().size())
			return false;
		for (int i=0;i<al.arguments().size();i++) {
			IExpr ai = al.arguments().get(i);
			IExpr ci = cae.arguments().get(i);
			if (ai != ci)
				return false;
		}
		return true;
	}

	@Override
	public Var saveClosure(boolean wantObject, IExpr call) {
		Var v = meth.avar(J.FLCLOSURE, state.nextVar("cmn"));
		stmts.add(meth.assign(v, call));
		closures.put(call, v);
		return v;
	}

	@Override
	public IExpr singleton() {
		if (stmts.size() != 1)
			throw new RuntimeException("Multiple result expressions");
		IExpr ret = stmts.get(0);
		stmts.clear();
		return ret;
	}

	@Override
	public IExpr convert() {
		IExpr ret;
		if (stmts.isEmpty())
			throw new NotImplementedException("there must be at least one statement in a block");
		else if (stmts.size() == 1)
			ret = stmts.get(0);
		else
			ret = meth.block(stmts.toArray(new IExpr[stmts.size()]));
		this.dead = true;
		return ret;
	}
}
