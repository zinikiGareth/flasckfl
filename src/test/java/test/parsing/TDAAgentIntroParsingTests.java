package test.parsing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.PackageNamer;
import org.flasck.flas.parser.TDAIntroParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tokenizers.Tokenizable;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import flas.matchers.AgentDefnMatcher;
import flas.matchers.HandlerImplementsMatcher;

public class TDAAgentIntroParsingTests {
	interface AgentConsumer extends NamedType, TopLevelDefinitionConsumer {};
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors = context.mock(ErrorReporter.class);
	private LocalErrorTracker tracker = new LocalErrorTracker(errors);
	private TopLevelDefinitionConsumer builder = context.mock(AgentConsumer.class);
	private TopLevelNamer namer = new PackageNamer("test.pkg");

	@Test
	public void theSimplestagentCreatesAScopeEntryAndReturnsAFieldParser() {
		context.checking(new Expectations() {{
			oneOf(builder).newAgent(with(errors), with(AgentDefnMatcher.match("test.pkg.JamesBond")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("agent JamesBond"));
		assertTrue(nested instanceof TDAMultiParser);
	}

	@Test
	public void anagentCanIncludeAFunction() {
		context.checking(new Expectations() {{
			oneOf(builder).newAgent(with(tracker), with(AgentDefnMatcher.match("test.pkg.JamesBond")));
			oneOf(builder).functionDefn(with(tracker), with(any(FunctionDefinition.class)));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("agent JamesBond"));
		assertTrue(nested instanceof TDAMultiParser);
		nested.tryParsing(TDABasicIntroParsingTests.line("f = 42"));
		nested.scopeComplete(null);
	}
	
	@Test
	public void agentsCanHaveNestedHandlers() {
		context.checking(new Expectations() {{
			oneOf(builder).newAgent(with(tracker), with(AgentDefnMatcher.match("test.pkg.JamesBond")));
			oneOf(builder).newHandler(with(tracker), with(HandlerImplementsMatcher.named("test.pkg.JamesBond.Handler")));
		}});
		TDAIntroParser parser = new TDAIntroParser(tracker, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("agent JamesBond"));
		nested.tryParsing(TDABasicIntroParsingTests.line("handler Contract Handler"));
	}

	@Test
	public void thereMustBeATypeName() {
		Tokenizable toks = TDABasicIntroParsingTests.line("agent");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void theTypeNameMustBeTheValidKind() {
		Tokenizable toks = TDABasicIntroParsingTests.line("agent fred");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "invalid or missing type name");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertNotNull(nested);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void aPolymorphicAgentDefinitionCreatesTheRightScopeEntryAndReturnsAFieldParser() {
		Tokenizable toks = TDABasicIntroParsingTests.line("agent JamesBond A");
		context.checking(new Expectations() {{
			oneOf(errors).message(toks, "extra tokens at end of line");
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(toks);
		assertTrue(nested instanceof IgnoreNestedParser);
	}

	@Test
	public void agentsInPackagesHaveQualifiedNames() {
		context.checking(new Expectations() {{
			oneOf(builder).newAgent(with(errors), with(AgentDefnMatcher.match("test.pkg.InPackage")));
		}});
		TDAIntroParser parser = new TDAIntroParser(errors, namer, builder);
		TDAParsing nested = parser.tryParsing(TDABasicIntroParsingTests.line("agent InPackage"));
		assertTrue(nested instanceof TDAMultiParser);
	}
}
