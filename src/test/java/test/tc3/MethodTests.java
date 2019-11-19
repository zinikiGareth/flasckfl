package test.tc3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.EnsureListMessage;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.MessageChecker;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;
import flas.matchers.ExprResultMatcher;
import flas.matchers.PolyInstanceMatcher;

public class MethodTests {
	public interface RAV extends ResultAware, Visitor {	}

	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final StringLiteral str = new StringLiteral(pos, "yoyo");
	private final List<Pattern> args = new ArrayList<>();
	private final ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, new SolidName(pkg, "X"), "meth"), args);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final RAV r = context.mock(RAV.class);
	private final StackVisitor sv = new StackVisitor();

	@Before
	public void init() {
		sv.push(r);
	}
	
	@Test
	public void aSingleDebugMessageGivesAListOfMessage() {
		sv.push(new FunctionChecker(errors, sv, state));
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(LoadBuiltins.debug)); will(returnValue(LoadBuiltins.debug));
			oneOf(r).result(with(any(EnsureListMessage.class)));
		}});
		sv.leaveObjectMethod(meth);
	}

	@Test
	public void noMessagesNullType() {
		sv.push(new FunctionChecker(errors, sv, state));
		context.checking(new Expectations() {{
			oneOf(r).result(null);
		}});
		sv.leaveObjectMethod(meth);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void weCanHandleArgumentTypes() {
		sv.push(new FunctionChecker(errors, sv, state));
		TypedPattern tp = new TypedPattern(pos, new TypeReference(pos, "String"), new VarName(pos, meth.name(), "str"));
		args.add(tp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		Slot s = context.mock(Slot.class);
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "unknown"); will(returnValue(ut));
		}});
		sv.argSlot(s);
		context.assertIsSatisfied();
		sv.visitPattern(tp, false);
		sv.visitTypedPattern(tp, false);
		sv.leavePattern(tp, false);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(LoadBuiltins.debug)); will(returnValue(LoadBuiltins.debug));
			oneOf(r).result(with(ApplyMatcher.type(Matchers.is(ut), (Matcher)Matchers.any(EnsureListMessage.class))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	// TODO: Consolidating different types (a method with a Debug and a Send)
	
	@Test
	public void sendMessageIsFine() {
		new MessageChecker(errors, state, sv, pos);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.send))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.send));
	}

	@Test
	public void debugMessageIsFine() {
		new MessageChecker(errors, state, sv, pos);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.debug))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
	}

	@Test
	public void unionMessageIsFine() {
		new MessageChecker(errors, state, sv, pos);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.message))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.message));
	}

	@Test
	public void anEmptyListIsFine() {
		new MessageChecker(errors, state, sv, pos);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.nil))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.nil));
	}

	@Test
	public void listOfDebugMessagesIsFine() {
		new MessageChecker(errors, state, sv, pos);
		PolyInstance pi = new PolyInstance(LoadBuiltins.list, Arrays.asList(LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(pi))));
		}});
		sv.result(new ExprResult(pos, pi));
	}

	@Test
	public void consOfSendMessagesIsFine() {
		new MessageChecker(errors, state, sv, pos);
		PolyInstance pi = new PolyInstance(LoadBuiltins.cons, Arrays.asList(LoadBuiltins.send));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(pi))));
		}});
		sv.result(new ExprResult(pos, pi));
	}

	@Test
	public void listOfMessagesIsFine() {
		new MessageChecker(errors, state, sv, pos);
		PolyInstance pi = new PolyInstance(LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(pi))));
		}});
		sv.result(new ExprResult(pos, pi));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void aNumberIsNotFine() {
		new MessageChecker(errors, state, sv, pos);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "Number cannot be a Message");
			oneOf(r).result(with(ExprResultMatcher.expr((Matcher)any(ErrorType.class))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.number));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void listOfNumbersIsNotFine() {
		new MessageChecker(errors, state, sv, pos);
		PolyInstance pi = new PolyInstance(LoadBuiltins.list, Arrays.asList(LoadBuiltins.number));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "List[Number] cannot be a Message");
			oneOf(r).result(with(ExprResultMatcher.expr((Matcher)any(ErrorType.class))));
		}});
		sv.result(new ExprResult(pos, pi));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void anyOtherPolyIsNotFine() {
		StructDefn sda = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "Foo"), true, Arrays.asList(new PolyType(pos, "A")));
		new MessageChecker(errors, state, sv, pos);
		PolyInstance pi = new PolyInstance(sda, Arrays.asList(LoadBuiltins.message));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.Foo[Message] cannot be a Message");
			oneOf(r).result(with(ExprResultMatcher.expr((Matcher)any(ErrorType.class))));
		}});
		sv.result(new ExprResult(pos, pi));
	}
}
