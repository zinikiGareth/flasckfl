package test.unittests;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AccessorHolder;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataDeclaration.Assignment;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.Traverser;
import org.flasck.flas.resolver.RepositoryResolver;
import org.flasck.flas.resolver.Resolver;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class UnitDataDeclTypesTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
//	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	private final SolidName nested = new SolidName(pkg, "Nested");
	private final RepositoryReader rr = context.mock(RepositoryReader.class);
	private final Resolver r = new RepositoryResolver(errors, rr);

	@Test
	public void testThereMustBeAtLeastOneFieldAssignmentInAUTData() {
		StructDefn sd = new StructDefn(pos, FieldsType.STRUCT, "test.repo", "StructThing", false);
		sd.ctorfields.add(new StructField(pos, sd, false, LoadBuiltins.stringTR, "fld"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.ut.StructThing"); will(returnValue(sd));
			oneOf(errors).message(pos, "either an expression or at least one field assignment must be specified for test.repo.StructThing");
		}});
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "ut"), null);
		Resolver r = new RepositoryResolver(errors, rr);
		new Traverser(r).visitUnitTestStep(udd);
	}

	@Test
	public void testOneAssignmentIsEnough() {
		StructDefn sd = new StructDefn(pos, FieldsType.STRUCT, "test.repo", "StructThing", false);
		sd.ctorfields.add(new StructField(pos, sd, false, LoadBuiltins.stringTR, "fld"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.ut.StructThing"); will(returnValue(sd));
		}});
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "ut"), null);
		udd.fields.add(new Assignment(new UnresolvedVar(pos, "x"), new StringLiteral(pos, "hello")));
		Resolver r = new RepositoryResolver(errors, rr);
		new Traverser(r).visitUnitTestStep(udd);
	}

	@Test
	public void noFieldsAreNeededIfThereIsAnAssignment() {
		StructDefn sd = new StructDefn(pos, FieldsType.STRUCT, "test.repo", "StructThing", false);
		sd.ctorfields.add(new StructField(pos, sd, false, LoadBuiltins.stringTR, "fld"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.ut.StructThing"); will(returnValue(sd));
		}});
		TypeReference tr = new TypeReference(pos, "StructThing");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, tr, FunctionName.function(pos, pkg, "ut"), new StringLiteral(pos, "foo"));
		Resolver r = new RepositoryResolver(errors, rr);
		new Traverser(r).visitUnitTestStep(udd);
	}

	@Test
	public void aContractCanBeInstantiatedWithoutAnyFuss() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Contract"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.udd.Contract"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.Contract"); will(returnValue(cd));
		}});
		TypeReference ctr = new TypeReference(pos, "Contract");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, nested, "udd"), null);
		r.visitUnitDataDeclaration(udd);
		r.visitTypeReference(ctr);
		r.leaveUnitDataDeclaration(udd);
	}

	@Test
	public void aContractMayNotBeGivenAnExpression() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Contract"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.udd.Contract"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.Contract"); will(returnValue(cd));
			oneOf(errors).message(pos, "a contract data declaration may not be initialized");
		}});
		TypeReference ctr = new TypeReference(pos, "Contract");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, nested, "udd"), new StringLiteral(pos, "hello"));
		r.visitUnitDataDeclaration(udd);
		r.visitTypeReference(ctr);
		r.leaveUnitDataDeclaration(udd);
	}

	@Test
	public void aContractMayNotBeAssignedFields() {
		ContractDecl cd = new ContractDecl(pos, pos, ContractType.CONTRACT, new SolidName(pkg, "Contract"));
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.udd.Contract"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.Contract"); will(returnValue(cd));
			oneOf(errors).message(pos, "a contract data declaration may not be initialized");
		}});
		TypeReference ctr = new TypeReference(pos, "Contract");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, nested, "udd"), null);
		udd.field(new UnresolvedVar(pos, "x"), new StringLiteral(pos, "hello"));
		r.visitUnitDataDeclaration(udd);
		r.visitTypeReference(ctr);
		r.leaveUnitDataDeclaration(udd);
	}

	@Test
	public void aObjectCanBeInstantiatedWithAnExpression() {
		AccessorHolder od = new ObjectDefn(pos, pos, new SolidName(pkg, "Obj"), false, new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(rr).get("test.repo.Nested.udd.Obj"); will(returnValue(null));
			oneOf(rr).get("test.repo.Nested.Obj"); will(returnValue(od));
		}});
		TypeReference ctr = new TypeReference(pos, "Obj");
		UnresolvedVar from = new UnresolvedVar(pos, "Obj");
		UnitDataDeclaration udd = new UnitDataDeclaration(pos, false, ctr, FunctionName.function(pos, nested, "udd"), new MemberExpr(pos, from, new UnresolvedVar(pos, "fld")));
		r.visitUnitDataDeclaration(udd);
		r.visitTypeReference(ctr);
		r.leaveUnitDataDeclaration(udd);
	}

}
