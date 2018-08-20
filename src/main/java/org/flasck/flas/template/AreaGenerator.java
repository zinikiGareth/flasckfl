package org.flasck.flas.template;

import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWContentExpr;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.flasck.flas.template.TemplateTraversor.DefinedVar;

public interface AreaGenerator {
	void done();

	void copyVar(AreaName parentArea, AreaName definedIn, String s);
	
	void assignToVar(String varName);

	void addAssign(FunctionName call, String passVar);

	void interested(String var, FunctionName call);

	void onFieldAssign(Map<ApplyExpr, FunctionName> changers, Object expr, String field, FunctionName call);

	void onAssign(CardMember valExpr, FunctionName call);

	void newListChild(AreaName child);

	void contentExpr(FunctionName tfn, boolean rawHTML);

	EventHandlerGenerator needAddHandlers();

	void createNested(String v, AreaName nestedArea, String holeName, List<DefinedVar> varsToCopy);

	void yoyoExpr(String tfn);

	void setText(String text);

	void setVarFormats(FunctionName tfn);

	void setSimpleClass(String css);

	void handleTEA(RWTemplateExplicitAttr tea, int an);

	void dropZone(List<String> droppables);

	void supportDragging();

	void makeItemDraggable();

	void assignToList(FunctionName listFn);

	void makeEditable(RWContentExpr ce, String field);

	CaseChooser chooseCase(FunctionName sn);
}
