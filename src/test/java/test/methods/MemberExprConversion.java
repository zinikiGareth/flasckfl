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
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.testsupport.matchers.ExprMatcher;
import org.flasck.flas.testsupport.matchers.MakeAcorMatcher;
import org.flasck.flas.testsupport.matchers.MakeSendMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MemberExprConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null, null);
	private final PackageName pkg = new PackageName("test.repo");

	@Before
	public void allow() {
		context.checking(new Expectations() {{
			allowing(nv).push(with(any(MemberExprConvertor.class)));
		}});
	}
	@Test
	public void dotOperatorBecomesMkSend() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr.Up");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		List<TypedPattern> args = new ArrayList<>();
		cd.addMethod(new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cd.name(), "fred"), args, null));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		UnresolvedVar fld = new UnresolvedVar(pos, "fred");
		MemberExpr me = new MemberExpr(pos, from, fld);
		me.bindContainerType(cd);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.sending(FunctionName.contractMethod(pos, new SolidName(pkg, "Ctr"), "fred"), ExprMatcher.unresolved("from"), 0)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(null, nv, null, me);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}

	@Test
	public void dotOperatorBecomesMkSendExpectingMoreArgs() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "Ctr.Down");
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Ctr"));
		List<TypedPattern> args = new ArrayList<>();
		FunctionName fn = FunctionName.contractMethod(pos, cd.name(), "fred");
		args.add(new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, fn, "x")));
		args.add(new TypedPattern(pos, LoadBuiltins.numberTR, new VarName(pos, fn, "y")));
		cd.addMethod(new ContractMethodDecl(pos, pos, pos, true, fn, args, null));
		ctr.bind(cd);
		TypedPattern tp = new TypedPattern(pos, ctr, new VarName(pos, cd.name(), "from"));
		from.bind(tp);
		UnresolvedVar fld = new UnresolvedVar(pos, "fred");
		MemberExpr me = new MemberExpr(pos, from, fld);
		me.bindContainerType(cd);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.sending(FunctionName.contractMethod(pos, new SolidName(pkg, "Ctr"), "fred"), ExprMatcher.unresolved("from"), 2)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(null, nv, null, me);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}

	@Test
	public void dotOperatorCanHandleUDD() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference ctr = new TypeReference(pos, "ObjectDefn");
		ObjectDefn od = new ObjectDefn(pos, pos, new SolidName(pkg, "ObjectDefn"), true, new ArrayList<>());
		ctr.bind(od);
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, pkg, "udd"), null);
		List<Pattern> args = new ArrayList<>();
		FunctionName fred = FunctionName.objectMethod(pos, od.name(), "fred");
		od.addMethod(new ObjectMethod(pos, fred, args, null, null));
		from.bind(udd);
		UnresolvedVar fld = new UnresolvedVar(pos, "fred");
		MemberExpr me = new MemberExpr(pos, from, fld);
		me.bindContainerType(od);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeSendMatcher.sending(fred, ExprMatcher.unresolved("from"), 0)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(null, nv, null, me);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}

	@Test
	public void dotOperatorCanHandleStruct() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		TypeReference tr = new TypeReference(pos, "StructDefn");
		StructDefn sd = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "StructDefn"), true, new ArrayList<>());
		StructField sf = new StructField(pos, pos, sd, true, true, LoadBuiltins.stringTR, "fred", null);
		sf.fullName(new VarName(pos, sd.name(), "fred"));
		sd.addField(sf);
		tr.bind(sd);
		TypedPattern tp = new TypedPattern(pos, tr, new VarName(pos, sd.name(), "from"));
		from.bind(tp);
		UnresolvedVar fld = new UnresolvedVar(pos, "fred");
		MemberExpr me = new MemberExpr(pos, from, fld);
		me.bindContainerType(sd);
		context.checking(new Expectations() {{
			oneOf(nv).result(with(MakeAcorMatcher.acor(FunctionName.contractMethod(pos, new SolidName(pkg, "StructDefn"), "_field_fred"), ExprMatcher.unresolved("from"), 0)));
		}});
		MemberExprConvertor mc = new MemberExprConvertor(null, nv, null, me);
		Traverser gen = new Traverser(mc).withMemberFields();
		gen.visitExpr(me, 0);
	}
}
