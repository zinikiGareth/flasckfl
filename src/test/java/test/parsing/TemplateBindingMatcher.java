package test.parsing;

import org.flasck.flas.parsedForm.TemplateBinding;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class TemplateBindingMatcher extends TypeSafeMatcher<TemplateBinding> {
	private final String name;

	public TemplateBindingMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description desc) {
		desc.appendText("TemplateBind[");
		desc.appendValue(name);
		desc.appendText("]");
	}

	@Override
	protected boolean matchesSafely(TemplateBinding bind) {
		if (!name.equals(bind.getSlot()))
			return false;
		return true;
	}

	public static TemplateBindingMatcher called(String name) {
		return new TemplateBindingMatcher(name);
	}

}
