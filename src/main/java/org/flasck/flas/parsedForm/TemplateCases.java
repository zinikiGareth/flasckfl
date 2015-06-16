package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TemplateCases {
	public final List<TemplateOr> cases = new ArrayList<TemplateOr>();
	public final InputPosition loc;
	public final Object switchOn;

	public TemplateCases(InputPosition loc, Object switchOn) {
		this.loc = loc;
		this.switchOn = switchOn;
	}

	public void addCase(TemplateOr tor) {
		cases.add(tor);
	}

}
