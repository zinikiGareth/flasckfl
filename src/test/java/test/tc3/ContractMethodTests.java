package test.tc3;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.testsupport.matchers.ApplyMatcher;
import org.flasck.flas.testsupport.matchers.PosMatcher;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.FunctionGroupTCState;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.tc3.MethodTests.RAV;

public class ContractMethodTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final List<Pattern> args = new ArrayList<>();
	private final List<TypedPattern> cmdargs = new ArrayList<>();
	private final ContractMethodDecl cm = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, new SolidName(pkg, "AContract"), "meth"), cmdargs, null);
	private final ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, new CSName(new CardName(pkg, "CardName"), "S0"), cm.name.name), args, null, null);
	private final StackVisitor sv = new StackVisitor();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);
	private CurrentTCState state = new FunctionGroupTCState(repository, new DependencyGroup());
	private final StringLiteral str = new StringLiteral(pos, "yoyo");
	private final RAV r = context.mock(RAV.class);

	@Before
	public void init() {
		meth.bindFromContract(cm);
		sv.push(r);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void weCanMatchASimpleUntypedArgument() {
		new FunctionChecker(errors, repository, sv, meth.name(), state, meth);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		cmdargs.add(tp);
		VarPattern vp = new VarPattern(pos, new VarName(pos, meth.name(), "str"));
		args.add(vp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		ArgSlot s = new ArgSlot(0, new HSIPatternOptions());
		sv.argSlot(s);
		sv.varInIntro(vp.name(), vp, null);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), (Matcher)Matchers.is(LoadBuiltins.listMessages)))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void theArgumentTypeMayBeSpecifiedIfItsTheSame() {
		new FunctionChecker(errors, repository, sv, meth.name(), state, meth);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		cmdargs.add(tp);
		args.add(tp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		ArgSlot s = new ArgSlot(0, new HSIPatternOptions());
		sv.argSlot(s);
		sv.matchType(tp.type(), tp.var, null);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), (Matcher)Matchers.is(LoadBuiltins.listMessages)))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void itIsAnErrorToSpecifyADifferentArgumentType() {
		new FunctionChecker(errors, repository, sv, meth.name(), state, meth);
		TypedPattern ctp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		cmdargs.add(ctp);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.numberTR, new VarName(pos, meth.name(), "str"));
		args.add(tp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot bind str to Number when the contract specifies String");
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type((Matcher)Matchers.any(ErrorType.class), (Matcher)Matchers.is(LoadBuiltins.listMessages)))));
		}});
		ArgSlot s = new ArgSlot(0, new HSIPatternOptions());
		sv.argSlot(s);
		sv.matchType(tp.type(), tp.var, null);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		sv.leaveObjectMethod(meth);
	}
}
