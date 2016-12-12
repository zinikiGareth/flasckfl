package org.flasck.flas.rewrittenForm;

import org.flasck.flas.commonBase.names.AreaName;

public class RWD3Thing implements RWTemplateLine {
	private final AreaName areaName;
	public final Object data;

	public RWD3Thing(AreaName areaName, Object expr) {
		this.areaName = areaName;
		this.data = expr;
	}
	
	@Override
	public AreaName areaName() {
		return areaName;
	}

	@Override
	public String toString() {
		return "D3Thing[" + data + "]";
	}
}
