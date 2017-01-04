package org.flasck.flas.jsgen;

import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.template.AreaGenerator;
import org.zinutils.bytecode.Expr;

public class JSAreaGenerator implements AreaGenerator {

	// TODO: make this private and only used locally
	public final JSForm fn;

	public JSAreaGenerator(JSForm fn) {
		this.fn = fn;
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyVar(String parentClass, String definedInType, String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void assignToVar(String varName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addAssign(String call) {
		// TODO Auto-generated method stub

	}

	@Override
	public void interested(String var, String call) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAssign(Expr expr, String field, String call) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onAssign(CardMember valExpr, String call) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newListChild(String child) {
		// TODO Auto-generated method stub

	}

	@Override
	public void contentExpr(String tfn, boolean rawHTML) {
		// TODO Auto-generated method stub

	}

	@Override
	public void needAddHandlers() {
		// TODO Auto-generated method stub

	}

	@Override
	public void createNested(String v, String cn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void yoyoExpr(String tfn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setText(String text) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVarFormats(String tfn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSimpleClass(String css) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newVar(String newVar) {
		// TODO Auto-generated method stub

	}

	@Override
	public Expr sourceFor(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Expr cardField(CardMember expr) {
		// TODO Auto-generated method stub
		return null;
	}

}
