package test.parsing.ut;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.UnitTestFileName;
import org.flasck.flas.commonBase.names.UnitTestName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.ut.TDAProcessFieldsParser;
import org.flasck.flas.parser.ut.TDAUnitTestParser;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.parser.ut.UnitDataFieldConsumer;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.parser.ut.UnitTestNamer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.CaptureAction;

import flas.matchers.ExprMatcher;
import flas.matchers.UnitTestCaseMatcher;
import test.parsing.LocalErrorTracker;

public class UnitTestTopLevelParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private UnitTestNamer namer = context.mock(UnitTestNamer.class);
	private UnitTestDefinitionConsumer builder = context.mock(UnitTestDefinitionConsumer.class);
	private UnitDataFieldConsumer udc = context.mock(UnitDataFieldConsumer.class);
	private final PackageName pkg = new PackageName("test.pkg._ut_file");
	private InputPosition pos = new InputPosition("fred", 10, 0, "hello");
	private FunctionScopeUnitConsumer topLevel = context.mock(FunctionScopeUnitConsumer.class);

	@Test
	public void testWeCanCreateATestCase() {
		UnitTestFileName utfn = new UnitTestFileName(pkg, "foo");
		context.checking(new Expectations() {{
			oneOf(namer).unitTest(); will(returnValue(new UnitTestName(utfn, 4)));
			oneOf(builder).testCase(with(UnitTestCaseMatcher.number(1).description("we can write anything here")));
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line("test we can write anything here"));
		assertTrue(nested instanceof TestStepParser);
	}

	@Test
	public void testWeCanCreateATopLevelDataDeclarationWithAnExpression() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(namer).dataName(with(any(InputPosition.class)), with("x")); will(returnValue(FunctionName.function(pos, pkg, "x")));
			oneOf(builder).data(with(any(UnitDataDeclaration.class))); will(captureIt);
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line("data Number x <- 86"));
		assertTrue(nested instanceof NoNestingParser);
		
		UnitDataDeclaration data = (UnitDataDeclaration) captureIt.get(0);
		assertEquals("test.pkg._ut_file.x", data.name.uniqueName());
		assertEquals("Number", data.ofType.name());
		assertThat(data.expr, ExprMatcher.number(86));
	}

	@Test
	public void testWeCanCreateATopLevelDataDeclarationWithFields() {
		CaptureAction captureIt = new CaptureAction(null);
		context.checking(new Expectations() {{
			oneOf(namer).dataName(with(any(InputPosition.class)), with("card")); will(returnValue(FunctionName.function(pos, pkg, "card")));
			oneOf(builder).data(with(any(UnitDataDeclaration.class))); will(captureIt);
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line("data SomeCard card"));
		assertTrue(nested instanceof TDAProcessFieldsParser);
		
		UnitDataDeclaration data = (UnitDataDeclaration) captureIt.get(0);
		assertEquals("test.pkg._ut_file.card", data.name.uniqueName());
		assertEquals("SomeCard", data.ofType.name());
		assertNull(data.expr);
	}

	@Test
	public void testWeCanProcessAFieldAssignment() {
		context.checking(new Expectations() {{
			oneOf(udc).field((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(ExprMatcher.number(86)));
		}});
		TDAProcessFieldsParser utp = new TDAProcessFieldsParser(tracker, udc);
		TDAParsing nested = utp.tryParsing(line("x <- 86"));
		assertTrue(nested instanceof NoNestingParser);
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}

	@Test
	public void testWeCanProcessMultipleFieldAssignments() {
		context.checking(new Expectations() {{
			oneOf(udc).field((UnresolvedVar) with(ExprMatcher.unresolved("x")), with(ExprMatcher.number(86)));
			oneOf(udc).field((UnresolvedVar) with(ExprMatcher.unresolved("y")), with(ExprMatcher.string("hello")));
		}});
		TDAProcessFieldsParser utp = new TDAProcessFieldsParser(tracker, udc);
		TDAParsing nested = utp.tryParsing(line("x <- 86"));
		assertTrue(nested instanceof NoNestingParser);
		utp.tryParsing(line("y <- 'hello'"));
		nested.scopeComplete(pos);
		utp.scopeComplete(pos);
	}

	@Test
	public void testItMustHaveAKeyword() {
		final Tokenizable line = line("!$%");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testItMustHaveTheTestKeyword() {
		final Tokenizable line = line("foo");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testItMustHaveADescription() {
		final Tokenizable line = line("test");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "test case must have a description");
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(tracker, namer, builder, topLevel);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(cline(string));
	}

	public static ContinuedLine cline(String string) {
		ContinuedLine ret = new ContinuedLine();
		ret.lines.add(new SingleLine("fred", 1, new Indent(1,0), string));
		return ret;
	}
}
