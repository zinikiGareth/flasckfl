package test.parsing;

import static org.junit.Assert.assertNull;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.flas.stories.TDAStoryTests;

public class TDABasicIntroParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private TopLevelDefinitionConsumer builder = context.mock(TopLevelDefinitionConsumer.class);
	// TODO: might be easier (and better) to use the real thing (a PackageNamer)
	private TopLevelNamer namer = context.mock(TopLevelNamer.class);

	@Test
	public void aBlankLineReturnsNothingAndDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(line(""));
		assertNull(nested);
	}

	@Test
	public void aLineWithoutAPlausibleKeywordDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(line("+x"));
		assertNull(nested);
	}

	@Test
	public void aLineWithAnUnrecognizedKeywordDoesNothing() {
		context.checking(new Expectations() {{
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(line("garbage"));
		assertNull(nested);
	}

	public static Tokenizable line(String string) {
		return new Tokenizable(TDAStoryTests.line(string));
	}

}
