package test.parsing.ut;

import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.ut.FreeTextParser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class FreeTextParserTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	@SuppressWarnings("unchecked")
	private Consumer<String> handler = context.mock(Consumer.class);
	private InputPosition pos = new InputPosition("fred", 10, 0, null, "hello");

	@Test
	public void aSimpleOneLiner() {
		context.checking(new Expectations() {{
			oneOf(handler).accept("hello");
		}});
		FreeTextParser p = new FreeTextParser(tracker, handler);
		p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		p.scopeComplete(pos);
	}

	@Test
	public void twoLinesSameIndentation() {
		context.checking(new Expectations() {{
			oneOf(handler).accept("hello world");
		}});
		FreeTextParser p = new FreeTextParser(tracker, handler);
		p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		p.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		p.scopeComplete(pos);
	}

	@Test
	public void twoLinesSecondIndented() {
		context.checking(new Expectations() {{
			oneOf(handler).accept("hello world");
		}});
		FreeTextParser p = new FreeTextParser(tracker, handler);
		TDAParsing indented = p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		assertTrue(indented instanceof FreeTextParser);
		indented.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		indented.scopeComplete(pos);
		p.scopeComplete(pos);
	}

	@Test
	public void threeLinesSecondThirdIndented() {
		context.checking(new Expectations() {{
			oneOf(handler).accept("hello there world");
		}});
		FreeTextParser p = new FreeTextParser(tracker, handler);
		TDAParsing indented = p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		assertTrue(indented instanceof FreeTextParser);
		indented.tryParsing(UnitTestTopLevelParsingTests.line("there"));
		indented.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		indented.scopeComplete(pos);
		p.scopeComplete(pos);
	}

	@Test
	public void threeLinesSecondIndentedThirdNot() {
		context.checking(new Expectations() {{
			oneOf(handler).accept("hello there world");
		}});
		FreeTextParser p = new FreeTextParser(tracker, handler);
		TDAParsing indented = p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		assertTrue(indented instanceof FreeTextParser);
		indented.tryParsing(UnitTestTopLevelParsingTests.line("there"));
		indented.scopeComplete(pos);
		p.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		p.scopeComplete(pos);
	}
}
