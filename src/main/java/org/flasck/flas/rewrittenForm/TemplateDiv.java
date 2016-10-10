package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.TemplateLine;

@SuppressWarnings("serial")
public class TemplateDiv extends TemplateFormatEvents implements Serializable {
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();
	public List<String> droppables;

	public TemplateDiv(String customTag, String customTagVar, List<Object> attrs, List<Object> formats) {
		super(formats);
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
	}
}
