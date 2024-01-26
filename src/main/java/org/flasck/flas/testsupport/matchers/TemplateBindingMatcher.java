package org.flasck.flas.testsupport.matchers;

import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parsedForm.TemplateBinding;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TemplateBindingMatcher extends TypeSafeMatcher<TemplateBinding> {
	private final String name;
	private String expr;
	private TemplateName sendsTo;

	public TemplateBindingMatcher(String name) {
		this.name = name;
	}
	
	public TemplateBindingMatcher expr(String string) {
		expr = string;
		return this;
	}

	public TemplateBindingMatcher sendsTo(TemplateName ot) {
		this.sendsTo = ot;
		return this;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("TemplateBind[");
		desc.appendValue(name);
		if (expr != null) {
			desc.appendText("<-");
			desc.appendValue(expr);
		}
		if (sendsTo != null) {
			desc.appendText("=>");
			desc.appendValue(sendsTo);
		}
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(TemplateBinding bind) {
		if (!name.equals(bind.assignsTo.text))
			return false;
		if ((expr == null) != (bind.defaultBinding == null || bind.defaultBinding.expr == null))
			return false;
		if (expr != null && !expr.equals(bind.defaultBinding.expr.toString()))
			return false;
		if ((sendsTo == null) != (bind.defaultBinding == null || bind.defaultBinding.sendsTo == null))
			return false;
		if (sendsTo != null && !sendsTo.equals(bind.defaultBinding.sendsTo.name))
			return false;
		return true;
	}

	public static TemplateBindingMatcher called(String name) {
		return new TemplateBindingMatcher(name);
	}
}
