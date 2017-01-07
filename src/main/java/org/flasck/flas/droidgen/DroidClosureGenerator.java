package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.exceptions.UtilException;

public class DroidClosureGenerator {
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private VarHolder vh;

	public DroidClosureGenerator(HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.form = form;
		this.meth = meth;
		this.vh = vh;
	}

	public IExpr closure(HSIEBlock closure) {
		HSIEBlock c0 = closure.nestedCommands().get(0);
		if (c0 instanceof PushExternal && ((PushExternal)c0).fn.uniqueName().equals("FLEval.field")) {
			return handleField(closure);
		} else {
		// Process all the arguments
			int pos = 0;
			List<Expr> al = new ArrayList<Expr>();
			for (HSIEBlock b : closure.nestedCommands()) {
				PushReturn c = (PushReturn) b;
				al.add(upcast(appendValue(c, pos)));
				pos++;
			}
		
		
		
		// Loop over everything in the closure pushing it onto the stack (in al)
		if (c0 instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)c0).fn;
			Expr needsObject = null;
			boolean fromHandler = form.needsCardMember();
			Object defn = fn;
			if (fn != null) {
				while (defn instanceof PackageVar)
					defn = ((PackageVar)defn).defn;
				if (defn instanceof ObjectReference || defn instanceof CardFunction) {
					needsObject = meth.myThis();
					fromHandler |= fn.fromHandler();
				} else if (defn instanceof RWHandlerImplements) {
					RWHandlerImplements hi = (RWHandlerImplements) defn;
					if (hi.inCard)
						needsObject = meth.myThis();
					System.out.println("Creating handler " + fn + " in block " + closure);
				} else if (fn.toString().equals("FLEval.curry")) {
					ExternalRef f2 = ((PushExternal)closure.nestedCommands().get(1)).fn;
					if (f2 instanceof ObjectReference || f2 instanceof CardFunction) {
						needsObject = meth.myThis();
						fromHandler |= f2.fromHandler();
					}
				}
			}
			if (needsObject != null && fromHandler)
				needsObject = meth.getField("_card");
			Expr clz = al.remove(0);
			String t = clz.getType();
			if (!t.equals("java.lang.Class") && (needsObject != null || !t.equals(J.OBJECT))) {
				return meth.aNull();
	//			throw new UtilException("Type of " + clz + " is not a Class but " + t);
	//			clz = meth.castTo(clz, "java.lang.Class");
			}
			for (int i=0;i<al.size();i++) {
				al.set(i, meth.box(al.get(i)));
			}
			if (needsObject != null)
				return meth.makeNew(J.FLCLOSURE, meth.as(needsObject, J.OBJECT), clz, meth.arrayOf(J.OBJECT, al));
			else
				return meth.makeNew(J.FLCLOSURE, clz, meth.arrayOf(J.OBJECT, al));
		} else if (c0 instanceof PushVar) {
			return vh.get(((PushVar)c0).var.var);
		} else
			throw new UtilException("Can't handle " + c0);
		}
	}

	private IExpr handleField(HSIEBlock closure) {
		List<Expr> al = new ArrayList<>();
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(1), 1)));
		al.add(meth.box(appendValue((PushReturn) closure.nestedCommands().get(2), 2)));
		return meth.makeNew(J.FLCLOSURE, meth.classConst(J.FLFIELD), meth.arrayOf(J.OBJECT, al));
	}

	Expr appendValue(PushReturn c, int pos) {
		return (Expr) c.visit(new DroidAppendPush(form, meth, vh, pos));
	}

	// TODO: I think this wants to go away and we just want to autobox stuff
	Expr upcast(Expr expr) {
//		return meth.box(expr);
		if (expr.getType().equals("int"))
			return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", expr);
		return expr;
	}

}
