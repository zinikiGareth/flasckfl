package org.flasck.flas.parsedForm.template;

import java.util.List;

public abstract class TemplateFormat implements TemplateLine {
	public final List<Object> formats;
	
	public TemplateFormat(List<Object> formats) {
		this.formats = formats;
	}
}
