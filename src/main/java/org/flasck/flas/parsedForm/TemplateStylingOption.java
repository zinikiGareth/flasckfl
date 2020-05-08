package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class TemplateStylingOption {
	public final Expr cond;
	public final List<Expr> styles;
	public final TemplateField styleField;

	public TemplateStylingOption(TemplateField field, Expr cond, List<Expr> styles) {
		this.styleField = field;
		this.cond = cond;
		this.styles = styles;
	}

	public String constant() {
		StringBuilder sb = new StringBuilder();
		for (Expr s : styles) {
			if (!(s instanceof StringLiteral))
				continue;
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(((StringLiteral)s).text);
		}
		if (sb.length() == 0)
			return null;
		return sb.toString();
	}
}
