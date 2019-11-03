package test.resolver;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.TypeBinder;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestStep;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class ResolverTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
//	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final SolidName nested = new SolidName(pkg, "Nested");
	private final FunctionName nameF = FunctionName.function(pos, nested, "f");
	private final FunctionName nameX = FunctionName.function(pos, pkg, "x");
	private final FunctionName nameY = FunctionName.function(pos, pkg, "y");
	private final FunctionDefinition fn = new FunctionDefinition(nameF, 0);
	private final TypeBinder vx = new FunctionDefinition(nameX, 0);
	private final TypeBinder vy = new FunctionDefinition(nameY, 0);
	private final FunctionName namePlPl = FunctionName.function(pos, null, "++");
	private final FunctionDefinition op = new FunctionDefinition(namePlPl, 2);
	private final StructDefn type = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(pkg, "Hello"), true, new ArrayList<>());
	private final StructDefn number = new StructDefn(pos, pos, FieldsType.STRUCT, new SolidName(null, "Number"), true, new ArrayList<>());
	private final ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "ContractName"));

	@Test
	public void testWeCanResolveASimpleName() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.f"); will(returnValue(fn));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(pkg);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 2);
		assertEquals(fn, var.defn());
	}

	@Test
	public void testWeCanResolveASimpleOperator() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("++"); will(returnValue(op));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		final UnresolvedOperator var = new UnresolvedOperator(pos, "++");
		r.visitUnresolvedOperator(var, 2);
		assertEquals(op, var.defn());
	}

	@Test
	public void testWeCannotResolveAnUndefinedOperator() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("+>>"); will(returnValue(null));
			oneOf(errors).message(pos, "cannot resolve '+>>'");
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		final UnresolvedOperator var = new UnresolvedOperator(pos, "+>>");
		r.visitUnresolvedOperator(var, 2);
	}

	@Test
	public void anUndefinedNameCantBeResolved() {
		context.checking(new Expectations() {{
			oneOf(errors).message(pos, "cannot resolve 'f'");
		}});
		Repository ry = new Repository();
		Resolver r = new RepositoryResolver(errors, ry);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 2);
	}

	@Test
	public void testWeCanResolveANameInANestedScope() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.Nested.f"); will(returnValue(fn));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(nested);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 2);
		assertEquals(fn, var.defn());
	}

	@Test
	public void testWeCannotResolveANameIfWeAreNotInTheRightScope() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.f"); will(returnValue(null));
			oneOf(ry).get("f"); will(returnValue(null));
			oneOf(errors).message(pos, "cannot resolve 'f'");
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(pkg);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 4);
	}

	@Test
	public void parentScopesWillBeExaminedIfTheDefinitionIsNotInTheCurrentScope() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.Nested.f"); will(returnValue(null));
			oneOf(ry).get("test.repo.f"); will(returnValue(fn));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(nested);
		final UnresolvedVar var = new UnresolvedVar(pos, "f");
		r.visitUnresolvedVar(var, 0);
		assertEquals(fn, var.defn());
	}

	@Test
	public void weCanResolveSomethingInsideAFunctionDefinition() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.Nested.f.x"); will(returnValue(null));
			oneOf(ry).get("test.repo.Nested.x"); will(returnValue(null));
			oneOf(ry).get("test.repo.x"); will(returnValue(vx));
		}});
		final UnresolvedVar var = new UnresolvedVar(pos, "x");
		final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
		intro.functionCase(new FunctionCaseDefn(null, var));
		fn.intro(intro);
		Resolver r = new RepositoryResolver(errors, ry);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitFunction(fn);
		assertEquals(vx, var.defn());
	}

	@Test
	public void weCanResolveAVarInAGuard() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.Nested.f.x"); will(returnValue(null));
			oneOf(ry).get("test.repo.Nested.x"); will(returnValue(null));
			oneOf(ry).get("test.repo.x"); will(returnValue(vx));
		}});
		final UnresolvedVar var = new UnresolvedVar(pos, "x");
		final FunctionIntro intro = new FunctionIntro(nameF, new ArrayList<>());
		intro.functionCase(new FunctionCaseDefn(var, new StringLiteral(pos, "hello")));
		fn.intro(intro);
		Resolver r = new RepositoryResolver(errors, ry);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitFunction(fn);
		assertEquals(vx, var.defn());
	}

	@Test
	public void weCanResolveATypeNameInAPattern() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			exactly(2).of(ry).get("++.Number"); will(returnValue(null));
			exactly(2).of(ry).get("Number"); will(returnValue(number));
		}});
		List<Pattern> patts = new ArrayList<>();
		TypeReference tl = new TypeReference(pos, "Number");
		patts.add(new TypedPattern(pos, tl, new VarName(pos, op.name(), "l")));
		TypeReference tr = new TypeReference(pos, "Number");
		patts.add(new TypedPattern(pos, tr, new VarName(pos, op.name(), "r")));
		FunctionIntro oi = new FunctionIntro(namePlPl, patts);
		op.intro(oi);
		Resolver r = new RepositoryResolver(errors, ry);
		Traverser t = new Traverser(r);
		r.currentScope(nested);
		t.visitFunction(op);
		// TODO: define number as a type ...
		assertEquals(number, tl.defn());
		assertEquals(number, tr.defn());
	}

	@Test
	public void testWeCanResolveTypeReferences() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.Hello"); will(returnValue(type));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(pkg);
		final TypeReference ty = new TypeReference(pos, "Hello");
		r.visitTypeReference(ty);
		assertEquals(type, ty.defn());
	}

	@Test
	public void testWeCanResolveVarsInsideUnitTestSteps() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.x"); will(returnValue(vx));
			oneOf(ry).get("test.repo.y"); will(returnValue(vy));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(pkg);
		UnresolvedVar x = new UnresolvedVar(pos, "x");
		UnresolvedVar y = new UnresolvedVar(pos, "y");
		final UnitTestStep uts = new UnitTestAssert(x, y);
		new Traverser(r).visitUnitTestStep(uts);
		assertEquals(vx, x.defn());
		assertEquals(vy, y.defn());
	}

	@Test
	public void testWeCanResolveArgumentTypesInAContractMethod() {
		RepositoryReader ry = context.mock(RepositoryReader.class);
		context.checking(new Expectations() {{
			oneOf(ry).get("test.repo.MyHandler"); will(returnValue(cd));
		}});
		Resolver r = new RepositoryResolver(errors, ry);
		r.currentScope(pkg);
		SolidName cname = new SolidName(pkg, "MyContract");
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.DOWN, FunctionName.contractMethod(pos, cname, "m"), new ArrayList<>());
		TypeReference tr = new TypeReference(pos, "MyHandler");
		cmd.args.add(new TypedPattern(pos, tr, new VarName(pos, op.name(), "handler")));
		new Traverser(r).visitContractMethod(cmd);
		assertEquals(cd, tr.defn());
	}

}
