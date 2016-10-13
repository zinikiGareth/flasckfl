package org.flasck.flas.commonBase.template;

import java.io.Serializable;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class TemplateList extends TemplateFormat implements Serializable {
	public final InputPosition listLoc;
	public final Object listVar;
	public final InputPosition iterLoc;
	public final Object iterVar;
	public final String customTag;
	public final String customTagVar;
	public final boolean supportDragOrdering;
	public TemplateLine template;

	public TemplateList(InputPosition listLoc, Object listVar, InputPosition iterLoc, Object iterVar, String customTag, String customTagVar, List<Object> formats, boolean supportDragOrdering) {
		super(formats);
		this.listLoc = listLoc;
		this.listVar = listVar;
		this.iterLoc = iterLoc;
		this.iterVar = iterVar;
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.supportDragOrdering = supportDragOrdering;
	}

}
