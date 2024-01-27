package test.parsing.ut;

import static org.junit.Assert.assertTrue;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.LocalErrorTracker;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.parser.LocatableConsumer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.ut.FreeTextParser;
import org.flasck.flas.testsupport.matchers.FreeTextTokenMatcher;
import org.flasck.flas.tokenizers.FreeTextToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

public class FreeTextParserTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	@SuppressWarnings("unchecked")
	private LocatableConsumer<FreeTextToken> handler = context.mock(LocatableConsumer.class);
	private InputPosition pos = new InputPosition("fred", 1, 0, null, "hello");
	private KeywordToken kw = new KeywordToken(pos, "match", 5);

	@Before
	public void setup() {
		context.checking(new Expectations() {{
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
		}});
	}

	@Test
	public void aSimpleOneLiner() {
		context.checking(new Expectations() {{
			oneOf(handler).accept(with(pos), with(FreeTextTokenMatcher.text("hello")));
		}});
		FreeTextParser p = new FreeTextParser(kw, tracker, handler);
		p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		p.scopeComplete(pos);
	}

	@Test
	public void twoLinesSameIndentation() {
		context.checking(new Expectations() {{
			oneOf(handler).accept(with(pos), with(FreeTextTokenMatcher.text("hello world")));
		}});
		FreeTextParser p = new FreeTextParser(kw, tracker, handler);
		p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		p.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		p.scopeComplete(pos);
	}

	@Test
	public void twoLinesSecondIndented() {
		context.checking(new Expectations() {{
			oneOf(handler).accept(with(pos), with(FreeTextTokenMatcher.text("hello world")));
		}});
		FreeTextParser p = new FreeTextParser(kw, tracker, handler);
		TDAParsing indented = p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		assertTrue(indented instanceof FreeTextParser);
		indented.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		indented.scopeComplete(pos);
		p.scopeComplete(pos);
	}

	@Test
	public void threeLinesSecondThirdIndented() {
		context.checking(new Expectations() {{
			oneOf(handler).accept(with(pos), with(FreeTextTokenMatcher.text("hello there world")));
		}});
		FreeTextParser p = new FreeTextParser(kw, tracker, handler);
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
			oneOf(handler).accept(with(pos), with(FreeTextTokenMatcher.text("hello there world")));
		}});
		FreeTextParser p = new FreeTextParser(kw, tracker, handler);
		TDAParsing indented = p.tryParsing(UnitTestTopLevelParsingTests.line("hello"));
		assertTrue(indented instanceof FreeTextParser);
		indented.tryParsing(UnitTestTopLevelParsingTests.line("there"));
		indented.scopeComplete(pos);
		p.tryParsing(UnitTestTopLevelParsingTests.line("world"));
		p.scopeComplete(pos);
	}
}
