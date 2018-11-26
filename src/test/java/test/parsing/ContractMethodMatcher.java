package test.parsing;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ContractMethodMatcher extends TypeSafeMatcher<ContractMethodDecl> {
	private final ContractMethodDir type;
	private final String name;
	private boolean optional = false;
	private List<PatternMatcher> args = new ArrayList<>();

	public ContractMethodMatcher(ContractMethodDir dir, String name) {
		this.type = dir;
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ContractMethod(");
		if (optional)
			arg0.appendValue("optional");
		else
			arg0.appendValue("required");
		arg0.appendValue(type);
		arg0.appendText(" ");
		arg0.appendValue(name);
		for (PatternMatcher m : args) {
			arg0.appendText(" ");
			arg0.appendValue(m);
		}
		arg0.appendText(")");
	}

	@Override
	protected boolean matchesSafely(ContractMethodDecl cmd) {
		if (!cmd.dir.equals(type))
			return false;
		if (optional != !cmd.required)
			return false;
		if (args.size() != cmd.args.size())
			return false;
		for (int i=0;i<args.size();i++) {
			if (!args.get(i).matches(cmd.args.get(i)))
				return false;
		}
		return true;
	}

	public ContractMethodMatcher optional() {
		this.optional = true;
		return this;
	}

	public ContractMethodMatcher arg(PatternMatcher matcher) {
		this.args.add(matcher);
		return this;
	}

	public static ContractMethodMatcher up(String name) {
		return new ContractMethodMatcher(ContractMethodDir.UP, name);
	}

	public static ContractMethodMatcher down(String name) {
		return new ContractMethodMatcher(ContractMethodDir.DOWN, name);
	}
}
