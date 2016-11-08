package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.template.TemplateFormat;
import org.flasck.flas.commonBase.template.TemplateLine;

public class TemplateList extends TemplateFormat {
	public final InputPosition listLoc;
	public final Object listExpr;
	public final InputPosition iterLoc;
	public final String iterVar;
	public final InputPosition customTagLoc;
	public final String customTag;
	public final InputPosition customTagVarLoc;
	public final String customTagVar;
	public final boolean supportDragOrdering;
	public TemplateLine template;

	public TemplateList(InputPosition kw, InputPosition listLoc, Object listExpr, InputPosition iterLoc, String iterVar, InputPosition ctLoc, String customTag, InputPosition ctvLoc, String customTagVar, List<Object> formats, boolean supportDragOrdering) {
		super(kw, formats);
		this.listLoc = listLoc;
		this.listExpr = listExpr;
		this.iterLoc = iterLoc;
		this.iterVar = iterVar;
		this.customTagLoc = ctLoc;
		this.customTag = customTag;
		this.customTagVarLoc = ctvLoc;
		this.customTagVar = customTagVar;
		this.supportDragOrdering = supportDragOrdering;
	}

}
