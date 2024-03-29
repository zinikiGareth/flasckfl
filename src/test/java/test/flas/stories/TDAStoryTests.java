package test.flas.stories;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDANester;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.testsupport.TestSupport;
import org.flasck.flas.testsupport.matchers.TokenizableMatcher;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

/* This is testing that the whole nesting thing works
 * It is not a test of any parsers.  They are tested separately
 * The TDAMultiParser should be tested separately too
 */
public class TDAStoryTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	ErrorReporter errors = context.mock(ErrorReporter.class);

	@Test
	public void nothingMeansNothing() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).scopeComplete(null);
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.flush();
	}

	@Test
	public void aSimpleLineComesThroughAndNothingNeedsToBeReturned() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.flush();
	}

	@Test
	public void twoLinesAtTheSameDepthGoToTheSameParser() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("a second line")));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.line(1, TestSupport.line("a second line"));
		story.flush();
	}

	@Test
	public void anIndentedLineGoesToTheParserReturnedByTheFirstParsedLine() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		TDAParsing nested = context.mock(TDAParsing.class, "nested");
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(nested));
			oneOf(nested).tryParsing(with(TokenizableMatcher.match("a second line")));
			oneOf(nested).scopeComplete(with(any(InputPosition.class)));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.line(2, TestSupport.line("a second line"));
		story.flush();
	}

	@Test
	public void ifTheOuterOneDoesntReturnAParserNestedBlocksAreIgnored() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(null));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.line(2, TestSupport.line("a second line"));
		story.flush();
	}

	@Test
	public void anIndentedLineDoesntGoToAPreviouslyDefinedParserAfterWeExdent() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		TDAParsing nested = context.mock(TDAParsing.class, "nested");
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(nested));
			oneOf(nested).tryParsing(with(TokenizableMatcher.match("a second line")));
			oneOf(nested).scopeComplete(with(any(InputPosition.class)));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("back at top"))); will(returnValue(null));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.line(2, TestSupport.line("a second line"));
		story.line(1, TestSupport.line("back at top"));
		story.line(2, TestSupport.line("will be ignored"));
		story.flush();
	}

	@Test
	public void theSecondTopLineCanCreateADifferentNestedParser() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		TDAParsing nested = context.mock(TDAParsing.class, "nested");
		TDAParsing nested2 = context.mock(TDAParsing.class, "nested2");
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world"))); will(returnValue(nested));
			oneOf(nested).tryParsing(with(TokenizableMatcher.match("a second line")));
			oneOf(nested).scopeComplete(with(any(InputPosition.class)));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("back at top"))); will(returnValue(nested2));
			oneOf(nested2).tryParsing(with(TokenizableMatcher.match("second nesting")));
			oneOf(nested2).scopeComplete(with(any(InputPosition.class)));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.line(2, TestSupport.line("a second line"));
		story.line(1, TestSupport.line("back at top"));
		story.line(2, TestSupport.line("second nesting"));
		story.flush();
	}

	@Test
	public void multipleFilesCanBeProcessed() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.flush();
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.flush();
	}

	@Test
	public void aSubsequentFileCanBeEmpty() {
		TDAParsing topLevel = context.mock(TDAParsing.class);
		context.checking(new Expectations() {{
			oneOf(topLevel).tryParsing(with(TokenizableMatcher.match("hello, world")));
			oneOf(topLevel).scopeComplete(with(any(InputPosition.class)));
			oneOf(topLevel).scopeComplete(null);
		}});
		TDANester story = new TDANester(errors, topLevel);
		story.newFile();
		story.line(1, TestSupport.line("hello, world"));
		story.flush();
		story.newFile();
		story.flush();
	}
}
