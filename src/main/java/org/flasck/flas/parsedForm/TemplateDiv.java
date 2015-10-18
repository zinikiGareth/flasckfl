package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class TemplateDiv extends TemplateFormatEvents implements Serializable {
	public final String customTag;
	public final String customTagVar;
	public final List<Object> attrs;
	public final List<TemplateLine> nested = new ArrayList<TemplateLine>();

	public TemplateDiv(String customTag, String customTagVar, List<Object> attrs, List<Object> formats) {
		super(formats);
		this.customTag = customTag;
		this.customTagVar = customTagVar;
		this.attrs = attrs;
	}
}
