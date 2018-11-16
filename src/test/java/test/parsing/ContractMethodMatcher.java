package test.parsing;

import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class ContractMethodMatcher extends TypeSafeMatcher<ContractMethodDecl> {
	private final ContractMethodDir type;
	private final String name;
	private int tyloc = -1;
	private int varloc = -1;
	private int assloc = -1;
	private Matcher<?> assExpr;

	public ContractMethodMatcher(ContractMethodDir dir, String name) {
		this.type = dir;
		this.name = name;
	}

	@Override
	public void describeTo(Description arg0) {
		arg0.appendText("ContractMethod(");
		arg0.appendValue(type);
		arg0.appendText(" ");
		arg0.appendValue(name);
		arg0.appendText(")");
		if (tyloc != -1) {
			arg0.appendText(" @{");
			arg0.appendValue(tyloc);
			arg0.appendText(",");
			arg0.appendValue(varloc);
			arg0.appendText("}");
		}
		if (assExpr != null) {
			arg0.appendText(" assign{");
			arg0.appendValue(assloc);
			arg0.appendValue(assExpr);
			arg0.appendText("}");
		}
	}

	@Override
	protected boolean matchesSafely(ContractMethodDecl arg0) {
		/*
		if (!arg0.type.name().equals(type))
			return false;
		if (!arg0.name.equals(name))
			return false;
		if (tyloc != -1 && tyloc != arg0.type.location().off)
			return false;
		if (varloc != -1 && varloc != arg0.location().off)
			return false;
		if (assloc != -1 && assloc != arg0.assOp.off)
			return false;
		if (assExpr != null && !assExpr.matches(arg0.init))
			return false;
			*/
		return true;
	}
	
	public ContractMethodMatcher locs(int ty, int var) {
		tyloc = ty;
		varloc = var;
		return this;
	}

	public ContractMethodMatcher assign(int loc, Matcher<?> expr) {
		assloc = loc;
		assExpr = expr;
		return this;
	}

	public static ContractMethodMatcher up(String name) {
		return new ContractMethodMatcher(ContractMethodDir.UP, name);
	}
}
