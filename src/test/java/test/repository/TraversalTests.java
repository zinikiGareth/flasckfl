package test.repository;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
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
	final UnitTestNamer namer = new UnitTestPackageNamer(pkg.uniqueName(), "file");
	final Repository r = new Repository();
	final Visitor v = context.mock(Visitor.class);

	@Test
	public void traverseStructDefn() {
		StructDefn s = new StructDefn(pos, FieldsType.STRUCT, "foo.bar", "MyStruct", true);
		r.addEntry(s.name(), s);
		context.checking(new Expectations() {{
			oneOf(v).visitStructDefn(s);
		}});
		r.traverse(v);
	}

	@Test
	public void traverseUnitTest() {
		UnitTestName name = new UnitTestName(new PackageName("foo.bar"), 1);
		UnitTestCase utc = new UnitTestCase(name, "do something");
		r.addEntry(name, utc);
		context.checking(new Expectations() {{
			oneOf(v).visitUnitTest(utc);
		}});
		r.traverse(v);
	}
}
