package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class TemplateStylingOption extends TemplateCustomization {
	public final Expr cond;
	public final List<Expr> styles;
	public final List<Expr> orelse;

	public TemplateStylingOption(Expr cond, List<Expr> styles, List<Expr> orelse) {
		this.cond = cond;
		this.styles = styles;
		this.orelse = orelse;
	}

	public String strings() {
		return buildConstant(styles);
	}

	public String elseStrings() {
		return buildConstant(orelse);
	}

	private String buildConstant(List<Expr> list) {
		StringBuilder sb = new StringBuilder();
		for (Expr s : list) {
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
