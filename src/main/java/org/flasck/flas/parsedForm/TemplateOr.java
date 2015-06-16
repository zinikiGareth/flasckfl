package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TemplateOr {
	public final Object cond;
	public final List<TemplateLine> template = new ArrayList<TemplateLine>();

	public TemplateOr(Object expr) {
		this.cond = expr;
	}

}
