package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.template.TemplateLine;

public class TemplateDiv extends TemplateFormatEvents {
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
