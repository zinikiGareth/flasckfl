package test.tc3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.patterns.HSIPatternOptions;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ErrorType;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.FunctionGroupTCState;
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
import org.zinutils.support.jmock.CaptureAction;

import flas.matchers.ApplyMatcher;
import flas.matchers.PosMatcher;

public class MethodTests {
	public interface RAV extends ResultAware, RepositoryVisitor {	}

	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private final InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final StringLiteral str = new StringLiteral(pos, "yoyo");
	private final List<Pattern> args = new ArrayList<>();
	private final ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, new SolidName(pkg, "X"), "meth"), args, null, null);
	private final ErrorReporter errors = context.mock(ErrorReporter.class);
	private final LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private final RepositoryReader repository = context.mock(RepositoryReader.class);
	private final CurrentTCState state = new FunctionGroupTCState(repository, new DependencyGroup());
	private final RAV r = context.mock(RAV.class);
	private final StackVisitor sv = new StackVisitor();
	private String fnCxt = "f";

	@Before
	public void init() {
		sv.push(r);
	}
	
	@Test
	public void aSingleDebugMessageGivesAListOfMessage() {
		new FunctionChecker(tracker, repository, sv, FunctionName.objectMethod(pos, null, "m"), state, null);
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(r).result(with(PosMatcher.type(Matchers.is(LoadBuiltins.listMessages))));
		}});
		sv.leaveObjectMethod(meth);
	}

	@Test
	public void noMessagesNilType() {
		new FunctionChecker(tracker, repository, sv, FunctionName.objectMethod(pos, null, "m"), state, null);
		sv.leaveMessage(null);
		context.checking(new Expectations() {{
			oneOf(r).result(new PosType(pos, LoadBuiltins.nil));
		}});
		sv.leaveObjectMethod(meth);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void weCanHandleArgumentTypes() {
		new FunctionChecker(tracker, repository, sv, meth.name(), state, null);
		TypedPattern tp = new TypedPattern(pos, LoadBuiltins.stringTR, new VarName(pos, meth.name(), "str"));
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
			oneOf(r).result(with(PosMatcher.type((Matcher)ApplyMatcher.type((Matcher)Matchers.any(UnifiableType.class), Matchers.is(LoadBuiltins.listMessages)))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	// TODO: Consolidating different types (a method with a Debug and a Send)
	
	@Test
	public void weCanHandleAnAssignMessage() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos, s.name());
		StructField sf = new StructField(pos, os, false, true, LoadBuiltins.stringTR, "s");
		os.addField(sf);
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "s");
		var.bind(sf);
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, var, sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		sv.visitAssignSlot(msg.slot);
		sv.leaveMessage(msg);
		context.checking(new Expectations() {{
			oneOf(r).result(new PosType(pos, LoadBuiltins.listMessages));
		}});
		sv.leaveObjectMethod(meth);
	}

	@Test
	public void theObjectMustHaveState() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		var.bind(new StructField(pos, null, false, true, LoadBuiltins.stringTR, "x"));
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, var, sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot use x as the main slot in assignment");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void theObjectMustHaveTheLeadVariable() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		StructField sf = new StructField(pos, os, false, true, LoadBuiltins.stringTR, "s");
		os.addField(sf);
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		StructDefn sd = new StructDefn(pos, FieldsType.STRUCT, "test.repo", "Struct", false);
		var.bind(new StructField(pos, sd, false, true, LoadBuiltins.stringTR, "x"));
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, var, sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot use x as the main slot in assignment");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void errorsAreSuppressedIfTheExprWasAlreadyBad() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos);
		os.addField(new StructField(pos, os, false, true, LoadBuiltins.stringTR, "s"));
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, var, sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, new ErrorType()));
		context.checking(new Expectations() {{
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void theFieldMustHaveTheRightType() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos, s.name());
		StructField sf = new StructField(pos, os, false, true, LoadBuiltins.stringTR, "s");
		os.addField(sf);
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar var = new UnresolvedVar(pos, "s");
		var.bind(sf);
		Expr nl = new NumericLiteral(pos, "42", 2);
		AssignMessage msg = new AssignMessage(pos, var, nl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.number));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "the field s is of type String, not Number");
		}});
		sv.visitAssignSlot(msg.slot);
		sv.leaveMessage(msg);
	}

	@Test
	public void forNestedPathsTheLeadVariableMustReferToACompoundThing() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos, s.name());
		StructField sf = new StructField(pos, os, false, true, LoadBuiltins.stringTR, "s");
		os.addField(sf);
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar lead = new UnresolvedVar(pos, "s");
		lead.bind(sf);
		UnresolvedVar second = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, new MemberExpr(pos, lead, second), sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "field s is not a container");
		}});
		sv.visitAssignSlot(msg.slot);
	}

	@Test
	public void aNestedObjectMustHaveState() {
		ObjectDefn nestedObj = new ObjectDefn(pos, pos, new SolidName(pkg, "NestedObject"), true, new ArrayList<>());
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos, s.name());
		TypeReference tr = new TypeReference(pos, "NestedObject");
		tr.bind(nestedObj);
		StructField sf = new StructField(pos, os, false, true, tr, "obj");
		os.addField(sf);
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar lead = new UnresolvedVar(pos, "obj");
		lead.bind(sf);
		UnresolvedVar second = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, new MemberExpr(pos, lead, second), sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
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
		StateDefinition nos = new StateDefinition(pos, nestedObj.name());
		nestedObj.defineState(nos);
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		StateDefinition os = new StateDefinition(pos, nestedObj.name());
		TypeReference tr = new TypeReference(pos, "NestedObject");
		tr.bind(nestedObj);
		StructField sf = new StructField(pos, os, false, true, tr, "obj");
		os.addField(sf);
		s.defineState(os);
		s.addMethod(meth);
		UnresolvedVar lead = new UnresolvedVar(pos, "obj");
		lead.bind(sf);
		UnresolvedVar second = new UnresolvedVar(pos, "x");
		Expr sl = new StringLiteral(pos, "hello");
		AssignMessage msg = new AssignMessage(pos, new MemberExpr(pos, lead, second), sl);
		meth.assignMessage(msg);
		new FunctionChecker(tracker, repository, sv, meth.name(), state, meth);
		sv.visitAssignMessage(msg);
		sv.result(new ExprResult(pos, LoadBuiltins.string));
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "there is no field x in test.repo.NestedObject");
		}});
		sv.visitAssignSlot(msg.slot);
	}
	
	@Test
	public void sendMessageIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.send));
		sv.leaveMessage(null);
	}

	@Test
	public void debugMessageIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.debug));
		sv.leaveMessage(null);
	}

	@Test
	public void unionMessageIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.message));
		sv.leaveMessage(null);
	}

	@Test
	public void anEmptyListIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.nil));
		sv.leaveMessage(null);
	}

	@Test
	public void listOfDebugMessagesIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@Test
	public void consOfSendMessagesIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.cons, Arrays.asList(LoadBuiltins.send));
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@Test
	public void listOfMessagesIsFine() {
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.message));
		context.checking(new Expectations() {{
			oneOf(r).result(new ExprResult(pos, LoadBuiltins.listMessages));
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void aNumberIsNotFine() {
		state.bindVarToUT(fnCxt, meth.name().uniqueName(), state.createUT(meth.location(), "method " + meth.name().uniqueName()));
		new FunctionChecker(tracker, repository, sv, meth.name(), state, null);
		SendMessage msg = new SendMessage(pos, new NumericLiteral(pos, 42));
		meth.sendMessage(msg);
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		CaptureAction capture = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "Number cannot be a Message");
			oneOf(r).result(with(PosMatcher.type((Matcher)Matchers.any(ErrorType.class)))); will(capture);
		}});
		sv.result(new ExprResult(pos, LoadBuiltins.number));
		sv.leaveMessage(msg);
		sv.leaveObjectMethod(meth);
		HashMap<TypeBinder, PosType> mts = new HashMap<>();
		mts.put(meth, (PosType) capture.get(0));
		state.groupDone(tracker, mts, new HashMap<TypeBinder, PosType>());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
