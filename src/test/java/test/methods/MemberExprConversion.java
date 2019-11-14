package test.methods;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.method.MemberExprConvertor;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ExprMatcher;
import flas.matchers.MakeSendMatcher;

public class MemberExprConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Test
	public void dotOperatorBecomesMkSend() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		List<Pattern> args = new ArrayList<>();
		cd.addMethod(new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.UP, FunctionName.contractMethod(pos, cd.name(), "fred"), args));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		UnresolvedVar fld = new UnresolvedVar(pos, "fred");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.sending(FunctionName.contractMethod(pos, new SolidName(pkg, "Ctr"), "fred"), ExprMatcher.unresolved("from"), 0)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(nv);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}

	@Test
	public void dotOperatorBecomesMkSendExpectingMoreArgs() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr");
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Ctr"));
		List<Pattern> args = new ArrayList<>();
		FunctionName fn = FunctionName.contractMethod(pos, cd.name(), "fred");
		args.add(new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, fn, "x")));
		args.add(new TypedPattern(pos, LoadBuiltins.numberTR, new VarName(pos, fn, "y")));
		cd.addMethod(new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.UP, fn, args));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		UnresolvedVar fld = new UnresolvedVar(pos, "fred");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.sending(FunctionName.contractMethod(pos, new SolidName(pkg, "Ctr"), "fred"), ExprMatcher.unresolved("from"), 2)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(nv);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}
}
