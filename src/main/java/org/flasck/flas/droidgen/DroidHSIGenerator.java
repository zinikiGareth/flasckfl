package org.flasck.flas.droidgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEBlock;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.slf4j.Logger;
import org.zinutils.bytecode.BlockExpr;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidHSIGenerator {
	private final DroidClosureGenerator closGen;
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final VarHolder vh;
	
	public DroidHSIGenerator(DroidClosureGenerator closGen, HSIEForm form, NewMethodDefiner meth, VarHolder vh) {
		this.closGen = closGen;
		this.form = form;
		this.meth = meth;
		this.vh = vh;
	}

	public Expr generateBlock(HSIEBlock blk, Var assignReturnTo) {
		List<Expr> stmts = new ArrayList<Expr>();
		for (HSIEBlock h : blk.nestedCommands()) {
			if (h instanceof Head) {
				Head hh = (Head) h;
				Var hv = vh.get(hh.v);
				stmts.add(meth.assign(hv, meth.callStatic(J.FLEVAL, "java.lang.Object", "head", hv)));
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, J.FLERROR), meth.returnObject(hv), null));
			} else if (h instanceof Switch) {
				Switch s = (Switch)h;
				Var hv = vh.get(s.var);
				String ctor = s.ctor;
				if (ctor.indexOf(".") == -1)
					ctor = J.BUILTINPKG +"." + ctor;
				stmts.add(meth.ifBoolean(meth.instanceOf(hv, ctor), generateBlock(s, assignReturnTo), null));
			} else if (h instanceof IFCmd) {
				IFCmd c = (IFCmd)h;
				Var hv;
				if (vh.has(c.var.var))
					hv = vh.get(c.var.var);
				else {
					hv = meth.avar("java.lang.Object", c.var.var.toString());
					vh.put(c.var.var, hv);
					Expr cl = (Expr) closGen.closure(form.mytype, form.getClosure(c.var.var));
					stmts.add(meth.assign(hv, cl));
				}

				Expr testVal;
				Expr ifblk = generateBlock(c, assignReturnTo);
				if (c.value != null) {
					testVal = closGen.upcast(exprValue(meth, c.value));
					stmts.add(meth.ifEquals(hv, testVal, ifblk, null));
				} else {
					stmts.add(meth.ifBoolean(isTruthy(meth, hv), ifblk, null));
				}
			} else if (h instanceof BindCmd) {
				BindCmd bc = (BindCmd) h;
				vh.put(bc.bind, meth.avar(JavaType.object_, bc.from + "." + bc.field));
			} else if (h instanceof PushReturn) {
				PushReturn r = (PushReturn) h;
				if (r instanceof PushVar) {
					PushVar pv = (PushVar) r;
					if (pv.var.var.idx < form.nformal) {
						Var hv = vh.get(pv.var.var);
						if (assignReturnTo != null) {
							ensureString(stmts, meth, hv);
							stmts.add(meth.assign(assignReturnTo, hv));
						} else
							stmts.add(meth.returnObject(hv));
					} else {
						if (pv.deps != null) {
							for (VarInSource cov : pv.deps) {
								Var v;
								if (vh.has(cov.var))
									v = vh.get(cov.var);
								else {
									v = meth.avar("java.lang.Object", cov.var.toString());
									vh.put(cov.var, v);
								}
								Expr cl = (Expr) closGen.closure(form.mytype, form.getClosure(cov.var));
								stmts.add(meth.assign(v, cl));
							}
						}
						Expr cl = (Expr) closGen.closure(form.mytype, form.getClosure(pv.var.var));
						if (assignReturnTo != null) {
							ensureString(stmts, meth, vh.get(pv.var.var));
							stmts.add(meth.assign(assignReturnTo, cl));
						} else
							stmts.add(meth.returnObject(cl));
					}
				} else {
					Expr expr = closGen.appendValue(form.mytype, r, 0);
					stmts.add(meth.returnObject(expr));
				}
			} else if (h instanceof ErrorCmd) {
				stmts.add(meth.returnObject(meth.makeNew(J.FLERROR, meth.stringConst(meth.getName() + ": case not handled"))));
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

	private Expr exprValue(NewMethodDefiner meth, Object value) {
		if (value instanceof Integer)
			return meth.intConst((Integer)value);
		else if (value instanceof Boolean)
			return meth.intConst(((Boolean)value)?1:0);
		else
			throw new UtilException("Cannot handle " + value.getClass());
	}
}
