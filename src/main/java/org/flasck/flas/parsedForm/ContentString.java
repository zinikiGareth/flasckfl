package org.flasck.flas.parsedForm;

import java.util.List;

public class ContentString extends TemplateFormatEvents {
	public final String text;

	public ContentString(String text, List<Object> formats) {
		super(formats);
		this.text = text;
	}
}
