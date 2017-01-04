package org.flasck.flas.template;

import java.util.List;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.zinutils.bytecode.Expr;

public interface AreaGenerator {
	void done();

	void copyVar(AreaName parentArea, AreaName definedIn, String s);
	
	void assignToVar(String varName);

	void addAssign(String call, String passVar);

	void interested(String var, String call);

	void onFieldAssign(Object expr, String field, String call);

	void onAssign(CardMember valExpr, String call);

	void newListChild(AreaName child);

	void contentExpr(String tfn, boolean rawHTML);

	EventHandlerGenerator needAddHandlers();

	void createNested(String v, AreaName nestedArea);

	void yoyoExpr(String tfn);

	void setText(String text);

	void setVarFormats(String tfn);

	void setSimpleClass(String css);

	void handleTEA(RWTemplateExplicitAttr tea, int an);

	void dropZone(List<String> droppables);

	void supportDragging();

	void makeItemDraggable();

	void assignToList(String listFn);

	void makeEditable(RWContentExpr ce, String field);

	CaseChooser chooseCase(String sn);
}
