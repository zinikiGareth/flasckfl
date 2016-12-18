package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.Map;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.HandlerLambda;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWFunctionDefinition;
import org.flasck.flas.rewrittenForm.RWMethodDefinition;
import org.flasck.flas.rewrittenForm.RWStructDefn;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.types.Type;
import org.flasck.flas.types.Type.WhatAmI;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.PushCSR;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushFunc;
import org.flasck.flas.vcode.hsieForm.PushInt;
import org.flasck.flas.vcode.hsieForm.PushString;
import org.flasck.flas.vcode.hsieForm.PushTLV;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.PushVisitor;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.PushBool;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.StringUtil;

public final class DroidAppendPush implements PushVisitor {
	private HSIEForm form;
	private NewMethodDefiner meth;
	private Map<String, Var> svars;
	private Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars;
	private CodeType fntype;
	private int pos;

	public DroidAppendPush(HSIEForm form, NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, CodeType fntype, int pos) {
		this.form = form;
		this.meth = meth;
		this.svars = svars;
		this.vars = vars;
		this.fntype = fntype;
		this.pos = pos;
	}

	@Override
	public Object visit(PushExternal pe) {
		if (pe.fn instanceof PackageVar || pe.fn instanceof ObjectReference) {
			boolean needToCallEvalMethod = false;
			Object defn = null;
			if (pe.fn instanceof PackageVar) {
				defn = pe.fn;
				while (defn instanceof PackageVar)
					defn = ((PackageVar)defn).defn;
				if (pos != 0) {
					if (defn instanceof RWStructDefn && ((RWStructDefn)defn).fields.isEmpty())
						needToCallEvalMethod = true;
				} else if (pos == 0 && defn instanceof RWFunctionDefinition) {
					RWFunctionDefinition fn = (RWFunctionDefinition) defn;
					if (fn.nargs == 0)
						needToCallEvalMethod = true;
				}
			}
			int idx = pe.fn.uniqueName().lastIndexOf(".");
			String inside;
			String dot;
			String member;
			if (idx == -1) {
				inside = "org.flasck.android.builtin";
				dot = ".";
				member = pe.fn.uniqueName();
			} else {
				String first = pe.fn.uniqueName().substring(0, idx);
				if ("FLEval".equals(first)) {
					inside = "org.flasck.android.FLEval";
					member = StringUtil.capitalize(pe.fn.uniqueName().substring(idx+1));
				} else {
					inside = pe.fn.uniqueName().substring(0, idx);
					member = pe.fn.uniqueName().substring(idx+1);
				}
				dot = "$";
			}
			String clz;
			if (defn instanceof RWFunctionDefinition || defn instanceof RWMethodDefinition || (defn instanceof Type && ((Type)defn).iam == WhatAmI.FUNCTION)) {
				if (inside.equals("org.flasck.android.FLEval"))
					clz = inside + "$" + member;
				else
					clz = inside + ".PACKAGEFUNCTIONS$" + member;
			} else {
				clz = inside + dot + member;
			}
			meth.getBCC().addInnerClassReference(Access.PUBLICSTATIC, inside, member);
			if (!needToCallEvalMethod) { // handle the simple class case ...
				return meth.classConst(clz);
			} else {
				return meth.callStatic(clz, "java.lang.Object", "eval", meth.arrayOf("java.lang.Object", new ArrayList<Expr>()));
			}
		} else if (pe.fn instanceof ScopedVar) {
			ScopedVar sv = (ScopedVar) pe.fn;
			if (sv.definedBy.equals(form.funcName)) {
				// TODO: I'm not quite sure what should happen here, or even what this case represents, but I know it should be something to do with the *actual* function definition
				return meth.stringConst(pe.fn.uniqueName());
			}
			if (!svars.containsKey(pe.fn.uniqueName()))
				throw new UtilException("ScopedVar not in scope: " + pe.fn);
			return svars.get(pe.fn.uniqueName());
		} else if (pe.fn instanceof CardFunction) {
			String jnn = DroidUtils.javaNestedName(pe.fn.uniqueName());
			return meth.makeNew(jnn, meth.myThis());
		} else if (pe.fn instanceof CardMember) {
			if (fntype == CodeType.CARD || fntype == CodeType.EVENTHANDLER)
				return meth.myThis();
			else if (fntype == CodeType.HANDLER || fntype == CodeType.CONTRACT || fntype == CodeType.AREA) {
				CardMember cm = (CardMember)pe.fn;
				Expr field = meth.getField(meth.getField("_card"), cm.var);
				return field;
			} else
				throw new UtilException("Can't handle " + fntype + " for card member");
		} else if (pe.fn instanceof HandlerLambda) {
			if (fntype == CodeType.HANDLER)
				return meth.getField(((HandlerLambda)pe.fn).var);
			else
				throw new UtilException("Can't handle " + fntype + " with handler lambda");
		} else
			throw new UtilException("Can't handle " + pe.fn + " of type " + pe.fn.getClass());
	}

	@Override
	public Object visit(PushVar pv) {
		if (!vars.containsKey(pv.var.var))
			throw new UtilException("Do not have the variable " + pv.var);
		return vars.get(pv.var.var);
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