package org.flasck.flas.rewriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.LocatedToken;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.AbsoluteVar;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.CardMember;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.EventCaseDefn;
import org.flasck.flas.parsedForm.EventHandlerDefinition;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LocalVar;
import org.flasck.flas.parsedForm.MethodCaseDefn;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StringLiteral;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.stories.Builtin;
import org.flasck.flas.vcode.hsieForm.HSIEForm.Type;
import org.junit.Before;
import org.junit.Test;
import org.zinutils.collections.CollectionUtils;

public class RewriterTests {
	private final ErrorResult errors = new ErrorResult();
	private final Rewriter rw = new Rewriter(errors);
	private Scope scope;
	private Scope builtinScope;
	private ScopeEntry pkgEntry;
	
	@Before
	public void setup() {
		builtinScope = Builtin.builtinScope();
		builtinScope.define("Timer", "Timer", null);
		PackageDefn pd = new PackageDefn(builtinScope, "ME");
		scope = pd.innerScope();
		pkgEntry = pd.innerScope().outerEntry;
	}

	@Test
	public void testRewritingSomethingGloballyDefined() {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		cases.add(new FunctionCaseDefn(scope, "ME.f", new ArrayList<Object>(), new UnresolvedVar(null, "Nil")));
		FunctionDefinition fn = new FunctionDefinition(Type.FUNCTION, "ME.f", 0, cases);
		scope.define("f", "ME.f", fn);
		rw.rewrite(pkgEntry);
		fn = rw.functions.get("ME.f");
		assertEquals("ME.f", fn.name);
		assertTrue(fn.cases.get(0).expr instanceof AbsoluteVar);
		assertEquals("Nil", ((AbsoluteVar)fn.cases.get(0).expr).id);
	}

	@Test
	public void testRewritingAParameter() {
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(new VarPattern("x"));
		cases.add(new FunctionCaseDefn(scope, "ME.f", args, new UnresolvedVar(null, "x")));
		FunctionDefinition fn = new FunctionDefinition(Type.FUNCTION, "ME.f", 1, cases);
		scope.define("f", "ME.f", fn);
		rw.rewrite(pkgEntry);
		fn = rw.functions.get("ME.f");
		assertEquals("ME.f", fn.name);
		assertTrue(fn.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)fn.cases.get(0).expr).var);
	}
	
	@Test
	public void testRewritingANestedParameter() {
		Scope innerScope;
		{
			List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern("x"));
			cases.add(new FunctionCaseDefn(scope, "ME.f", args, new StringLiteral("x")));
			FunctionDefinition fn = new FunctionDefinition(Type.FUNCTION, "ME.f", 1, cases);
			scope.define("f", "ME.f", fn);
			innerScope = cases.get(0).innerScope();
		}
		{
			List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
			ArrayList<Object> args = new ArrayList<Object>();
			args.add(new VarPattern("y"));
			cases.add(new FunctionCaseDefn(scope, "ME.f_0.g", args, new UnresolvedVar(null, "x")));
			FunctionDefinition fn = new FunctionDefinition(Type.FUNCTION, "ME.f_0.g", 1, cases);
			innerScope.define("g", "ME.f_0.g", fn);
		}
		rw.rewrite(pkgEntry);
		FunctionDefinition g = rw.functions.get("ME.f_0.g");
		System.out.println(rw.functions);
		assertEquals("ME.f_0.g", g.name);
		assertTrue(g.cases.get(0).expr instanceof LocalVar);
		assertEquals("x", ((LocalVar)g.cases.get(0).expr).var);
	}
	
	@Test
	public void testRewritingAStateVar() throws Exception {
		CardDefinition cd = new CardDefinition(scope, "MyCard");
		cd.state = new StateDefinition();
		cd.state.fields.add(new StructField(new TypeReference(null, "Number", null), "counter"));
//		scope.define("MyCard", "ME.MyCard", cd);
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		cases.add(new FunctionCaseDefn(scope, "ME.MyCard.f", new ArrayList<Object>(), new UnresolvedVar(null, "counter")));
		FunctionDefinition fn = new FunctionDefinition(Type.FUNCTION, "ME.MyCard.f", 0, cases);
		cd.fnScope.define("f", "ME.MyCard.f", fn);
		rw.rewrite(pkgEntry);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		fn = rw.functions.get("ME.MyCard.f");
		assertEquals("ME.MyCard.f", fn.name);
		assertTrue(fn.cases.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)fn.cases.get(0).expr).var);
	}

	@Test
	public void testRewritingAContractVar() throws Exception {
		CardDefinition cd = new CardDefinition(scope, "MyCard");
		// TODO: I would have expected this to complain that it can't find the referenced contract
		cd.contracts.add(new ContractImplements(null, "Timer", null, "timer"));
//		scope.define("MyCard", "ME.MyCard", cd);
		List<FunctionCaseDefn> cases = new ArrayList<FunctionCaseDefn>();
		cases.add(new FunctionCaseDefn(scope, "ME.MyCard.f", new ArrayList<Object>(), new UnresolvedVar(null, "timer")));
		FunctionDefinition fn = new FunctionDefinition(Type.FUNCTION, "ME.MyCard.f", 0, cases);
		cd.fnScope.define("f", "ME.MyCard.f", fn);
		rw.rewrite(pkgEntry);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		fn = rw.functions.get("ME.MyCard.f");
		assertEquals("ME.MyCard.f", fn.name);
		assertTrue(fn.cases.get(0).expr instanceof CardMember);
		assertEquals("timer", ((CardMember)fn.cases.get(0).expr).var);
	}

	@Test
	public void testRewritingAContractMethod() throws Exception {
		CardDefinition cd = new CardDefinition(scope, "MyCard");
		cd.state = new StateDefinition();
		cd.state.fields.add(new StructField(new TypeReference(null, "Number", null), "counter"));
		// TODO: I would have expected this to complain that it can't find the referenced contract
		ContractImplements ci = new ContractImplements(null, "Timer", null, "timer");
		cd.contracts.add(ci);
		List<MethodCaseDefn> mcds = new ArrayList<MethodCaseDefn>();
		MethodDefinition md = new MethodDefinition(new FunctionIntro("ME.MyCard._C0.m", new ArrayList<Object>()), mcds);
		MethodCaseDefn mcd1 = new MethodCaseDefn(md.intro);
		mcds.add(mcd1);
		mcd1.messages.add(new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "counter")), new UnresolvedVar(null, "counter")));
		ci.methods.add(md);
