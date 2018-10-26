package test.flas.stories;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.parser.TDAParsing;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import test.parsing.TokenizableMatcher;

/* This is testing that the whole nesting thing works
 * It is not a test of any parsers.  They are tested separately
 * The TDAMultiParser should be tested separately too
 */
public class TDAStoryTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();

	@Test
	public void nothingMeansNothing() { // but should there be some kind of "reset"?
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
		}});
		new TDANester(topLevel);
	}

	@Test
	public void aSimpleLineComesThroughAndNothingNeedsToBeReturned() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
		}});
		TDANester story = new TDANester(topLevel);
		story.line(1, line("hello, world"));
	}

	@Test
	public void twoLinesAtTheSameDepthGoToTheSameParser() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("a second line")));
		}});
		TDANester story = new TDANester(topLevel);
		story.line(1, line("hello, world"));
		story.line(1, line("a second line"));
	}

	@Test
	public void anIndentedLineGoesToTheParserReturnedByTheFirstParsedLine() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		TDAParsing nested = context.mock(TDAParsing.class, "nested");
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(nested));
			oneOf(nested).tryParsing(with(TokenizableMatcher.match("a second line")));
		}});
		TDANester story = new TDANester(topLevel);
		story.line(1, line("hello, world"));
		story.line(2, line("a second line"));
	}

	@Test
	public void ifTheOuterOneDoesntReturnAParserNestedBlocksAreIgnored() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(null));
		}});
		TDANester story = new TDANester(topLevel);
		story.line(1, line("hello, world"));
		story.line(2, line("a second line"));
	}

	@Test
	public void anIndentedLineDoesntGoToAPreviouslyDefinedParserAfterWeExdent() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		TDAParsing nested = context.mock(TDAParsing.class, "nested");
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(nested));
			oneOf(nested).tryParsing(with(TokenizableMatcher.match("a second line")));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("back at top"))); will(returnValue(null));
		}});
		TDANester story = new TDANester(topLevel);
		story.line(1, line("hello, world"));
		story.line(2, line("a second line"));
		story.line(1, line("back at top"));
		story.line(2, line("will be ignored"));
	}

	@Test
	public void theSecondTopLineCanCreateADifferentNestedParser() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		TDAParsing nested = context.mock(TDAParsing.class, "nested");
		TDAParsing nested2 = context.mock(TDAParsing.class, "nested2");
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(nested));
			oneOf(nested).tryParsing(with(TokenizableMatcher.match("a second line")));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("back at top"))); will(returnValue(nested2));
			oneOf(nested2).tryParsing(with(TokenizableMatcher.match("second nesting")));
		}});
		TDANester story = new TDANester(topLevel);
		story.line(1, line("hello, world"));
		story.line(2, line("a second line"));
		story.line(1, line("back at top"));
		story.line(2, line("second nesting"));
	}

	public static ContinuedLine line(String string) {
		ContinuedLine ret = new ContinuedLine();
		ret.lines.add(new SingleLine("fred", 1, new Indent(1,0), string));
		return ret;
	}
}
