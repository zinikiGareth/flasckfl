package org.flasck.flas.droidgen;

import java.util.ArrayList;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.PushVisitor;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.exceptions.UtilException;

public final class DroidPushArgument implements PushVisitor {
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final VarHolder vh;

	public DroidPushArgument(HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.form = form;
		this.meth = meth;
		this.vh = vh;
	}

	@Override
	public Object visit(PushExternal pe) {
		ExternalRef name = pe.fn;
		if (name instanceof PackageVar || name instanceof ObjectReference) {
			boolean needToCallEvalMethod = false;
			Object defn = null;
			if (name instanceof PackageVar) {
				defn = name;
				while (defn instanceof PackageVar)
					defn = ((PackageVar)defn).defn;
				if (defn instanceof RWStructDefn && ((RWStructDefn)defn).fields.isEmpty())
					needToCallEvalMethod = true;
			}
			String clz = DroidUtils.getJavaClassForDefn(meth, name, defn);
			if (!needToCallEvalMethod) { // handle the simple class case ...
				return meth.classConst(clz);
			} else {
				return meth.callStatic(clz, J.OBJECT, "eval", meth.arrayOf(J.OBJECT, new ArrayList<Expr>()));
			}
		} else if (name instanceof ScopedVar) {
			ScopedVar sv = (ScopedVar) name;
			if (sv.definedBy.equals(form.funcName)) {
				// TODO: I'm not quite sure what should happen here, or even what this case represents, but I know it should be something to do with the *actual* function definition
				return meth.stringConst(name.uniqueName());
			}
			return vh.getScoped(name.uniqueName());
		} else if (name instanceof CardFunction) {
			String jnn = DroidUtils.javaNestedName(name.uniqueName());
			return meth.classConst(jnn);
//			IExpr cardObj;
//			if (fromHandler())
//				cardObj = meth.getField("_card");
//			else
//				cardObj = meth.myThis();
//			return meth.makeNew(jnn, cardObj);
		} else if (name instanceof CardMember) {
			if (form.isCardMethod())
				return meth.myThis(); // surely this needs to deference cm.var?
			else if (form.needsCardMember()) {
				CardMember cm = (CardMember)name;
				Expr field = meth.getField(meth.getField("_card"), cm.var);
				return field;
			} else
				throw new UtilException("Can't handle card member with " + form.mytype);
		} else if (name instanceof HandlerLambda) {
			if (form.mytype == CodeType.HANDLER)
				return meth.getField(((HandlerLambda)name).var);
			else
				throw new UtilException("Can't handle handler lambda with " + form.mytype);
		} else
			throw new UtilException("Can't handle " + name + " of type " + name.getClass());
	}

	@Override
	public Object visit(PushVar pv) {
		return vh.get(pv.var.var);
	}

	@Override
	public Object visit(PushInt pi) {
		return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", meth.intConst(pi.ival));
	}

	@Override
	public Object visit(PushString ps) {
		return meth.stringConst(ps.sval.text);
	}

	@Override
	public Object visit(PushBool pb) {
		return meth.boolConst(pb.bval.value());
	}

	@Override
	public Object visit(PushTLV pt) {
		return meth.getField(meth.getField("_src_" + pt.tlv.simpleName), pt.tlv.simpleName);
	}

	@Override
	public Object visit(PushCSR pc) {
		if (pc.csr.fromHandler)
			return meth.getField("_card");
		else
			return meth.myThis();
	}

	@Override
	public Object visit(PushFunc pf) {
//				int x = c.func.name.lastIndexOf('.');
//				if (x == -1)
//					throw new UtilException("Invalid function name: " + c.func.name);
//				else
//					sb.append(c.func.name.substring(0, x+1) + "prototype" + c.func.name.substring(x));
//				throw new UtilException("What are you pushing? " + c);

		// this is clearly wrong, but we need to return a "function" object and I don't have one of those right now, I don't think 
		return meth.myThis();
	}
}