package test.repository;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AssignMessage;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ExprMatcher;

public class MethodTraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral str = new StringLiteral(pos, "hello");
	final NumericLiteral number = new NumericLiteral(pos, 42);
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Repository r = new Repository();
	final RepositoryVisitor v = context.mock(RepositoryVisitor.class);
	final UnresolvedVar dest = new UnresolvedVar(pos, "dest");
	final FunctionName nameF = FunctionName.standaloneMethod(pos, pkg, "meth");
	ArrayList<Pattern> args = new ArrayList<>();
	private ObjectMethod om = new ObjectMethod(pos, nameF, args, null);
	private StandaloneMethod sm = new StandaloneMethod(om);

	private SendMessage msg1 = new SendMessage(pos, new ApplyExpr(pos, dest, str));
	private UnresolvedVar slotX = new UnresolvedVar(pos, "x");
	private List<UnresolvedVar> slotName = new ArrayList<>();
	private AssignMessage msg2 = new AssignMessage(pos, slotName, new ApplyExpr(pos, dest, str));
	private VarPattern destP = new VarPattern(pos, new VarName(pos, nameF, "dest"));
	final ErrorReporter errors = context.mock(ErrorReporter.class);

	@Test
	public void traverseAMethodWithSendMessage() {
		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			ExprMatcher ae = ExprMatcher.apply(ExprMatcher.unresolved("dest"), ExprMatcher.string("hello"));
			oneOf(v).visitStandaloneMethod(sm); inSequence(s);
			oneOf(v).visitObjectMethod(om); inSequence(s);
			oneOf(v).visitPattern(destP, false); inSequence(s);
			oneOf(v).visitVarPattern(destP, false); inSequence(s);
			oneOf(v).visitPatternVar(pos, "dest"); inSequence(s);
			oneOf(v).leavePattern(destP, false); inSequence(s);
			oneOf(v).visitMessage(msg1); inSequence(s);
			oneOf(v).visitSendMessage(msg1); inSequence(s);
			oneOf(v).visitExpr((ApplyExpr) with(ae), with(0)); inSequence(s);
			oneOf(v).visitApplyExpr((ApplyExpr) with(ae)); inSequence(s);
			oneOf(v).visitExpr((UnresolvedVar) with(ExprMatcher.unresolved("dest")), with(1)); inSequence(s);
			oneOf(v).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("dest")), with(1)); inSequence(s);
			oneOf(v).visitExpr(str, 0); inSequence(s);
			oneOf(v).visitStringLiteral(str); inSequence(s);
			oneOf(v).leaveApplyExpr((ApplyExpr) with(ae)); inSequence(s);
			oneOf(v).leaveSendMessage(msg1); inSequence(s);
			oneOf(v).leaveMessage(msg1); inSequence(s);
			oneOf(v).leaveObjectMethod(om); inSequence(s);
			oneOf(v).leaveStandaloneMethod(sm); inSequence(s);
			oneOf(v).traversalDone();
		}});
		args.add(destP);
		dest.bind(destP);
		om.sendMessage(msg1);
		r.newStandaloneMethod(errors, sm);
		r.traverse(v);
	}

	@Test
	public void traverseAMethodWithAssignMessage() {
		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			ExprMatcher ae = ExprMatcher.apply(ExprMatcher.unresolved("dest"), ExprMatcher.string("hello"));
			oneOf(v).visitStandaloneMethod(sm); inSequence(s);
			oneOf(v).visitObjectMethod(om); inSequence(s);
			oneOf(v).visitPattern(destP, false); inSequence(s);
			oneOf(v).visitVarPattern(destP, false); inSequence(s);
			oneOf(v).visitPatternVar(pos, "dest"); inSequence(s);
			oneOf(v).leavePattern(destP, false); inSequence(s);
			oneOf(v).visitMessage(msg2); inSequence(s);
			oneOf(v).visitAssignMessage(msg2); inSequence(s);
			oneOf(v).visitExpr((ApplyExpr) with(ae), with(0)); inSequence(s);
			oneOf(v).visitApplyExpr((ApplyExpr) with(ae)); inSequence(s);
			oneOf(v).visitExpr((UnresolvedVar) with(ExprMatcher.unresolved("dest")), with(1)); inSequence(s);
			oneOf(v).visitUnresolvedVar((UnresolvedVar) with(ExprMatcher.unresolved("dest")), with(1)); inSequence(s);
			oneOf(v).visitExpr(str, 0); inSequence(s);
			oneOf(v).visitStringLiteral(str); inSequence(s);
			oneOf(v).leaveApplyExpr((ApplyExpr) with(ae)); inSequence(s);
			oneOf(v).visitAssignSlot(slotName);
			oneOf(v).leaveAssignMessage(msg2); inSequence(s);
			oneOf(v).leaveMessage(msg2); inSequence(s);
			oneOf(v).leaveObjectMethod(om); inSequence(s);
			oneOf(v).leaveStandaloneMethod(sm); inSequence(s);
			oneOf(v).traversalDone();
		}});
		args.add(destP);
		dest.bind(destP);
		om.assignMessage(msg2);
		slotName.add(slotX);
		r.newStandaloneMethod(errors, sm);
		r.traverse(v);
	}

	@Test
	public void traverseTheDotOperatorWithField() {
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		ApplyExpr apply = new ApplyExpr(pos, dot, str);
		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(apply, 0); inSequence(s);
			oneOf(v).visitApplyExpr(apply); inSequence(s);
			oneOf(v).visitExpr(dot, 1); inSequence(s);
			oneOf(v).visitMemberExpr(dot); inSequence(s);
			oneOf(v).visitExpr(from, 0); inSequence(s);
			oneOf(v).visitUnresolvedVar(from, 0); inSequence(s);
			oneOf(v).visitExpr(fld, 0); inSequence(s);
			oneOf(v).visitUnresolvedVar(fld, 0); inSequence(s);
			oneOf(v).leaveMemberExpr(dot); inSequence(s);
			oneOf(v).visitExpr(str, 0); inSequence(s);
			oneOf(v).visitStringLiteral(str); inSequence(s);
			oneOf(v).leaveApplyExpr(apply); inSequence(s);
		}});
		new Traverser(v).withMemberFields().visitExpr(apply, 0);
	}

	@Test
	public void traverseTheDotOperatorWithoutField() {
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		ApplyExpr apply = new ApplyExpr(pos, dot, str);
		Sequence s = context.sequence("inorder");
		context.checking(new Expectations() {{
			oneOf(v).visitExpr(apply, 0); inSequence(s);
			oneOf(v).visitApplyExpr(apply); inSequence(s);
			oneOf(v).visitExpr(dot, 1); inSequence(s);
			oneOf(v).visitMemberExpr(dot); inSequence(s);
			oneOf(v).visitExpr(from, 0); inSequence(s);
			oneOf(v).visitUnresolvedVar(from, 0); inSequence(s);
			oneOf(v).leaveMemberExpr(dot); inSequence(s);
			oneOf(v).visitExpr(str, 0); inSequence(s);
			oneOf(v).visitStringLiteral(str); inSequence(s);
			oneOf(v).leaveApplyExpr(apply); inSequence(s);
		}});
		new Traverser(v).visitExpr(apply, 0);
	}
}
