package org.flasck.flas.jsgen;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.template.AreaGenerator;
import org.zinutils.bytecode.Expr;

public class JSAreaGenerator implements AreaGenerator {

	private JSTarget target;
	// TODO: make this private and only used locally
	public final JSForm fn;
	private final AreaName areaName;

	public JSAreaGenerator(JSTarget target, JSForm fn, AreaName areaName) {
		this.target = target;
		this.fn = fn;
		this.areaName = areaName;
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyVar(AreaName parentArea, AreaName definedIn, String s) {
		fn.add(JSForm.flex("this._src_" + s + " = parent._src_" + s));
	}

	@Override
	public void assignToVar(String newVar) {
		fn.add(JSForm.flex("this._src_"+newVar+ " = this"));
		JSForm nda = JSForm.flex(areaName.jsName() +".prototype._assignToVar = function(obj)").needBlock();
		nda.add(JSForm.flex("if (this. " + newVar + " == obj) return"));
		JSForm ifremove = JSForm.flex("if (this." + newVar+ ")");
		// TODO: I claim this should be this.newVar, not obj
		ifremove.add(JSForm.flex(" this._wrapper.removeOnUpdate('crorepl', this._parent._croset, obj.id, this)"));
		nda.add(ifremove);
		nda.add(JSForm.flex("this." + newVar + " = obj"));
		JSForm ifload = JSForm.flex("if (this." + newVar+ ")").needBlock();
		// TODO: I claim this should also be this.newVar, not obj for consistency, but at least they are the same here ...
		ifload.add(JSForm.flex("this._wrapper.onUpdate('crorepl', this._parent._croset, obj.id, this)"));
		nda.add(ifload);
		nda.add(JSForm.flex("this._fireInterests()"));
		target.add(nda);
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
