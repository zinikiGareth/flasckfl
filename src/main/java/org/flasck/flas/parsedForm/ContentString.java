package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class ContentString extends TemplateFormatEvents {
	public final String text;

	public ContentString(InputPosition pos, String text, List<Object> formats) {
		super(pos, formats);
		this.text = text;
	}
}
