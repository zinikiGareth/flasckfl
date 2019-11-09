package test.methods;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.method.MemberExprConvertor;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class MemberExprConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
//	private final PackageName pkg = new PackageName("test.repo");

	// TODO: this needs much more plumbing, but we'll get back to that
	// It needs from bound to a contract
	// It needs to get fld from the contract
	// It needs the type of that
	@Test
	public void dotOperatorBecomesMkSend() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.curry(0)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(nv);
		Traverser gen = new Traverser(mc);
		gen.visitExpr(me, 0);
	}

}
