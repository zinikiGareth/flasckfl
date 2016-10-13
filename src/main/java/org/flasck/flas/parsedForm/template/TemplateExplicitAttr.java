package org.flasck.flas.parsedForm.template;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class TemplateExplicitAttr implements Serializable {
	public final InputPosition location;
	public final String attr;
	public final int type;
	public final Object value;

	public TemplateExplicitAttr(InputPosition loc, String attr, int type, Object value) {
		this.location = loc;
		this.attr = attr;
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return "TEA[" + attr + ":" + type + "," + value + "]";
	}
}
