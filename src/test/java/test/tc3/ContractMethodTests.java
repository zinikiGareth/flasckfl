package test.tc3;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.EnsureListMessage;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.PosType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.PosMatcher;
import test.tc3.MethodTests.RAV;

public class ContractMethodTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final List<Pattern> args = new ArrayList<>();
	private final List<TypedPattern> cmdargs = new ArrayList<>();
	private final ContractMethodDecl cm = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, new SolidName(pkg, "AContract"), "meth"), cmdargs, null);
	private final ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, new CSName(new CardName(pkg, "CardName"), "S0"), cm.name.name), args, null);
	private final StackVisitor sv = new StackVisitor();
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final StringLiteral str = new StringLiteral(pos, "yoyo");
	private final RAV r = context.mock(RAV.class);
	private RepositoryReader repository = context.mock(RepositoryReader.class);

	@Before
	public void init() {
		meth.bindFromContract(cm);
		sv.push(r);
		context.checking(new Expectations() {{
			allowing(state).resolveAll(errors, true);
			allowing(state).hasGroup(); will(returnValue(false));
		}});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void weCanMatchASimpleUntypedArgument() {
		new FunctionChecker(errors, repository, sv, state, meth);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		cmdargs.add(tp);
		VarPattern vp = new VarPattern(pos, new VarName(pos, meth.name(), "str"));
		args.add(vp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		Slot s = context.mock(Slot.class);
		sv.argSlot(s);
		sv.varInIntro(vp.name(), vp, null);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.debug))); will(returnValue(new PosType(pos, LoadBuiltins.debug)));
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), (Matcher)Matchers.any(EnsureListMessage.class)))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void theArgumentTypeMayBeSpecifiedIfItsTheSame() {
		new FunctionChecker(errors, repository, sv, state, meth);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		cmdargs.add(tp);
		args.add(tp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		Slot s = context.mock(Slot.class);
		sv.argSlot(s);
		sv.matchType(tp.type(), tp.var, null);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.debug))); will(returnValue(new PosType(pos, LoadBuiltins.debug)));
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(LoadBuiltins.string), (Matcher)Matchers.any(EnsureListMessage.class)))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void itIsAnErrorToSpecifyADifferentArgumentType() {
		new FunctionChecker(errors, repository, sv, state, meth);
		TypedPattern ctp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		cmdargs.add(ctp);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.numberTR, new VarName(pos, meth.name(), "str"));
		args.add(tp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot bind str to Number when the contract specifies String");
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.debug))); will(returnValue(new PosType(pos, LoadBuiltins.debug)));
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type((Matcher)Matchers.any(ErrorType.class), (Matcher)Matchers.any(EnsureListMessage.class)))));
		}});
		Slot s = context.mock(Slot.class);
		sv.argSlot(s);
		sv.matchType(tp.type(), tp.var, null);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		sv.leaveObjectMethod(meth);
	}
}