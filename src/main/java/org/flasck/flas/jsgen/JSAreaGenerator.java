package org.flasck.flas.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.template.TemplateListVar;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsform.JSTarget;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.flasck.flas.template.AreaGenerator;
import org.flasck.flas.template.CaseChooser;
import org.flasck.flas.template.EventHandlerGenerator;
import org.zinutils.exceptions.NotImplementedException;

public class JSAreaGenerator implements AreaGenerator {
	private JSTarget target;
	private final JSForm fn;
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
	public void assignToList(FunctionName listFn) {
		JSForm atv = JSForm.flex(areaName.jsName() + ".prototype._assignToVar = function()").needBlock();
		String tfn = listFn.name;
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
	public void addAssign(String call, String passVar) {
		fn.add(JSForm.flex(call + ".call(this" + (passVar != null ? ", " + passVar : "") + ")"));
	}

	@Override
	public void interested(String var, String call) {
		fn.add(JSForm.flex("this._src_" + var + "._interested(this, " + call + ")"));
	}

	@Override
	public void onFieldAssign(Object expr, String field, String call) {
		String jsexpr;
		if (expr instanceof TemplateListVar) {
			String name = ((TemplateListVar)expr).simpleName;
			jsexpr = "this._src_" + name + "." + name;
		} else if (expr instanceof CardMember) {
			jsexpr = "this._card." + ((CardMember)expr).var;
		} else
			throw new NotImplementedException();
		
		fn.add(JSForm.flex("this._onAssign(" + jsexpr +", '" + field + "', " + call + ")"));
	}

	@Override
	public void onAssign(CardMember cm, String call) {
		fn.add(JSForm.flex("this._onAssign(this._card, '" + cm.var + "', " + call + ")"));
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
	public EventHandlerGenerator needAddHandlers() {
		JSForm ahf = JSForm.flex(areaName.jsName() +".prototype._add_handlers = function()").needBlock();
		target.add(ahf);
		return new JSEventHandlerGenerator(ahf);
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
		String scf = areaName.jsName() + ".prototype._setVariableFormats";
		JSForm scvs = JSForm.flex(scf + " = function()").needBlock();
		scvs.add(JSForm.flex("this._mydiv.setAttribute('class', join(FLEval.full(this."+tfn+"()), ' '))"));
		target.add(scvs);
	}

	@Override
	public void setSimpleClass(String css) {
		fn.add(JSForm.flex("this._mydiv.className = '" + css + "'"));
	}

	@Override
	public void makeEditable(RWContentExpr ce, String field) {
		fn.add(JSForm.flex("this._editable(" + areaName.jsName() + "._rules)"));
		JSForm rules = JSForm.flex(areaName.jsName() + "._rules =").needBlock();
		JSForm save = JSForm.flex("save: function(wrapper, text)").needBlock();
		if (ce.editFn != null) {
			save.add(JSForm.flex("var containingObject = " + ce.editFn.jsName() + "()"));
		} else
			save.add(JSForm.flex("var containingObject = this._card"));
		// TODO: we may need to convert the text field to a more complex object type (e.g. integer) as specified in the rules we are given
		save.add(JSForm.flex("containingObject." + field + " = text"));
		// TODO: we need to consider which of the four types of change was just made (based on something put on atn)
		// 1. Transient local state (do nothing more)
		// 2. Persistent local state (save state object)
		// 3. Main object field or 4. Loaded object field (save data object using the appropriate contract)
		save.add(JSForm.flex("wrapper.saveObject(containingObject)"));
//		save.add(JSForm.flex("console.log('saved to:', containingObject)"));
		rules.add(save);
		// if we add another block, need "save.comma();"
		target.add(rules);
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
