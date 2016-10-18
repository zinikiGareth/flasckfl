package org.flasck.flas.rewrittenForm;

import java.util.List;

public class RWContentString extends RWTemplateFormatEvents {
	public final String text;

	public RWContentString(String text, List<Object> formats) {
		super(formats);
		this.text = text;
	}
}
