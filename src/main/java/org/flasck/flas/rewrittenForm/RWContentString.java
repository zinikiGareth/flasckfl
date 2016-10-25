package org.flasck.flas.rewrittenForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class RWContentString extends RWTemplateFormatEvents {
	public final String text;

	public RWContentString(InputPosition kw, String text, List<Object> formats) {
		super(kw, formats);
		this.text = text;
	}
}
