package test.parsing;

import org.flasck.flas.parsedForm.TemplateBinding;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class TemplateBindingMatcher extends TypeSafeMatcher<TemplateBinding> {
	private final String name;
	private String expr;
	private String sendsTo;

	public TemplateBindingMatcher(String name) {
		this.name = name;
	}
	
	public TemplateBindingMatcher expr(String string) {
		expr = string;
		return this;
	}

	public TemplateBindingMatcher sendsTo(String string) {
		this.sendsTo = string;
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
		if (!name.equals(bind.slot))
			return false;
		if ((expr == null) != (bind.expr == null))
			return false;
		if (expr != null && !expr.equals(bind.expr.toString()))
			return false;
		if ((sendsTo == null) != (bind.sendsTo == null))
			return false;
		return true;
	}

	public static TemplateBindingMatcher called(String name) {
		return new TemplateBindingMatcher(name);
	}
}
