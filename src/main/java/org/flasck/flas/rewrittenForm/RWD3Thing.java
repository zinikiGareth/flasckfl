package org.flasck.flas.rewrittenForm;

import org.flasck.flas.commonBase.template.TemplateLine;

public class RWD3Thing implements TemplateLine {
	public final Object data;

	public RWD3Thing(Object expr) {
		this.data = expr;
	}

	@Override
	public String toString() {
		return "D3Thing[" + data + "]";
	}
}