//	@Ignore // this needs to be fixed but is too complicated right now.  See also one of the UTs
	public void listOfNumbersIsNotFine() {
		state.bindVarToUT(fnCxt, meth.name().uniqueName(), state.createUT(meth.location(), "method " + meth.name().uniqueName()));
		new FunctionChecker(tracker, repository, sv, meth.name(), state, null);
		SendMessage msg = new SendMessage(pos, new NumericLiteral(pos, 42));
		meth.sendMessage(msg);
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		PolyInstance pi = new PolyInstance(pos, LoadBuiltins.list, Arrays.asList(LoadBuiltins.number));
		CaptureAction capture = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "Number cannot be a Message");
			oneOf(r).result(with(PosMatcher.type((Matcher)Matchers.any(ErrorType.class)))); will(capture);
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(msg);
		sv.leaveObjectMethod(meth);
		HashMap<TypeBinder, PosType> mts = new HashMap<>();
		mts.put(meth, (PosType) capture.get(0));
		state.groupDone(tracker, mts, new HashMap<TypeBinder, PosType>());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void anyOtherPolyIsNotFine() {
		state.bindVarToUT(fnCxt, meth.name().uniqueName(), state.createUT(meth.location(), "method " + meth.name().uniqueName()));
		new FunctionChecker(tracker, repository, sv, meth.name(), state, null);
		SendMessage msg = new SendMessage(pos, new NumericLiteral(pos, 42));
		meth.sendMessage(msg);
		StructDefn sda = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "Foo"), true, Arrays.asList(new PolyType(pos, new SolidName(null, "A"))));
		new MessageChecker(tracker, repository, state, sv, fnCxt, meth, null);
		PolyInstance pi = new PolyInstance(pos, sda, Arrays.asList(LoadBuiltins.message));
		CaptureAction capture = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "test.repo.Foo[Message] cannot be a Message");
			oneOf(r).result(with(PosMatcher.type((Matcher)Matchers.any(ErrorType.class)))); will(capture);
		}});
		sv.result(new ExprResult(pos, pi));
		sv.leaveMessage(msg);
		sv.leaveObjectMethod(meth);
		HashMap<TypeBinder, PosType> mts = new HashMap<>();
		mts.put(meth, (PosType) capture.get(0));
		state.groupDone(tracker, mts, new HashMap<TypeBinder, PosType>());
	}
}
