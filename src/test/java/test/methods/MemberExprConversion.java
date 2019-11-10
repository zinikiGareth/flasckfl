package test.methods;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.method.MemberExprConvertor;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.ExprMatcher;

public class MemberExprConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	// TODO: this needs much more plumbing, but we'll get back to that
	// It needs from bound to a contract
	// It needs to get fld from the contract
	// It needs the type of that
	@Test
	public void dotOperatorBecomesMkSend() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.sending(FunctionName.contractMethod(pos, new SolidName(pkg, "Ctr"), "fld"), ExprMatcher.unresolved("from"), 0)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(nv);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}
}
