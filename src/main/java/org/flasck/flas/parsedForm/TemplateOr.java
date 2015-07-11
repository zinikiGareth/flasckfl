package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class TemplateOr implements TemplateLine {
	public final Object cond;
	public final List<Object> template = new ArrayList<Object>();

	public TemplateOr(Object expr) {
		this.cond = expr;
	}

}
