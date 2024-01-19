package test.resolver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.FunctionTypeReference;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.SendMessage;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TupleTypeReference;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.flasck.flas.tc3.Apply;
import org.flasck.flas.tc3.PolyInstance;
import org.flasck.flas.tc3.Primitive;
import org.flasck.flas.tc3.Tuple;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.ApplyMatcher;

public class ResolverTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
//	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private InputPosition pos = new InputPosition("-", 1, 0, null, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final SolidName nested = new SolidName(pkg, "Nested");
	private final FunctionName nameF = FunctionName.function(pos, nested, "f");
	private final FunctionName nameM = FunctionName.function(pos, nested, "meth");
	private final FunctionName nameX = FunctionName.function(pos, pkg, "x");
	private final FunctionName nameY = FunctionName.function(pos, pkg, "y");
	private final FunctionDefinition fn = new FunctionDefinition(nameF, 0, null);
	private final ObjectMethod meth = new ObjectMethod(pos, nameM, new ArrayList<Pattern>(), null, null);
	private final TypeBinder vx = new FunctionDefinition(nameX, 0, null);
	private final TypeBinder vy = new FunctionDefinition(nameY, 0, null);
	private final FunctionName namePlPl = FunctionName.function(pos, null, "++");
	private final FunctionDefinition op = new FunctionDefinition(namePlPl, 2, null);
	private final StructDefn type = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "Hello"), true, new ArrayList<>());
	private final StructDefn number = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(null, "Number"), true, new ArrayList<>());
	private final ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "AContract"));
	private final ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cd.name(), "d"), new ArrayList<>(), null);
	private final ContractMethodDecl cmu = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cd.name(), "u"), new ArrayList<>(), null);
	private final ContractDecl ht = new ContractDecl(pos, pos, ContractType.HANDLER, new SolidName(pkg, "HandlerType"));
	private final RepositoryReader rr = context.mock(RepositoryReader.class);
	private final FunctionIntro intro = null;

	@Before
	public void doAttaching() {
		cd.addMethod(cmd);
		cd.addMethod(cmu);
	}
	
	@Test
	public void testWeCanResolveASimpleName() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.f"); will(returnValue(fn));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 2);
		assertEquals(fn, var.defn());
	}

	@Test
	public void testWeCanResolveASimpleOperator() {
		context.checking(new Expectations() {{
			oneOf(rr).get("++"); will(returnValue(op));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final UnresolvedOperator var = new UnresolvedOperator(pos, "++");
		r.visitUnresolvedOperator(var, 2);
		assertEquals(op, var.defn());
	}

	@Test
	public void testWeCannotResolveAnUndefinedOperator() {
		context.checking(new Expectations() {{
			oneOf(rr).get("+>>"); will(returnValue(null));
			oneOf(errors).message(pos, "cannot resolve '+>>'");
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final UnresolvedOperator var = new UnresolvedOperator(pos, "+>>");
		r.visitUnresolvedOperator(var, 2);
	}

	@Test
	public void anUndefinedNameCantBeResolved() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot resolve 'f'");
		}});
		Repository repo = new Repository();
		Resolver r = new RepositoryResolver(errors, repo);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 2);
	}

	@Test
	public void testWeCanResolveANameInANestedScope() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.f"); will(returnValue(fn));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(nested);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 2);
		assertEquals(fn, var.defn());
	}

	@Test
	public void testWeCannotResolveANameIfWeAreNotInTheRightScope() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.f"); will(returnValue(null));
			oneOf(rr).get("f"); will(returnValue(null));
			oneOf(errors).message(pos, "cannot resolve 'f'");
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 4);
	}

	@Test
	public void parentScopesWillBeExaminedIfTheDefinitionIsNotInTheCurrentScope() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.f"); will(returnValue(null));
			oneOf(rr).get("test.repo.f"); will(returnValue(fn));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(nested);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 0);
		assertEquals(fn, var.defn());
	}

	@Test
	public void weCanResolveSomethingInsideAFunctionDefinition() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.f.x"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.x"); will(returnValue(null));
			oneOf(rr).get("test.repo.x"); will(returnValue(vx));
		}});
		final UnresolvedVar var = new UnresolvedVar(pos, "x");
		final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
		intro.functionCase(new FunctionCaseDefn(pos, intro, null, var));
		fn.intro(intro);
		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitFunction(fn);
		assertEquals(vx, var.defn());
	}

	@Test
	public void weCanResolveSomethingInsideAMethodMessage() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.meth.x"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.x"); will(returnValue(null));
			oneOf(rr).get("test.repo.x"); will(returnValue(vx));
		}});
		final UnresolvedVar var = new UnresolvedVar(pos, "x");
		meth.sendMessage(new SendMessage(pos, var));
		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitObjectMethod(meth);
		assertEquals(vx, var.defn());
	}

	@Test
	public void weCanResolveAVarInAGuard() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.f.x"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.x"); will(returnValue(null));
			oneOf(rr).get("test.repo.x"); will(returnValue(vx));
		}});
		final UnresolvedVar var = new UnresolvedVar(pos, "x");
		final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
		intro.functionCase(new FunctionCaseDefn(pos, intro, var, new StringLiteral(pos, "hello")));
		fn.intro(intro);
		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitFunction(fn);
		assertEquals(vx, var.defn());
	}

	@Test
	public void weCanResolveATypeNameInAPattern() {
		context.checking(new Expectations() {{
			exactly(2).of(rr).get("++.Number"); will(returnValue(null));
			exactly(2).of(rr).get("Number"); will(returnValue(number));
		}});
		List<Pattern> patts = new ArrayList<>();
		TypeReference tl = new TypeReference(pos, "Number");
		patts.add(new TypedPattern(pos, tl, new VarName(pos, op.name(), "l")));
		TypeReference tr = new TypeReference(pos, "Number");
		patts.add(new TypedPattern(pos, tr, new VarName(pos, op.name(), "r")));
		FunctionIntro oi = new FunctionIntro(namePlPl, patts);
		op.intro(oi);
		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitFunction(op);
		// TODO: define number as a type ...
		assertEquals(number, tl.namedDefn());
		assertEquals(number, tr.namedDefn());
	}

	@Test
	public void weCanResolveATypeNameInAStructField() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.String"); will(returnValue(null));
			oneOf(rr).get("test.repo.String"); will(returnValue(null));
			oneOf(rr).get("String"); will(returnValue(LoadBuiltins.string));
		}});
		SolidName str = new SolidName(pkg, "MyStruct");
		StructDefn s = new StructDefn(pos, pos, FieldsType.STRUCT, str, true, new ArrayList<>());
		StructField fld = new StructField(pos, s, false, true, LoadBuiltins.stringTR, "fld");
		s.addField(fld);

		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitStructField(fld);
		assertEquals(LoadBuiltins.string, fld.type.namedDefn());
	}

	@Test
	public void weCanResolveAPolyTypeNameInAStructField() {
		PolyType pa = new PolyType(pos, new SolidName(null, "A"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.MyStruct.A"); will(returnValue(pa));
		}});
		TypeReference pv = new TypeReference(pos, "A");
		pv.bind(pa);
		SolidName str = new SolidName(pkg, "MyStruct");
		StructDefn s = new StructDefn(pos, pos, FieldsType.STRUCT, str, true, Arrays.asList(pa));
		StructField fld = new StructField(pos, s, false, true , pv, "fld");
		s.addField(fld);

		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(pkg);
		t.visitStructDefn(s);
		assertEquals(pa, fld.type.namedDefn());
	}

	@Test
	public void weCanResolveATupleInAPolyField() {
		StructDefn listType = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(null, "List"), true, Arrays.asList(new PolyType(pos, new SolidName(null, "A"))));
		Primitive strType = new Primitive(pos, "String");
		Primitive nbrType = new Primitive(pos, "Number");
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.MyStruct.List"); will(returnValue(null));
			oneOf(rr).get("test.repo.List"); will(returnValue(null));
			oneOf(rr).get("List"); will(returnValue(listType));
			oneOf(rr).get("test.repo.MyStruct.String"); will(returnValue(null));
			oneOf(rr).get("test.repo.String"); will(returnValue(null));
			oneOf(rr).get("String"); will(returnValue(strType));
			oneOf(rr).get("test.repo.MyStruct.Number"); will(returnValue(null));
			oneOf(rr).get("test.repo.Number"); will(returnValue(null));
			oneOf(rr).get("Number"); will(returnValue(nbrType));
			oneOf(errors).mark();
		}});
		TupleTypeReference pv = new TupleTypeReference(pos, new TypeReference(pos, "String"), new TypeReference(pos, "Number"));
		SolidName str = new SolidName(pkg, "MyStruct");
		StructDefn s = new StructDefn(pos, pos, FieldsType.STRUCT, str, true, null);
		StructField fld = new StructField(pos, s, false, true, new TypeReference(pos, "List", pv), "fld");
		s.addField(fld);

		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(pkg);
		t.visitStructDefn(s);
		assertTrue(fld.type.namedDefn() instanceof PolyInstance);
		PolyInstance pi = (PolyInstance) fld.type.namedDefn();
		assertEquals(listType, pi.struct());
		assertEquals(1, pi.polys().size());
		assertTrue(pi.polys().get(0) instanceof PolyInstance);
		PolyInstance pt = (PolyInstance) pi.polys().get(0);
		assertTrue(pt.struct() instanceof Tuple);
		assertEquals(2, pt.polys().size());
		assertEquals(strType, pt.polys().get(0));
		assertEquals(nbrType, pt.polys().get(1));
	}

	@Test
	public void weCanResolveAFunctionInAStructField() {
		Primitive strType = new Primitive(pos, "String");
		Primitive nbrType = new Primitive(pos, "Number");
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.MyStruct.String"); will(returnValue(null));
			oneOf(rr).get("test.repo.String"); will(returnValue(null));
			oneOf(rr).get("String"); will(returnValue(strType));
			oneOf(rr).get("test.repo.MyStruct.Number"); will(returnValue(null));
			oneOf(rr).get("test.repo.Number"); will(returnValue(null));
			oneOf(rr).get("Number"); will(returnValue(nbrType));
		}});
		FunctionTypeReference ft = new FunctionTypeReference(pos, new TypeReference(pos, "String"), new TypeReference(pos, "Number"));
		SolidName str = new SolidName(pkg, "MyStruct");
		StructDefn s = new StructDefn(pos, pos, FieldsType.STRUCT, str, true, null);
		StructField fld = new StructField(pos, s, false, true, ft, "fld");
		s.addField(fld);

		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(pkg);
		t.visitStructDefn(s);
		assertTrue(fld.type.applyDefn() instanceof Apply);
		Apply app = fld.type.applyDefn();
		assertEquals(1, app.argCount());
		assertEquals(strType, app.get(0));
		assertEquals(nbrType, app.get(1));
	}

	@Test
	public void weCanResolveAPolyTypeNameInAnObjectStateDecl() {
		PolyType pa = new PolyType(pos, new SolidName(null, "A"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.MyObject.A"); will(returnValue(pa));
		}});
		TypeReference pv = new TypeReference(pos, "A");
		pv.bind(pa);
		SolidName str = new SolidName(pkg, "MyObject");
		ObjectDefn od = new ObjectDefn(pos, pos, str, true, Arrays.asList(pa));
		StateDefinition sd = new StateDefinition(pos);
		StructField fld = new StructField(pos, sd, false, true , pv, "fld");
		sd.addField(fld);
		od.defineState(sd);

		Resolver r = new RepositoryResolver(errors, rr);
		Traverser t = new Traverser(r);
		r.currentScope(pkg);
		t.visitObjectDefn(od);
		assertEquals(pa, fld.type.namedDefn());
	}

	@Test
	public void testWeCanResolveTypeReferences() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Hello"); will(returnValue(type));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		final TypeReference ty = new TypeReference(pos, "Hello");
		r.visitTypeReference(ty, true, -1);
		assertEquals(type, ty.namedDefn());
	}

	@Test
	public void testWeCanResolveVarsInsideUnitTestSteps() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.x"); will(returnValue(vx));
			oneOf(rr).get("test.repo.y"); will(returnValue(vy));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		UnresolvedVar x = new UnresolvedVar(pos, "x");
		UnresolvedVar y = new UnresolvedVar(pos, "y");
		final UnitTestStep uts = new UnitTestAssert(x, y);
		new Traverser(r).visitUnitTestStep(uts);
		assertEquals(vx, x.defn());
		assertEquals(vy, y.defn());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testWeCanResolveArgumentTypesInAContractMethod() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.HandlerType"); will(returnValue(ht));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		SolidName cname = new SolidName(pkg, "MyContract");
		TypeReference tr = new TypeReference(pos, "HandlerType");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>(), new TypedPattern(pos, tr, new VarName(pos, op.name(), "handler")));
		new Traverser(r).visitContractMethod(cmd);
		assertEquals(ht, tr.namedDefn());
		assertThat(cmd.type(), (Matcher)ApplyMatcher.type(Matchers.is(ht), Matchers.is(LoadBuiltins.send)));
	}

	@Test
	public void testWeDoNotTryToResolveMemberNamesAtThisPhase() {
		UnresolvedVar from = new UnresolvedVar(pos, "obj");
		UnresolvedVar fld = new UnresolvedVar(pos, "m");
		MemberExpr dot = new MemberExpr(pos, from, fld);
		VarPattern vp = new VarPattern(pos, new VarName(pos, pkg, "obj"));
		context.checking(new Expectations() {{
			oneOf(rr).get("obj.m"); will(returnValue(null));
			oneOf(rr).get("test.repo.obj"); will(returnValue(vp));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		Traverser tr = new Traverser(r);
		tr.visitMemberExpr(dot, 0);
		assertEquals(vp, from.defn());
		assertNull(fld.defn());
	}

	@Test
	public void testAcorsCanGetHoldOfObjectStateVariables() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		StateDefinition state = new StateDefinition(pos);
		StructField fld = new StructField(pos, state, false, true, LoadBuiltins.stringTR, "fld");
		state.addField(fld);
		s.defineState(state);
		FunctionDefinition acorFn = new FunctionDefinition(FunctionName.function(pos, obj, "acor"), 2, null);
		FunctionIntro fi = new FunctionIntro(FunctionName.caseName(acorFn.name(), 1), new ArrayList<>());
		acorFn.intro(fi);
		fi.functionCase(new FunctionCaseDefn(pos, intro, null, new UnresolvedVar(pos, "fld")));
		ObjectAccessor oa = new ObjectAccessor(s, acorFn);
		s.acors.add(oa);

		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.MyObject.acor._1.fld"); will(returnValue(null));
			oneOf(rr).get("test.repo.MyObject.acor.fld"); will(returnValue(null));
			oneOf(rr).get("test.repo.MyObject.fld"); will(returnValue(fld));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		r.currentScope(pkg);
		new Traverser(r).visitObjectAccessor(oa);
	}

	@Test
	public void testWeCanResolveProvidesTypeReferencesInsideAgents() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Card.S0.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Card.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Hello"); will(returnValue(type));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final CardName card = new CardName(pkg, "Card");
		final TypeReference ty = new TypeReference(pos, "Hello");
		Provides cs = new Provides(pos, pos, null, ty, new CSName(card, "S0"));
		r.currentScope(cs.name());
		r.visitTypeReference(ty, true, 0);
		assertEquals(type, ty.namedDefn());
	}

	@Test
	public void itIsAnErrorForTheProvidedTypeNotToExist() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Card.S0.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Card.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Hello"); will(returnValue(null));
			oneOf(rr).get("Hello"); will(returnValue(null));
			oneOf(errors).message(pos, "cannot resolve 'Hello'");
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final CardName card = new CardName(pkg, "Card");
		final TypeReference ty = new TypeReference(pos, "Hello");
		Provides pr = new Provides(pos, pos, null, ty, new CSName(card, "S0"));
		r.currentScope(card);
		r.visitProvides(pr);
		r.visitTypeReference(ty, true, 0);
	}

	@Test
	public void ifTheProvidedTypeIsResolvedItMustBeAContractDecl() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Card.S0.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Card.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Hello"); will(returnValue(type));
			oneOf(errors).message(pos, "Hello is not a contract");
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final CardName card = new CardName(pkg, "Card");
		final TypeReference ty = new TypeReference(pos, "Hello");
		Provides pr = new Provides(pos, pos, null, ty, new CSName(card, "S0"));
		r.currentScope(card);
		r.visitProvides(pr);
		r.visitTypeReference(ty, true, 0);
		r.leaveProvides(pr);
	}

	@Test
	public void ifTheProvidedTypeIsResolvedItIsAttachedToTheProvides() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Card.S0.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Card.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Hello"); will(returnValue(cd));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final CardName card = new CardName(pkg, "Card");
		final TypeReference ty = new TypeReference(pos, "Hello");
		Provides pr = new Provides(pos, pos, null, ty, new CSName(card, "S0"));
		r.currentScope(card);
		r.visitProvides(pr);
		r.visitTypeReference(ty, true, 0);
		assertEquals(cd, pr.actualType());
	}

	@Test
	public void aMethodImplementingAContractHasTheCMDAttachedToItDuringResolution() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Card.S0.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Card.Hello"); will(returnValue(null));
			oneOf(rr).get("test.repo.Hello"); will(returnValue(cd));
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final CardName card = new CardName(pkg, "Card");
		final TypeReference ty = new TypeReference(pos, "Hello");
		Provides pr = new Provides(pos, pos, null, ty, new CSName(card, "S0"));
		ObjectMethod om = new ObjectMethod(pos, FunctionName.objectMethod(pos, pr.name(), "u"), new ArrayList<>(), null, null);
		pr.addImplementationMethod(om);
		r.currentScope(card);
		r.visitProvides(pr);
		r.visitTypeReference(ty, true, 0);
		r.visitObjectMethod(om);
		assertEquals(cmu, om.contractMethod());
	}

	@Test
	public void itIsAnErrorToReferenceAContractMethodWhichIsNotOnTheContract() {
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Card.S0.AContract"); will(returnValue(null));
			oneOf(rr).get("test.repo.Card.AContract"); will(returnValue(null));
			oneOf(rr).get("test.repo.AContract"); will(returnValue(cd));
			oneOf(errors).message(pos, "there is no method 'absent' on 'test.repo.AContract'");
		}});
		Resolver r = new RepositoryResolver(errors, rr);
		final CardName card = new CardName(pkg, "Card");
		final TypeReference ty = new TypeReference(pos, "AContract");
		Provides pr = new Provides(pos, pos, null, ty, new CSName(card, "S0"));
		ObjectMethod om = new ObjectMethod(pos, FunctionName.objectMethod(pos, pr.name(), "absent"), new ArrayList<>(), null, null);
		pr.addImplementationMethod(om);
		r.currentScope(card);
		r.visitProvides(pr);
		r.visitTypeReference(ty, true, 0);
		r.visitObjectMethod(om);
	}
}
