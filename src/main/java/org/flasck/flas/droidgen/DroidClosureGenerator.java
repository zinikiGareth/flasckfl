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
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
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

	public IExpr closure(CodeType fntype, HSIEBlock closure) {
		// Loop over everything in the closure pushing it onto the stack (in al)
		HSIEBlock c0 = closure.nestedCommands().get(0);
		if (c0 instanceof PushExternal) {
			ExternalRef fn = ((PushExternal)c0).fn;
			Expr needsObject = null;
			boolean fromHandler = fntype == CodeType.AREA;
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
			int pos = 0;
			boolean isField = false;
			List<Expr> al = new ArrayList<Expr>();
			for (HSIEBlock b : closure.nestedCommands()) {
				PushReturn c = (PushReturn) b;
				if (c instanceof PushExternal && pos == 0) {
					isField = "FLEval.field".equals(((PushExternal)c).fn);
				}
				if (c instanceof PushExternal && isField && pos == 2)
					System.out.println("c.fn = " + ((PushExternal)c).fn);
				else
					al.add(upcast(appendValue(fntype, c, pos)));
				pos++;
			}
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

	Expr appendValue(CodeType fntype, PushReturn c, int pos) {
		return (Expr) c.visit(new DroidAppendPush(form, meth, vh, fntype, pos));
	}

	Expr upcast(Expr expr) {
		if (expr.getType().equals("int"))
			return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", expr);
		return expr;
	}

}
