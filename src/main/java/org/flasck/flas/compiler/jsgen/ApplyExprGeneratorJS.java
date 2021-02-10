package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSCurryArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LogicHolder;
import org.flasck.flas.parsedForm.MakeAcor;
import org.flasck.flas.parsedForm.MakeSend;
import org.flasck.flas.parsedForm.Messages;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.WithTypeSignature;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.IgnoreMe;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ApplyExprGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final List<JSExpr> stack = new ArrayList<>();

	public ApplyExprGeneratorJS(JSFunctionState state, NestedVisitor nv, JSBlockCreator block) {
		this.state = state;
		this.sv = nv;
		if (block == null)
			throw new NullPointerException("Cannot have a null block");
		this.block = block;
		nv.push(this);
	}

	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, false);
	}
	
	@Override
	public void leaveApplyExpr(ApplyExpr expr) {
		if (expr.args.isEmpty()) { // if it's a single-arg apply, just return the value
			if (stack.size() != 1)
				throw new NotImplementedException("stack should now have size 1");
			sv.result(stack.remove(0));
		} else {
			Object fn = expr.fn;
			WithTypeSignature defn;
			if (fn instanceof UnresolvedVar)
				defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
			else if (fn instanceof TypeReference)
				defn = (WithTypeSignature) ((TypeReference)fn).defn();
			else if (fn instanceof UnresolvedOperator)
				defn = (WithTypeSignature) ((UnresolvedOperator)fn).defn();
			else if (fn instanceof MakeSend)
				defn = (MakeSend) fn;
			else if (fn instanceof MemberExpr) {
				fn = ((MemberExpr)fn).converted();
				if (fn instanceof MakeSend)
					defn = (MakeSend) fn;
				else if (fn instanceof MakeAcor)
					defn = (MakeAcor) fn;
				else if (fn instanceof UnresolvedVar)
					defn = (WithTypeSignature) ((UnresolvedVar)fn).defn();
				else if (fn instanceof TypeReference)
					defn = (WithTypeSignature) ((TypeReference)fn).defn();
				else if (fn instanceof ApplyExpr) {
					defn = (WithTypeSignature) ((UnresolvedVar) ((ApplyExpr)fn).fn).defn();
					makeClosure(null, defn.argCount() - ((ApplyExpr)fn).args.size());
					return;
				} else
					throw new NotImplementedException("unknown operator type: " + fn.getClass());
			} else if (fn instanceof MakeAcor)
				defn = (MakeAcor)fn;
			else
				throw new NotImplementedException("unknown operator type: " + fn.getClass());
			makeClosure(defn, defn.argCount());
		}
	}

	@Override
	public void leaveMessages(Messages msgs) {
		sv.result(block.makeArray(stack.toArray(new JSExpr[stack.size()])));
	}

	// TODO: I think in these first two cases we should also check for explicit currying
	// There certainly isn't implicit currying on the first one which is an Array of unspecified length
	private void makeClosure(WithTypeSignature defn, int expArgs) {
		// First adjust "expected Args" for "hidden" arguments
		if (defn instanceof HandlerImplements) {
			HandlerImplements hi = (HandlerImplements)defn;
			if (hi.getParent() != null) {
				if (state.hasContainer(hi.getParent().name())) {
					expArgs++;
					stack.add(1, state.container(hi.getParent().name()));
				} else {
					// turn the mock into the real thing
					JSExpr mockCard = stack.remove(1);
					stack.add(1, block.unmock(mockCard));
				}
			}
		} else if (defn instanceof ObjectCtor) {
			stack.add(1, state.container(new PackageName("_DisplayUpdater")));
			expArgs++;
			expArgs += ((ObjectCtor)defn).getObject().contracts.size();
		}
		
		// Then divided and conquer for special cases ...
		if (defn instanceof FunctionDefinition && defn.name().uniqueName().equals("()")) {
			stack.remove(0);
			sv.result(block.makeTuple(stack.toArray(new JSExpr[stack.size()])));
		} else if (defn instanceof StructDefn && defn.name().uniqueName().equals("Nil")) {
			stack.remove(0);
			sv.result(block.makeArray(stack));
		} else if (defn instanceof StructDefn && defn.name().uniqueName().equals("Hash")) {
			stack.remove(0);
			sv.result(block.makeHash(stack));
		} else if (defn instanceof StructDefn && stack.size() > 1) {
			// do the creation immediately
			NameOfThing fn = defn.name();
			if (fn.uniqueName().equals("Error"))
				fn = new SolidName(null, "FLError");
			if (stack.size() == expArgs + 2) { // it has a hash on the end
				stack.remove(0);
				JSExpr hash = stack.remove(stack.size()-1);
				JSExpr basic = block.structArgs(fn, stack.toArray(new JSExpr[stack.size()]));
				sv.result(block.applyHash(basic, hash));
			} else if (stack.size() == expArgs + 1) {
				stack.remove(0); // we are supplying the op directly here ...
				sv.result(block.structArgs(fn, stack.toArray(new JSExpr[stack.size()])));
			} else if (stack.size() > expArgs + 2) {
				throw new CantHappenException("this should have been caught by the typechecker: stack has " + stack.size() + " and we expected " + (expArgs+2) + " in " + defn);
			} else
				sv.result(block.curry(false, expArgs, stack.toArray(new JSExpr[stack.size()])));
		} else {
			boolean wantObject = false;
			if (defn instanceof LogicHolder && ((LogicHolder)defn).hasState()) {
				wantObject = true;
			}
			JSExpr[] args = new JSExpr[stack.size()];
			List<XCArg> xcs = new ArrayList<>();
			int k = 0;
			boolean explicit = false;
			for (JSExpr arg : stack) {
				if (arg instanceof JSCurryArg)
					explicit = true;
				else
					xcs.add(new XCArg(k, arg));
				args[k++] = arg;
			}
			JSExpr call;
			if (explicit)
				call = block.xcurry(wantObject, expArgs, xcs);
			else if (stack.size() < expArgs+1)
				call = block.curry(wantObject, expArgs, args);
			else {
				call = block.closure(wantObject, args);
				if (wantObject && state.ocmsgs() != null) { // we are in an object ctor ...
					block.willSplitRWM(call, state.ocmsgs());
				}
			}
			sv.result(call);
		}
	}

	@Override
	public void leaveMakeSend(MakeSend expr) {
		if (stack.size() != 1 && stack.size() != 2)
			throw new NotImplementedException("badly formed stack in makeSend");
		JSExpr obj = stack.remove(0);
		JSExpr handler = stack.isEmpty() ? null : stack.remove(0);
		sv.result(block.makeSend(expr.sendMeth.name, obj, expr.nargs, handler));
	}

	@Override
	public void leaveMakeAcor(MakeAcor expr) {
		if (stack.size() != 1)
			throw new NotImplementedException(); // I don't think I understand this case, yet
		JSExpr obj = stack.remove(0);
		sv.result(block.makeAcor(expr.acorMeth, obj, expr.nargs));
	}
	
	@Override
	public void result(Object r) {
		if (!(r instanceof IgnoreMe))
			stack.add((JSExpr)r);
	}
}
