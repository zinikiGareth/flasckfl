package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.TemplateLine;

@SuppressWarnings("serial")
public class TemplateCases implements TemplateLine, Serializable {
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
