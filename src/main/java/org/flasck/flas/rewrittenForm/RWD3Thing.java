package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.AreaName;

public class RWD3Thing implements RWTemplateLine {
	private InputPosition loc;
	private final AreaName areaName;
	public final Object data;

	public RWD3Thing(InputPosition loc, AreaName areaName, Object expr) {
		this.loc = loc;
		this.areaName = areaName;
		this.data = expr;
	}
	
	@Override
	public AreaName areaName() {
		return areaName;
	}
	
	public InputPosition location() {
		return loc;
	}

	@Override
	public String toString() {
		return "D3Thing[" + data + "]";
	}
}
