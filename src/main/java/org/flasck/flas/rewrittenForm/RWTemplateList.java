package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class RWTemplateList extends RWTemplateFormat {
	public final InputPosition listLoc;
	public final Object listVar;
	public final InputPosition iterLoc;
	public final Object iterVar;
	public final InputPosition customTagLoc;
	public final String customTag;
	public final InputPosition customTagVarLoc;
	public final String customTagVar;
	public final boolean supportDragOrdering;
	public RWTemplateLine template;
	public final String listFn;

	public RWTemplateList(InputPosition kw, InputPosition listLoc, Object listVar, InputPosition iterLoc, Object iterVar, InputPosition ctLoc, String customTag, InputPosition ctvLoc, String customTagVar, List<Object> formats, boolean supportDragOrdering, AreaName areaName, String listFn, String dynamicFn) {
		super(kw, areaName, formats, dynamicFn);
		this.listLoc = listLoc;
		this.listVar = listVar;
		this.iterLoc = iterLoc;
		this.iterVar = iterVar;
		this.customTagLoc = ctLoc;
		this.customTag = customTag;
		this.customTagVarLoc = ctvLoc;
		this.customTagVar = customTagVar;
		this.supportDragOrdering = supportDragOrdering;
		this.listFn = listFn;
	}
}
