package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateList extends TemplateFormat {
	public final InputPosition listLoc;
	public final Object listVar;
	public final Object iterVar;
	public final String customTag;
	public final String customTagVar;
	public final boolean supportDragOrdering;
	public TemplateLine template;

	public TemplateList(InputPosition listLoc, Object listVar, Object iterVar, String customTag, String customTagVar, List<Object> formats, boolean supportDragOrdering) {
		super(formats);
		this.listLoc = listLoc;
		this.listVar = listVar;
		this.iterVar = iterVar;
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.supportDragOrdering = supportDragOrdering;
	}

}
