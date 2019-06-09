package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;

public class TemplateStylingOption {
	public final Expr cond;
	public final List<StringLiteral> styles;

	public TemplateStylingOption(Expr cond, List<StringLiteral> styles) {
		this.cond = cond;
		this.styles = styles;
	}
}
