package org.flasck.flas.droidgen;

import org.flasck.flas.vcode.hsieForm.BindCmd;
import org.flasck.flas.vcode.hsieForm.ErrorCmd;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEVisitor;
import org.flasck.flas.vcode.hsieForm.Head;
import org.flasck.flas.vcode.hsieForm.IFCmd;
import org.flasck.flas.vcode.hsieForm.PushReturn;
import org.flasck.flas.vcode.hsieForm.PushVar;
import org.flasck.flas.vcode.hsieForm.Switch;
import org.flasck.flas.vcode.hsieForm.VarInSource;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.exceptions.UtilException;

public class DroidHSIProcessor implements HSIEVisitor {
	private final DroidHSIGenerator droidHSIGenerator;
	private final HSIEForm form;
	private final NewMethodDefiner meth;
	private final StmtCollector coll;
	private final VarHolder vh;
	private final Var assignReturnTo;
	private final DroidClosureGenerator closGen;

	public DroidHSIProcessor(DroidHSIGenerator droidHSIGenerator, HSIEForm form, NewMethodDefiner meth, StmtCollector coll, DroidClosureGenerator closGen, VarHolder vh, Var assignReturnTo) {
		this.droidHSIGenerator = droidHSIGenerator;
		this.form = form;
		this.meth = meth;
		this.coll = coll;
		this.closGen = closGen;
		this.vh = vh;
		this.assignReturnTo = assignReturnTo;
	}
	
	@Override
	public void visit(Head h) {
		Var hv = vh.get(h.v);
		coll.add(meth.assign(hv, meth.callStatic(J.FLEVAL, "java.lang.Object", "head", hv)));
		coll.add(meth.ifBoolean(meth.instanceOf(hv, J.FLERROR), meth.returnObject(hv), null));
	}

	@Override
	public void visit(Switch sw) {
		Var hv = vh.get(sw.var);
		String ctor = sw.ctor;
		if (ctor.indexOf(".") == -1)
			ctor = J.BUILTINPKG +"." + ctor;
		coll.add(meth.ifBoolean(meth.instanceOf(hv, ctor), droidHSIGenerator.generateHSI(sw, assignReturnTo), null));
	}

	
	@Override
	public void visit(IFCmd c) {
		Var hv;
		if (vh.has(c.var.var))
			hv = vh.get(c.var.var);
		else {
			hv = meth.avar("java.lang.Object", c.var.var.toString());
			vh.put(c.var.var, hv);
			Expr cl = (Expr) closGen.closure(form.getClosure(c.var.var));
			coll.add(meth.assign(hv, cl));
		}

		Expr testVal;
		Expr ifblk = droidHSIGenerator.generateHSI(c, assignReturnTo);
		if (c.value != null) {
			testVal = closGen.upcast(exprValue(meth, c.value));
			coll.add(meth.ifEquals(hv, testVal, ifblk, null));
		} else {
			coll.add(meth.ifBoolean(isTruthy(meth, hv), ifblk, null));
		}
	}
	
	@Override
	public void visit(BindCmd bc) {
		vh.put(bc.bind, meth.avar(JavaType.object_, bc.from + "." + bc.field));
	}

	@Override
	public void visit(PushReturn r) {
		if (r instanceof PushVar) {
			PushVar pv = (PushVar) r;
			if (pv.var.var.idx < form.nformal) {
				Var hv = vh.get(pv.var.var);
				if (assignReturnTo != null) {
					makeArgBeString(hv);
					coll.add(meth.assign(assignReturnTo, hv));
				} else
					coll.add(meth.returnObject(hv));
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
						Expr cl = (Expr) closGen.closure(form.getClosure(cov.var));
						coll.add(meth.assign(v, cl));
					}
				}
				Expr cl = (Expr) closGen.closure(form.getClosure(pv.var.var));
				if (assignReturnTo != null) {
					makeArgBeString(vh.get(pv.var.var));
					coll.add(meth.assign(assignReturnTo, cl));
				} else
					coll.add(meth.returnObject(cl));
			}
		} else {
			Expr expr = closGen.appendValue(r, true);
			coll.add(meth.returnObject(expr));
		}
	}
	
	@Override
	public void visit(ErrorCmd n) {
		coll.add(meth.returnObject(meth.makeNew(J.FLERROR, meth.stringConst(meth.getName() + ": case not handled"))));
	}

	private Expr isTruthy(NewMethodDefiner meth, Var hv) {
		return meth.callStatic(J.FLEVAL, J.BOOLEANP.getActual(), "isTruthy", hv);
	}

	private void makeArgBeString(Var v) {
		if (v == null)
			return;
		else if (v.getType().equals(J.STRING)) {
			// nothing to do ...
		} else if (v.getType().equals(J.INTEGER) || v.getType().equals(J.INTP.getActual())) {
			coll.add(meth.callStatic(J.INTEGER, J.STRING, J.STRING, v));
		} else
			throw new UtilException("Cannot handle " + v.getType());
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
