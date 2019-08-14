package test.repository;

import java.util.ArrayList;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructField;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parsedForm.ut.UnitTestPackage;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.parser.ut.UnitTestPackageNamer;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.Repository.Visitor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TraversalTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private InputPosition pos = new InputPosition("-", 1, 0, "hello");
	private final PackageName pkg = new PackageName("test.repo");
	final StringLiteral simpleExpr = new StringLiteral(pos, "hello");
	final UnitTestNamer namer = new UnitTestPackageNamer(new UnitTestFileName(pkg, "file"));
	final Repository r = new Repository();
	final Visitor v = context.mock(Visitor.class);

	@Test
	public void traverseStructDefn() {
		StructDefn s = new StructDefn(pos, FieldsType.STRUCT, "foo.bar", "MyStruct", true);
		r.addEntry(s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
			oneOf(v).leaveStructDefn(s);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseStructDefnWithFieldVisitsTheField() {
		StructDefn s = new StructDefn(pos, FieldsType.STRUCT, "foo.bar", "MyStruct", true);
		StructField sf = new StructField(pos, false, new TypeReference(pos, "X", new ArrayList<>()), "x");
		s.addField(sf);
		r.addEntry(s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
			oneOf(v).visitStructField(sf);
			oneOf(v).leaveStructDefn(s);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectDefn() {
		ObjectDefn s = new ObjectDefn(pos, pos, new SolidName(pkg, "MyObject"), true, new ArrayList<>());
		r.addEntry(s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectDefn(s);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectWithMethodsDoesNotDirectlyVisitTheMethod() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>());
		s.methods.add(meth);
		r.addEntry(s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectDefn(s);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseObjectMethodFromTheRepository() {
		SolidName obj = new SolidName(pkg, "MyObject");
		ObjectDefn s = new ObjectDefn(pos, pos, obj, true, new ArrayList<>());
		ObjectMethod meth = new ObjectMethod(pos, FunctionName.objectMethod(pos, obj, "meth"), new ArrayList<>());
		s.methods.add(meth);
		r.addEntry(meth.name(), meth);
		context.checking(new Expectations() {{
			oneOf(v).visitObjectMethod(meth);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseContract() {
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Contr"));
		r.addEntry(cd.nameAsName(), cd);
		context.checking(new Expectations() {{
			oneOf(v).visitContractDecl(cd);
			oneOf(v).leaveContractDecl(cd);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseContractWithMethods() {
		ContractDecl cd = new ContractDecl(pos, pos, new SolidName(pkg, "Contr"));
		ContractMethodDecl cmd = new ContractMethodDecl(pos, pos, pos, true, ContractMethodDir.UP, FunctionName.contractMethod(pos, cd.nameAsName(), "meth"), new ArrayList<>());
		cd.addMethod(cmd);
		r.addEntry(cd.nameAsName(), cd);
		context.checking(new Expectations() {{
			oneOf(v).visitContractDecl(cd);
			oneOf(v).visitContractMethod(cmd);
			oneOf(v).leaveContractDecl(cd);
		}});
		r.traverse(v);
	}

	// TODO: method with arguments
	
	@Test
	public void traverseUnitTest() {
		UnitTestFileName utfn = new UnitTestFileName(new PackageName("foo.bar"), "file");
		UnitTestName name = new UnitTestName(utfn, 1);
		UnitTestPackage utp = new UnitTestPackage(utfn);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		utp.testCase(utc);
		UnitTestAssert uta = new UnitTestAssert(null, null);
		utc.steps.add(uta);
		r.addEntry(name, utp);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitTestPackage(utp);
			oneOf(v).visitUnitTest(utc);
			oneOf(v).visitUnitTestStep(uta);
			oneOf(v).visitUnitTestAssert(uta);
			oneOf(v).postUnitTestAssert(uta);
			oneOf(v).leaveUnitTest(utc);
			oneOf(v).leaveUnitTestPackage(utp);
		}});
		r.traverse(v);
	}
}
