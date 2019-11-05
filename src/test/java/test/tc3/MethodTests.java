package test.tc3;

import java.util.ArrayList;
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
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository.Visitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.flas.tc3.CurrentTCState;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;
import org.flasck.flas.tc3.FunctionChecker;
import org.flasck.flas.tc3.UnifiableType;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

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
	public void aSingleDebugMessageGivesTheStaticTypeDebug() {
		sv.push(new FunctionChecker(errors, sv, state));
		SendMessage msg = new SendMessage(pos, new ApplyExpr(pos, LoadBuiltins.debug, str));
		meth.sendMessage(msg);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(r).result(LoadBuiltins.debug);
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

	@SuppressWarnings("unchecked")
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
			oneOf(state).createUT(); will(returnValue(ut));
		}});
		sv.argSlot(s);
		context.assertIsSatisfied();
		sv.visitPattern(tp, false);
		sv.visitTypedPattern(tp, false);
		sv.leavePattern(tp, false);
		sv.endArg(s);
		sv.visitSendMessage(msg);
		sv.result(new ExprResult(LoadBuiltins.debug));
		context.checking(new Expectations() {{
			oneOf(r).result(with(ApplyMatcher.type(Matchers.is(ut), Matchers.is(LoadBuiltins.debug))));
		}});
		sv.leaveObjectMethod(meth);
	}
	
	// Handle typing errors (duh!) - must always be a "Message"
	// Consolidating different types 
}
