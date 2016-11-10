package org.flasck.flas.rewrittenForm;

public class RWD3Thing implements RWTemplateLine {
	private final String areaName;
	public final Object data;

	public RWD3Thing(String areaName, Object expr) {
		this.areaName = areaName;
		this.data = expr;
	}
	
	@Override
	public String areaName() {
		return areaName;
	}

	@Override
	public String toString() {
		return "D3Thing[" + data + "]";
	}
}
