package test.tc3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.EnsureListMessage;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.MessageChecker;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.PosType;
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
import flas.matchers.PosMatcher;

public class MethodTests {
	public interface RAV extends ResultAware, RepositoryVisitor {	}

	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final StringLiteral str = new StringLiteral(pos, "yoyo");
	private final List<Pattern> args = new ArrayList<>();
	private final ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, new SolidName(pkg, "X"), "meth"), args, null);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final CurrentTCState state = context.mock(CurrentTCState.class);
	private final RAV r = context.mock(RAV.class);
	private final StackVisitor sv = new StackVisitor();

	@Before
	public void init() {
		sv.push(r);
		context.checking(new Expectations() {{
			allowing(state).resolveAll(errors, true);
			allowing(state).hasGroup(); will(returnValue(false));
		}});
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void aSingleDebugMessageGivesAListOfMessage() {
		new FunctionChecker(errors, sv, state, null);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.debug))); will(returnValue(new PosType(pos, LoadBuiltins.debug)));
			oneOf(r).result(with(PosMatcher.type((Matcher)any(EnsureListMessage.class))));
		}});
		sv.leaveObjectMethod(meth);
	}

	@Test
	public void noMessagesNilType() {
		new FunctionChecker(errors, sv, state, null);
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(r).result(new PosType(pos, LoadBuiltins.nil));
		}});
		sv.leaveObjectMethod(meth);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void weCanHandleArgumentTypes() {
		new FunctionChecker(errors, sv, state, null);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
		args.add(tp);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		Slot s = context.mock(Slot.class);
		UnifiableType ut = context.mock(UnifiableType.class);
		context.checking(new Expectations() {{
			oneOf(state).createUT(null, "slot slot"); will(returnValue(ut));
			oneOf(ut).canBeType(pos, LoadBuiltins.string);
		}});
		sv.argSlot(s);
		sv.matchType(tp.type(), tp.var, null);
		sv.endArg(s);
		context.assertIsSatisfied();
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.debug))); will(returnValue(new PosType(pos, LoadBuiltins.debug)));
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type(Matchers.is(ut), (Matcher)Matchers.any(EnsureListMessage.class)))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	// TODO: Consolidating different types (a method with a Debug and a Send)
	
	@Test
	public void weCanHandleAnAssignMessage() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		os.addField(new StructField(pos, false, LoadBuiltins.stringTR, "s"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "s");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(var), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		sv.visitAssignSlot(msg.slot);
		sv.leaveMessage(msg);
		context.checking(new Expectations() {{
			oneOf(state).consolidate(pos, Arrays.asList(new PosType(pos, LoadBuiltins.message))); will(returnValue(new PosType(pos, LoadBuiltins.message)));
			oneOf(r).result(new PosType(pos, new EnsureListMessage(pos, LoadBuiltins.message)));
		}});
		sv.leaveObjectMethod(meth);
	}

	@Test
	public void theObjectMustHaveState() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(var), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.MyObject does not have state");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void theObjectMustHaveTheLeadVariable() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		os.addField(new StructField(pos, false, LoadBuiltins.stringTR, "s"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(var), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "there is no field x in test.repo.MyObject");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void errorsAreSuppressedIfTheExprWasAlreadyBad() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		os.addField(new StructField(pos, false, LoadBuiltins.stringTR, "s"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(var), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, new ErrorType()));
		context.checking(new Expectations() {{
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void theFieldMustHaveTheRightType() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		os.addField(new StructField(pos, false, LoadBuiltins.stringTR, "s"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "s");
		Expr nl = new NumericLiteral(pos, "42", 2);
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(var), nl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.number));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "the field s in test.repo.MyObject is of type String, not Number");
		}});
		sv.visitAssignSlot(msg.slot);
		sv.leaveMessage(msg);
	}

	@Test
	public void forNestedPathsTheLeadVariableMustReferToACompoundThing() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		os.addField(new StructField(pos, false, LoadBuiltins.stringTR, "s"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar lead = new UnresolvedVar(pos, "s");
		UnresolvedVar second = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(lead, second), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "field s in test.repo.MyObject is not a container");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void aNestedObjectMustHaveState() {
		ObjectDefn nestedObj = new ObjectDefn(pos, pos, new SolidName(pkg, "NestedObject"), true, new ArrayList<>());
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		TypeReference tr = new TypeReference(pos, "NestedObject");
		tr.bind(nestedObj);
		os.addField(new StructField(pos, false, tr, "obj"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar lead = new UnresolvedVar(pos, "obj");
		UnresolvedVar second = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(lead, second), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.NestedObject does not have state");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void aNestedPathMustExist() {
		ObjectDefn nestedObj = new ObjectDefn(pos, pos, new SolidName(pkg, "NestedObject"), true, new ArrayList<>());
		StateDefinition nos = new StateDefinition(pos);
		nestedObj.defineState(nos);
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		TypeReference tr = new TypeReference(pos, "NestedObject");
		tr.bind(nestedObj);
		os.addField(new StructField(pos, false, tr, "obj"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar lead = new UnresolvedVar(pos, "obj");
		UnresolvedVar second = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, Arrays.asList(lead, second), sl);
		meth.assignMessage(msg);
		new FunctionChecker(errors, sv, state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "there is no field x in test.repo.NestedObject");
		}});
		sv.visitAssignSlot(msg.slot);
	}
	
	@Test
	public void sendMessageIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.send))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.send));
		sv.leaveMessage(null);
	}

	@Test
	public void debugMessageIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.debug))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
	}

	@Test
	public void unionMessageIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.message))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.message));
		sv.leaveMessage(null);
	}

	@Test
	public void anEmptyListIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(LoadBuiltins.nil))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.nil));
		sv.leaveMessage(null);
	}

	@Test
	public void listOfDebugMessagesIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(pi))));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@Test
	public void consOfSendMessagesIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.send));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(pi))));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@Test
	public void listOfMessagesIsFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ExprResultMatcher.expr(Matchers.is(pi))));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void aNumberIsNotFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "Number cannot be a Message");
			oneOf(r).result(with(ExprResultMatcher.expr((Matcher)any(ErrorType.class))));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.number));
		sv.leaveMessage(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void listOfNumbersIsNotFine() {
		new MessageChecker(errors, state, sv, pos, meth);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.number));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "List[Number] cannot be a Message");
			oneOf(r).result(with(ExprResultMatcher.expr((Matcher)any(ErrorType.class))));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void anyOtherPolyIsNotFine() {
		StructDefn sda = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "Foo"), true, Arrays.asList(new PolyType(pos, "A")));
		new MessageChecker(errors, state, sv, pos, meth);
		PolyInstance pi = new PolyInstance(pos, sda, Arrays.asList(LoadBuiltins.message));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.Foo[Message] cannot be a Message");
			oneOf(r).result(with(ExprResultMatcher.expr((Matcher)any(ErrorType.class))));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}
}
