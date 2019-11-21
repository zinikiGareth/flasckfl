package test.methods;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.method.MemberExprConvertor;
import org.flasck.flas.method.MessageConvertor;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Traverser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class MessageConversion {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private NestedVisitor nv = context.mock(NestedVisitor.class);
	private InputPosition pos = new InputPosition("-", 1, 0, null);
//	private final PackageName pkg = new PackageName("test.repo");
	
	@Test
	public void messageConvertorPushesMemberConvertor() {
		UnresolvedVar from = new UnresolvedVar(pos, "from");
		UnresolvedVar fld = new UnresolvedVar(pos, "fld");
		MemberExpr me = new MemberExpr(pos, from, fld);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(MemberExprConvertor.class)));
		}});
		MessageConvertor mc = new MessageConvertor(nv);
		mc.visitExpr(me, 0);
	}

	@Test
	public void conversionIsStoredOnUTIOnLeave() {
		StringLiteral sl = new StringLiteral(pos, "hello");
		context.checking(new Expectations() {{
			oneOf(nv).result(null);
		}});
		UnitTestInvoke uti = new UnitTestInvoke(sl);
		MessageConvertor mc = new MessageConvertor(nv);
		Traverser gen = new Traverser(mc);
		gen.visitExpr(sl, 0);
		gen.leaveUnitTestInvoke(uti);
		assertTrue(uti.isConverted());
		assertEquals(sl, uti.converted());
	}
	
	// Everything else here just asserts that it's a pass through
	@Test
	public void stringLiteralIsPassedThrough() {
		StringLiteral sl = new StringLiteral(pos, "hello");
		context.checking(new Expectations() {{
			oneOf(nv).result(sl);
		}});
		MessageConvertor mc = new MessageConvertor(nv);
		Traverser gen = new Traverser(mc);
		gen.visitExpr(sl, 0);
		gen.leaveMessage(null);
	}
	
	@Test
	public void numericLiteralIsPassedThrough() {
		NumericLiteral nl = new NumericLiteral(pos, "25", 2);
		context.checking(new Expectations() {{
			oneOf(nv).result(nl);
		}});
		MessageConvertor mc = new MessageConvertor(nv);
		Traverser gen = new Traverser(mc);
		gen.visitExpr(nl, 0);
		gen.leaveMessage(null);
	}

	@Test
	public void unresolvedVarIsPassedThroughWithoutComment() {
		UnresolvedVar uv = new UnresolvedVar(pos, "data");
		context.checking(new Expectations() {{
			oneOf(nv).result(uv);
		}});
		MessageConvertor mc = new MessageConvertor(nv);
		Traverser gen = new Traverser(mc);
		gen.visitExpr(uv, 0);
		gen.leaveMessage(null);
	}

	@Test
	public void unresolvedOpIsPassedThroughWithoutComment() {
		UnresolvedOperator op = new UnresolvedOperator(pos, "++");
		context.checking(new Expectations() {{
			oneOf(nv).result(op);
		}});
		MessageConvertor mc = new MessageConvertor(nv);
		Traverser gen = new Traverser(mc);
		gen.visitExpr(op, 0);
		gen.leaveMessage(null);
	}

	@Test
	public void applyExprPushesAnotherConvertor() {
		UnresolvedVar f = new UnresolvedVar(pos, "from");
		NumericLiteral nl = new NumericLiteral(pos, "85", 2);
		ApplyExpr ae = new ApplyExpr(pos, f, nl);
		context.checking(new Expectations() {{
			oneOf(nv).push(with(any(MessageConvertor.class)));
		}});
		MessageConvertor mc = new MessageConvertor(nv);
		mc.visitExpr(ae, 0);
	}
}
