package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.List;

import org.flasck.flas.parsedForm.TemplateLine;

@SuppressWarnings("serial")
public abstract class TemplateFormat implements TemplateLine, Serializable {
	public final List<Object> formats;
	
	public TemplateFormat(List<Object> formats) {
		this.formats = formats;
	}
}
