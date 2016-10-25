package org.flasck.flas.commonBase.template;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public abstract class TemplateFormat implements TemplateLine {
	public final InputPosition kw;
	public final List<Object> formats;
	
	public TemplateFormat(InputPosition kw, List<Object> formats) {
		this.kw = kw;
		this.formats = formats;
	}
}
