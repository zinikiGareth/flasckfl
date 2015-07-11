package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public abstract class TemplateFormat implements TemplateLine {
	public final List<Object> formats;
	public final List<Object> nested = new ArrayList<Object>();
	
	public TemplateFormat(List<Object> formats) {
		this.formats = formats;
	}
}