//		scope.define("MyCard", "ME.MyCard", cd);
		rw.rewrite(pkgEntry);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		md = rw.methods.get(0).method;
		assertEquals("ME.MyCard._C0.m", md.intro.name);
		assertTrue(md.cases.get(0).messages.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)md.cases.get(0).messages.get(0).expr).var);
	}

	// Handler with lambda

	@Test
	public void testRewritingAnEventHandler() throws Exception {
		CardDefinition cd = new CardDefinition(scope, "MyCard");
		cd.state = new StateDefinition();
		cd.state.fields.add(new StructField(new TypeReference(null, "Number", null), "counter"));
		// TODO: I would have expected this to complain that it can't find the referenced contract
		List<EventCaseDefn> ecds = new ArrayList<EventCaseDefn>();
		EventHandlerDefinition ehd = new EventHandlerDefinition(new FunctionIntro("ME.MyCard.eh", new ArrayList<Object>()), ecds);
		EventCaseDefn ecd1 = new EventCaseDefn(ehd.intro);
		ecds.add(ecd1);
		ecd1.messages.add(new MethodMessage(CollectionUtils.listOf(new LocatedToken(null, "counter")), new UnresolvedVar(null, "counter")));
		cd.fnScope.define("eh", "ME.MyCard.eh", ehd);
//		scope.define("MyCard", "ME.MyCard", cd);
		rw.rewrite(pkgEntry);
		errors.showTo(new PrintWriter(System.out), 0);
		assertFalse(errors.hasErrors());
		ehd = (EventHandlerDefinition) rw.eventHandlers.get(0).handler;
		assertEquals("ME.MyCard.eh", ehd.intro.name);
		assertTrue(ehd.cases.get(0).messages.get(0).expr instanceof CardMember);
		assertEquals("counter", ((CardMember)ehd.cases.get(0).messages.get(0).expr).var);
	}
}
