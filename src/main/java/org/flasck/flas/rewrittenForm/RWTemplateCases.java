package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.template.TemplateLine;

public class RWTemplateCases implements TemplateLine, Locatable {
	public final List<RWTemplateOr> cases = new ArrayList<RWTemplateOr>();
	public final InputPosition loc;
	public final Object switchOn;

	public RWTemplateCases(InputPosition loc, Object switchOn) {
		this.loc = loc;
		this.switchOn = switchOn;
	}

	public void addCase(RWTemplateOr tor) {
		cases.add(tor);
	}

	@Override
	public InputPosition location() {
		return loc;
	}

}
