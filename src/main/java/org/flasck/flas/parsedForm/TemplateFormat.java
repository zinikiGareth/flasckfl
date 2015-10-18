package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("serial")
public abstract class TemplateFormat implements TemplateLine, Serializable {
	public final List<Object> formats;
	
	public TemplateFormat(List<Object> formats) {
		this.formats = formats;
	}
}
