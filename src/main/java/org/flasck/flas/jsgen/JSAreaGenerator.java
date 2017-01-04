package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.flasck.flas.template.AreaGenerator;
import org.flasck.flas.template.CaseChooser;
import org.flasck.flas.template.TemplateTraversor;
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
	public void assignToList(String listFn) {
		JSForm atv = JSForm.flex(areaName.jsName() + ".prototype._assignToVar = function()").needBlock();
		String tfn = TemplateTraversor.simpleName(listFn);
		atv.add(JSForm.flex("var lv = FLEval.full(this." + tfn + "())"));
		fn.add(JSForm.flex(areaName.jsName() + ".prototype._assignToVar.call(this)"));
		atv.add(JSForm.flex("ListArea.prototype._assignToVar.call(this, lv)"));
		target.add(atv);
	}

	public void handleTEA(RWTemplateExplicitAttr tea, int an) {
		String saf = areaName.jsName() + ".prototype._setAttr_" + an;
		JSForm sak = JSForm.flex(saf + " = function()").needBlock();
		String tfn = tea.fnName.name;
		sak.add(JSForm.flex("var attr = FLEval.full(this." + tfn + "())"));
		JSForm ifassign = JSForm.flex("if (attr && !(attr instanceof FLError))").needBlock();
		sak.add(ifassign);
		ifassign.add(JSForm.flex("this._mydiv.setAttribute('" + tea.attr +"', attr)"));
		target.add(sak);
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
	public void newListChild(AreaName item) {
		JSForm nc = JSForm.flex(areaName.jsName() +".prototype._newChild = function()").needBlock();
		nc.add(JSForm.flex("return new " + item.jsName() + "(this)"));
		target.add(nc);
	}

	@Override
	public void contentExpr(String tfn, boolean rawHTML) {
		JSForm cexpr = JSForm.flex(areaName.jsName() +".prototype._contentExpr = function()").needBlock();
		if (rawHTML)
			cexpr.add(JSForm.flex("this._insertHTML(this." + tfn +"())"));
		else
			cexpr.add(JSForm.flex("this._assignToText(this." + tfn +"())"));
		target.add(cexpr);
	}

	@Override
	public void needAddHandlers() {
		// TODO Auto-generated method stub

	}

	@Override
	public void createNested(String v, AreaName nested) {
		fn.add(JSForm.flex("var " + v + " = new " + nested.jsName() + "(this)"));
	}

	@Override
	public void yoyoExpr(String tfn) {
		JSForm cexpr = JSForm.flex(areaName.jsName() +".prototype._yoyoExpr = function()").needBlock();
		cexpr.add(JSForm.flex("this._updateToCard(this." + tfn + "())"));
		target.add(cexpr);
	}

	@Override
	public void setText(String text) {
		fn.add(JSForm.flex("this._setText('" + text + "')"));
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

	@Override
	public void makeEditable() {
		fn.add(JSForm.flex("this._editable(" + areaName.jsName() + "._rules)"));
	}

	@Override
	public void supportDragging() {
		fn.add(JSForm.flex("this._supportDragging()"));
	}

	@Override
	public void makeItemDraggable() {
		fn.add(JSForm.flex("this._makeDraggable()"));
	}

	@Override
	public void dropZone(List<String> droppables) {
		List<String> asRegexps = new ArrayList<String>();
		for (String s : droppables)
			asRegexps.add("/" + s + "/");
		fn.add(JSForm.flex("this._dropSomethingHere(" + asRegexps + ")"));
	}

	public CaseChooser chooseCase(String sn) {
		JSForm sw = JSForm.flex(sn +" = function(parent)").needBlock();
		sw.add(JSForm.flex("\"use strict\""));
		target.add(sw);
		return new JSCaseChooser(sw);
	}
	
	
}
