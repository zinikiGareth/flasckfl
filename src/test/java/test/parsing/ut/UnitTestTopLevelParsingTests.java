package test.parsing.ut;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.parser.ut.TDAUnitTestParser;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitTestDefinitionConsumer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class UnitTestTopLevelParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelNamer namer = context.mock(TopLevelNamer.class);
	private UnitTestDefinitionConsumer builder = context.mock(UnitTestDefinitionConsumer.class);

	@Test
	public void testWeCanCreateADegenerateTestCase() {
		context.checking(new Expectations() {{
			oneOf(builder).testCase(with(UnitTestCaseMatcher.number(1).description("we can write anything here")));
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(errors, namer, builder);
		TDAParsing nested = utp.tryParsing(line("test we can write anything here"));
		assertTrue(nested instanceof TestStepParser);
	}

	@Test
	public void testItMustHaveAKeyword() {
		final Tokenizable line = line("!$%");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(errors, namer, builder);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testItMustHaveTheTestKeyword() {
		final Tokenizable line = line("foo");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "syntax error");
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(errors, namer, builder);
		TDAParsing nested = utp.tryParsing(line);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void testItMustHaveADescription() {
		final Tokenizable line = line("test");
		context.checking(new Expectations() {{
			oneOf(errors).message(line, "test case must have a description");
		}});
		TDAUnitTestParser utp = new TDAUnitTestParser(errors, namer, builder);
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
