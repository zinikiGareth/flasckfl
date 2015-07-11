package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public abstract class TemplateFormat implements TemplateLine {
	public final List<Object> formats;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();
	
	public TemplateFormat(List<Object> formats) {
		this.formats = formats;
	}
}
