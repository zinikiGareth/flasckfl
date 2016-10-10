package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.TemplateLine;

@SuppressWarnings("serial")
public class RWTemplateCases implements TemplateLine, Serializable {
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

}
