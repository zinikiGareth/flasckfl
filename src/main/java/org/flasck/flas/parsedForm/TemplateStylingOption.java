package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class TemplateStylingOption {
	public final Expr cond;
	public final List<StringLiteral> styles;
	public final TemplateField styleField;

	public TemplateStylingOption(TemplateField field, Expr cond, List<StringLiteral> styles) {
		this.styleField = field;
		this.cond = cond;
		this.styles = styles;
	}

	public String styleString() {
		StringBuilder sb = new StringBuilder();
		for (StringLiteral s : styles) {
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(s.text);
		}
		return sb.toString();
	}
}
