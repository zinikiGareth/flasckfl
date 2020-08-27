package test.names;

import static org.junit.Assert.assertEquals;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.jvm.J;
import org.junit.Test;

public class NameTests {
	InputPosition pos = new InputPosition("", -1, 0, "");
	PackageName pkg = new PackageName("demo.ziniki");
	SolidName account = new SolidName(pkg, "Account");
	HandlerName hdlr = new HandlerName(pkg, "BaseHandler");
	FunctionName f = FunctionName.function(pos, pkg, "f");
	HandlerName fh = new HandlerName(f, "NestedHandler");
	FunctionName f1 = FunctionName.caseName(f, 1);
	FunctionName fg = FunctionName.function(pos, f1, "g");
	FunctionName acctM = FunctionName.objectMethod(pos, account, "m");
	FunctionName hdlr1 = FunctionName.handlerMethod(pos, hdlr, "q");
	FunctionName fh1 = FunctionName.handlerMethod(pos, fh, "s");

	@Test
	public void aBuiltinFunctionName() {
		FunctionName strlen = FunctionName.function(pos, null, "strlen");
		assertEquals(J.FLEVAL + ".strlen", strlen.javaName());
		assertEquals("strlen", strlen.jsName());
	}

	@Test
	public void aSimpleFunctionName() {
		assertEquals("demo.ziniki.f", f.javaName());
		assertEquals("demo.ziniki.f", f.jsName());
	}

	@Test
	public void aNestedFunctionName() {
		assertEquals("demo.ziniki.f__1_g", fg.javaName());
		assertEquals("demo.ziniki.f._1.g", fg.jsName());
	}

	@Test
	public void aSolidName() {
		assertEquals("demo.ziniki.Account", account.javaName());
		assertEquals("demo.ziniki.Account", account.jsName());
	}

	@Test
	public void aFunctionInAnObject() {
		FunctionName fn = FunctionName.function(pos, account, "f");
		assertEquals("demo.ziniki.Account.f", fn.javaName());
		assertEquals("demo.ziniki.Account.f", fn.jsName());
	}

	@Test
	public void anObjectMethod() {
		assertEquals("demo.ziniki.Account.m", acctM.javaName());
		assertEquals("demo.ziniki.Account.m", acctM.jsName());
		assertEquals("demo.ziniki.Account.prototype.m", acctM.jsPName());
	}

	@Test
	public void aFunctionInAnObjectMethod() {
		FunctionName fn = FunctionName.function(pos, acctM, "f");
		assertEquals("demo.ziniki.Account.m_f", fn.javaName());
		assertEquals("demo.ziniki.Account.prototype.m.f", fn.jsName());
		assertEquals("demo.ziniki.Account.prototype.m.f", fn.jsPName());
	}

	@Test
	public void aHandlerMethod() {
		assertEquals("demo.ziniki.BaseHandler.q", hdlr1.javaName());
		assertEquals("demo.ziniki.BaseHandler.q", hdlr1.jsName());
	}

	@Test
	public void aHandlerInObjectAcor() {
		FunctionName fn = FunctionName.function(pos, acctM, "f");
		FunctionName fc = FunctionName.caseName(fn, 1);
		HandlerName hn = new HandlerName(fc, "Nested");
		assertEquals("demo.ziniki.Account.m_f__1.Nested", hn.javaName());
		assertEquals("demo.ziniki.Account.prototype.m.f._1.Nested", hn.jsName());
	}

	@Test
	public void aFunctionInAHandlerMethod() {
		FunctionName fn = FunctionName.function(pos, hdlr1, "f");
		assertEquals("demo.ziniki.BaseHandler.q_f", fn.javaName());
		assertEquals("demo.ziniki.BaseHandler.prototype.q.f", fn.jsName());
	}

	@Test
	public void aNestedFunctionInAHandlerMethod() {
		FunctionName fn = FunctionName.function(pos, hdlr1, "f");
		FunctionName fg = FunctionName.function(pos, FunctionName.caseName(fn, 3), "g");
		assertEquals("demo.ziniki.BaseHandler.q_f__3_g", fg.javaName());
		assertEquals("demo.ziniki.BaseHandler.prototype.q.f._3.g", fg.jsName());
	}

	@Test
	public void aHandlerInAFunction() {
		assertEquals("demo.ziniki.f.NestedHandler", fh.javaName());
		assertEquals("demo.ziniki.f.NestedHandler", fh.jsName());
	}

	@Test
	public void aNestedHandlerMethod() {
		assertEquals("demo.ziniki.f.NestedHandler.s", fh1.javaName());
		assertEquals("demo.ziniki.f.NestedHandler.s", fh1.jsName());
	}

	@Test
	public void aFunctionInANestedHandlerMethod() {
		FunctionName fn = FunctionName.function(pos, fh1, "k");
		assertEquals("demo.ziniki.f.NestedHandler.s_k", fn.javaName());
		assertEquals("demo.ziniki.f.NestedHandler.prototype.s.k", fn.jsName());
	}

	@Test
	public void aNestedFunctionInANestedHandlerMethod() {
		FunctionName fn = FunctionName.function(pos, fh1, "k");
		FunctionName fg = FunctionName.function(pos, FunctionName.caseName(fn, 7), "m");
		assertEquals("demo.ziniki.f.NestedHandler.s_k__7_m", fg.javaName());
		assertEquals("demo.ziniki.f.NestedHandler.prototype.s.k._7.m", fg.jsName());
	}

	@Test
	public void aFunctionNestedInAnEventHandler() {
		CardName cn = new CardName(pkg, "Card");
		FunctionName ev = FunctionName.eventMethod(pos, cn, "event");
		FunctionName fn = FunctionName.function(pos, ev, "k");
		assertEquals("demo.ziniki.Card.event_k", fn.javaName());
		assertEquals("demo.ziniki.Card.prototype.event.k", fn.jsName());
	}
}
