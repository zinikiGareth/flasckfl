package flas.matchers;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.TypedPattern;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ContractMethodMatcher extends TypeSafeMatcher<ContractMethodDecl> {
	private final String name;
	private boolean optional = false;
	private List<Matcher<Pattern>> args = new ArrayList<>();
	private TypedPatternMatcher handler;

	public ContractMethodMatcher(String name) {
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ContractMethod(");
		if (optional)
			arg0.appendValue("optional");
		else
			arg0.appendValue("required");
		arg0.appendText(" ");
		arg0.appendValue(name);
		for (Matcher<Pattern> m : args) {
			arg0.appendText(" ");
			arg0.appendValue(m);
		}
		if (handler != null) {
			arg0.appendText("->");
			arg0.appendValue(handler);
		}
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(ContractMethodDecl cmd) {
		if (optional != !cmd.required)
			return false;
		if (args.size() != cmd.args.size())
			return false;
		for (int i=0;i<args.size();i++) {
			if (!args.get(i).matches(cmd.args.get(i)))
				return false;
		}
		if (handler != null) {
			TypedPattern tp = cmd.handler;
			if (!handler.matches(tp))
				return false;
		}
		return true;
	}

	public ContractMethodMatcher optional() {
		this.optional = true;
		return this;
	}

	public ContractMethodMatcher arg(Matcher<Pattern> matcher) {
		this.args.add(matcher);
		return this;
	}

	public ContractMethodMatcher handler(TypedPatternMatcher typed) {
		this.handler = typed;
		return this;
	}

	public static ContractMethodMatcher named(String name) {
		return new ContractMethodMatcher(name);
	}
}
