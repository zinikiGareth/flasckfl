package org.flasck.flas.template;

import java.util.List;

import org.flasck.flas.commonBase.names.AreaName;
import org.flasck.flas.rewrittenForm.CardMember;
import org.flasck.flas.rewrittenForm.RWTemplateExplicitAttr;
import org.zinutils.bytecode.Expr;

public interface AreaGenerator {
	void done();

	void copyVar(AreaName parentArea, AreaName definedIn, String s);
	
	void assignToVar(String varName);

	void addAssign(String call);

	void interested(String var, String call);

	void onAssign(Expr expr, String field, String call);

	void onAssign(CardMember valExpr, String call);

	void newListChild(String child);

	void contentExpr(String tfn, boolean rawHTML);

	void needAddHandlers();

	void createNested(String v, AreaName nestedArea);

	void yoyoExpr(String tfn);

	void setText(String text);

	void setVarFormats(String tfn);

	void setSimpleClass(String css);

	Expr sourceFor(String name);

	Expr cardField(CardMember expr);

	void handleTEA(RWTemplateExplicitAttr tea, int an);

	void dropZone(List<String> droppables);
}
