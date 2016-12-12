package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.AreaName;

public class RWTemplateCases implements RWTemplateLine, Locatable {
	public final List<RWTemplateOr> cases = new ArrayList<RWTemplateOr>();
	public final InputPosition loc;
	private final AreaName areaName;
	public final Object switchOn;

	public RWTemplateCases(InputPosition loc, AreaName areaName, Object switchOn) {
		this.loc = loc;
		this.areaName = areaName;
		this.switchOn = switchOn;
	}

	public void addCase(RWTemplateOr tor) {
		cases.add(tor);
	}

	@Override
	public InputPosition location() {
		return loc;
	}

	@Override
	public AreaName areaName() {
		return areaName;
	}

}
