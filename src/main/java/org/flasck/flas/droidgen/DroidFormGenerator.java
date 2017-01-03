package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.rewrittenForm.CardFunction;
import org.flasck.flas.rewrittenForm.ExternalRef;
import org.flasck.flas.rewrittenForm.ObjectReference;
import org.flasck.flas.rewrittenForm.PackageVar;
import org.flasck.flas.rewrittenForm.RWHandlerImplements;
import org.flasck.flas.rewrittenForm.ScopedVar;
import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEForm.CodeType;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushExternal;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.slf4j.Logger;
import org.zinutils.bytecode.BlockExpr;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.ByteCodeStorage;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.GenericAnnotator;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.IFieldInfo;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidFormGenerator {
	private final ByteCodeStorage bce;
	private final HSIEForm form;

	public DroidFormGenerator(ByteCodeStorage bce, HSIEForm f) {
		this.bce = bce;
		this.form = f;
	}

	public void generate() {
		// TODO: this needs a lot of decrypting with funcNames
		String fnName = form.funcName.jsName();
		int idx = fnName.lastIndexOf(".");
		String inClz;
		String fn = form.funcName.name;
		boolean needTrampolineClass;
		boolean wantThis = false;
		if (form.mytype == CodeType.HANDLER || form.mytype == CodeType.CONTRACT || form.mytype == CodeType.SERVICE) {
			int idx2 = fnName.lastIndexOf(".", idx-1);
			String clz = fnName.substring(0, idx2);
			String sub = fnName.substring(idx2+1, idx);
			inClz = clz +"$"+sub;
			needTrampolineClass = false;
		} else if (form.mytype == CodeType.AREA) {
			int idx2 = fnName.lastIndexOf(".", idx-1);
			int idx3 = fnName.lastIndexOf(".", idx2-1);
			String clz = fnName.substring(0, idx3+1) + fnName.substring(idx3+2, idx2);
			String sub = fnName.substring(idx2+1, idx);
			inClz = clz +"$"+sub;
			needTrampolineClass = false;
		} else if (form.mytype == CodeType.CARD || form.mytype == CodeType.EVENTHANDLER) {
			inClz = fnName.substring(0, idx);
			if (form.mytype == CodeType.CARD) {
				needTrampolineClass = true;
				wantThis = true;
			} else
				needTrampolineClass = false;  // or maybe true; I don't think we've worked with EVENTHANDLERs enough to know; I just know CARD functions need a trampoline
		} else if (form.mytype == CodeType.FUNCTION || form.mytype == CodeType.STANDALONE) {
			String pkg = fnName.substring(0, idx);
			inClz = pkg +".PACKAGEFUNCTIONS";
			if (!bce.hasClass(inClz)) {
				ByteCodeSink bcc = bce.newClass(inClz);
				bcc.superclass("java.lang.Object");
			}
			needTrampolineClass = true;
		} else
			throw new UtilException("Can't handle " + fnName + " of code type " + form.mytype);
		
		// This here is a hack because we have random underscores in some classes and not others
		// I actually think what we currently do is inconsistent (compare Simple.prototype.f to Simple.inits_hello, to the way we treat D3 functions)
		// i.e. I don't think it will work on JS even
		if (form.mytype == CodeType.CARD) {
			int idx2 = inClz.lastIndexOf(".");
			if (inClz.charAt(idx2+1) == '_')
				inClz = inClz.substring(0, idx2+1) + inClz.substring(idx2+2);
		}
		ByteCodeSink bcc = bce.get(inClz);
		GenericAnnotator gen = GenericAnnotator.newMethod(bcc, needTrampolineClass && !wantThis, fn);
		gen.returns("java.lang.Object");
		List<PendingVar> tmp = new ArrayList<PendingVar>();
		if (form.mytype == CodeType.HANDLER) // and others?
			gen.argument("org.flasck.android.post.DeliveryAddress", "_fromDA");
		int j = 0;
		for (@SuppressWarnings("unused") ScopedVar s : form.scoped)
			tmp.add(gen.argument("java.lang.Object", "_s"+(j++)));
		for (int i=0;i<form.nformal;i++)
			tmp.add(gen.argument("java.lang.Object", "_"+i));
		MethodDefiner meth = gen.done();
		j = 0;
		Map<String, Var> svars = new HashMap<String, Var>();
		Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars = new HashMap<org.flasck.flas.vcode.hsieForm.Var, Var>();
		for (ScopedVar s : form.scoped) {
			svars.put(s.uniqueName(), tmp.get(j).getVar());
			j++;
		}
		for (int i=0;i<form.nformal;i++)
			vars.put(form.vars.get(i), tmp.get(i+j).getVar());
		Expr blk = generateBlock(meth, svars, vars, form, null);
		if (blk != null)
			blk.flush();
//		meth.returnObject(meth.myThis()).flush();
		
		// for package-level methods (i.e. regular floating functions in a functional language), generate a nested class
		if (needTrampolineClass) {
			ByteCodeSink inner = bce.newClass(inClz + "$" + fn);
			inner.superclass("java.lang.Object");
			if (wantThis) {
				IFieldInfo fi = inner.defineField(true, Access.PRIVATE, bcc.getCreatedName(), "_card");
				GenericAnnotator ctor = GenericAnnotator.newConstructor(inner, false);
				PendingVar arg = ctor.argument(bcc.getCreatedName(), "card");
				MethodDefiner c = ctor.done();
				c.callSuper("void", "java.lang.Object", "<init>").flush();
				c.assign(fi.asExpr(c), arg.getVar()).flush();
				c.returnVoid().flush();
			}
			GenericAnnotator g2 = GenericAnnotator.newMethod(inner, !wantThis, "eval");
			g2.returns("java.lang.Object");
			PendingVar args = g2.argument("[java.lang.Object", "args");
			MethodDefiner m2 = g2.done();
			Expr[] fnArgs = new Expr[tmp.size()];
			for (int i=0;i<tmp.size();i++) {
				fnArgs[i] = m2.arrayElt(args.getVar(), m2.intConst(i));
			}
			IExpr doCall;
			if (wantThis)
				doCall = m2.callVirtual("java.lang.Object", m2.getField("_card"), fn, fnArgs);
			else
				doCall = m2.callStatic(inClz, "java.lang.Object", fn, fnArgs);
			
			m2.returnObject(doCall).flush();
		}
	}

	private Expr generateBlock(NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, HSIEBlock blk, Var assignReturnTo) {
		List<Expr> stmts = new ArrayList<Expr>();
		for (HSIEBlock h : blk.nestedCommands()) {
			if (h instanceof Head) {
				Head hh = (Head) h;
				Var hv = vars.get(hh.v);
				stmts.add(meth.assign(hv, meth.callStatic("org.flasck.android.FLEval", "java.lang.Object", "head", hv)));
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, "org.flasck.android.FLError"), meth.returnObject(hv), null));
			} else if (h instanceof Switch) {
				Switch s = (Switch)h;
				Var hv = vars.get(s.var);
				String ctor = s.ctor;
				if (ctor.indexOf(".") == -1)
					ctor = "org.flasck.android.builtin." + ctor;
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, ctor), generateBlock(meth, svars, vars, s, assignReturnTo), null));
			} else if (h instanceof IFCmd) {
				IFCmd c = (IFCmd)h;
				Var hv = vars.get(c.var.var);
				if (hv == null) {
					hv = meth.avar("java.lang.Object", c.var.var.toString());
					vars.put(c.var.var, hv);
					Expr cl = (Expr) closure(form, meth, svars, vars, form.mytype, form.getClosure(c.var.var));
					stmts.add(meth.assign(hv, cl));
				}

				Expr testVal;
				Expr ifblk = generateBlock(meth, svars, vars, c, assignReturnTo);
				if (c.value != null) {
					testVal = upcast(meth, exprValue(meth, c.value));
					stmts.add(meth.ifEquals(hv, testVal, ifblk, null));
				} else {
					stmts.add(meth.ifBoolean(isTruthy(meth, hv), ifblk, null));
				}
			} else if (h instanceof BindCmd) {
				BindCmd bc = (BindCmd) h;
				vars.put(bc.bind, meth.avar(JavaType.object_, bc.from + "." + bc.field));
			} else if (h instanceof PushReturn) {
				PushReturn r = (PushReturn) h;
				if (r instanceof PushVar) {
					PushVar pv = (PushVar) r;
					Var hv = vars.get(pv.var.var);
					if (pv.var.var.idx < form.nformal) {
						if (assignReturnTo != null) {
							ensureString(stmts, meth, hv);
							stmts.add(meth.assign(assignReturnTo, hv));
						} else
							stmts.add(meth.returnObject(hv));
					} else {
						if (pv.deps != null) {
							for (VarInSource cov : pv.deps) {
								Var v = vars.get(cov.var);
								if (v == null) {
									v = meth.avar("java.lang.Object", cov.var.toString());
									vars.put(cov.var, v);
								}
								Expr cl = (Expr) closure(form, meth, svars, vars, form.mytype, form.getClosure(cov.var));
								stmts.add(meth.assign(v, cl));
							}
						}
						Expr cl = (Expr) closure(form, meth, svars, vars, form.mytype, form.getClosure(pv.var.var));
						if (assignReturnTo != null) {
							ensureString(stmts, meth, hv);
							stmts.add(meth.assign(assignReturnTo, cl));
						} else
							stmts.add(meth.returnObject(cl));
					}
				} else {
					Expr expr = appendValue(form, meth, svars, vars, form.mytype, r, 0);
					stmts.add(meth.returnObject(expr));
				}
			} else if (h instanceof ErrorCmd) {
				stmts.add(meth.returnObject(meth.makeNew("org.flasck.android.FLError", meth.stringConst(meth.getName() + ": case not handled"))));
			} else {
				System.out.println("Cannot generate block:");
				h.dumpOne((Logger)null, 0);
			}
		}
		if (stmts.isEmpty())
			return null;
		else if (stmts.size() == 1)
			return stmts.get(0);
		else
			return new BlockExpr(meth, stmts);
	}

	private IExpr closure(HSIEForm form, NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, CodeType fntype, HSIEBlock closure) {
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
					al.add(upcast(meth, appendValue(form, meth, svars, vars, fntype, c, pos)));
				pos++;
			}
			Expr clz = al.remove(0);
			String t = clz.getType();
			if (!t.equals("java.lang.Class") && (needsObject != null || !t.equals("java.lang.Object"))) {
				return meth.aNull();
	//			throw new UtilException("Type of " + clz + " is not a Class but " + t);
	//			clz = meth.castTo(clz, "java.lang.Class");
			}
			if (needsObject != null)
				return meth.makeNew("org.flasck.jvm.FLClosure", meth.as(needsObject, "java.lang.Object"), clz, meth.arrayOf("java.lang.Object", al));
			else
				return meth.makeNew("org.flasck.jvm.FLClosure", clz, meth.arrayOf("java.lang.Object", al));
		} else if (c0 instanceof PushVar) {
			return vars.get(((PushVar)c0).var.var);
		} else
			throw new UtilException("Can't handle " + c0);
	}

	private Expr upcast(NewMethodDefiner meth, Expr expr) {
		if (expr.getType().equals("int"))
			return meth.callStatic("java.lang.Integer", "java.lang.Integer", "valueOf", expr);
		return expr;
	}

	private Expr exprValue(NewMethodDefiner meth, Object value) {
		if (value instanceof Integer)
			return meth.intConst((Integer)value);
		else if (value instanceof Boolean)
			return meth.intConst(((Boolean)value)?1:0);
		else
			throw new UtilException("Cannot handle " + value.getClass());
	}

	private static Expr appendValue(HSIEForm form, NewMethodDefiner meth, Map<String, Var> svars, Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars, CodeType fntype, PushReturn c, int pos) {
		return (Expr) c.visit(new DroidAppendPush(form, meth, svars, vars, fntype, pos));
	}

	protected Var generateFunctionFromForm(NewMethodDefiner meth) {
		Map<org.flasck.flas.vcode.hsieForm.Var, Var> vars = new HashMap<org.flasck.flas.vcode.hsieForm.Var, Var>();
		Map<String, Var> svars = new HashMap<String, Var>();
		Var myvar = meth.avar("java.lang.Object", "tmp");
		generateBlock(meth, svars, vars, form, myvar).flush();
		return myvar;
	}

	private Expr isTruthy(NewMethodDefiner meth, Var hv) {
		return meth.callStatic("FLEval", "boolean", "isTruthy", hv);
	}

	private void ensureString(List<Expr> stmts, NewMethodDefiner meth, Var blk) {
		if (blk == null)
			return;
		else if (blk.getType().equals("java.lang.String")) {
			// nothing to do ...
		} else if (blk.getType().equals("java.lang.Integer") || blk.getType().equals("int")) {
			stmts.add(meth.callStatic("java.lang.Integer", "java.lang.String", "toString", blk));
		} else
			throw new UtilException("Cannot handle " + blk.getType());
	}

}
