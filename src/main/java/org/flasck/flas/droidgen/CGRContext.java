package org.flasck.flas.droidgen;

import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.template.AreaGenerator;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.Expr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;

public class CGRContext implements AreaGenerator {
	final ByteCodeSink bcc;
	public final NewMethodDefiner ctor;
	final Var card;
	final Var parent;
	MethodDefiner currentMethod;
	private DroidTemplateGenerator dg;

	public CGRContext(DroidTemplateGenerator dg, ByteCodeSink bcc, NewMethodDefiner ctor, Var card, Var parent) {
		this.dg = dg;
		this.bcc = bcc;
		this.ctor = ctor;
		this.card = card;
		this.parent = parent;
	}

	@Override
	public void done() {
		dg.done(this);
	}

	@Override
	public void copyVar(String parentClass, String definedInType, String s) {
		dg.copyVar(this, parentClass, definedInType, s);
	}

	@Override
	public void assignToVar(String varName) {
		dg.assignToVar(this, varName);
	}

	@Override
	public void addAssign(String call) {
		dg.addAssign(this, call);
	}

	@Override
	public void interested(String var, String call) {
		dg.interested(this, var, call);
	}

	@Override
	public void onAssign(Expr expr, String field, String call) {
		dg.onAssign(this, expr, field, call);
	}

	@Override
	public void onAssign(CardMember valExpr, String call) {
		dg.onAssign(this, valExpr, call);
	}

	@Override
	public void newListChild(String child) {
		dg.newListChild(this, child);
	}

	@Override
	public void contentExpr(String tfn, boolean rawHTML) {
		dg.contentExpr(this, tfn, rawHTML);
	}

	@Override
	public void needAddHandlers() {
		dg.needAddHandlers(this);
	}

	@Override
	public void createNested(String v, String cn) {
		dg.createNested(this, v, cn);
	}

	@Override
	public void yoyoExpr(String tfn) {
		dg.yoyoExpr(this, tfn);
	}

	@Override
	public void setText(String text) {
		dg.setText(this, text);		
	}

	@Override
	public void setVarFormats(String tfn) {
		dg.setVarFormats(this, tfn);
	}

	@Override
	public void setSimpleClass(String css) {
		dg.setSimpleClass(this, css);
	}

	@Override
	public void newVar(String newVar) {
		dg.newVar(this, newVar);
	}

	@Override
	public Expr sourceFor(String name) {
		return ctor.getField(ctor.getField(ctor.myThis(), "_src_" + name), name);
	}

	@Override
	public Expr cardField(CardMember expr) {
		return ctor.getField(ctor.getField(ctor.myThis(), "_card"), ((CardMember)expr).var);
	}
}
