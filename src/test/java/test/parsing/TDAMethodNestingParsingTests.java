package test.parsing;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parser.ObjectElementsConsumer;
import org.flasck.flas.parser.TDAMethodMessageParser;
import org.flasck.flas.parser.TDAObjectElementsParser;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

/** The way in which methods nest is non-obvious (but I think the same is true of guarded equations)
 * At the top (block) level is the method declaration (with or without the method keyword, depending on context)
 * Inside this are the method actions
 * Only the last of these can have a further level of nesting, which is a function scope.
 */
public class TDAMethodNestingParsingTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errorsMock = context.mock(ErrorReporter.class);
	private ObjectElementsConsumer builder = context.mock(ObjectElementsConsumer.class);
	
	@Test
	public void anObjectCtorCanHaveActionsWithNoNesting() {
		context.checking(new Expectations() {{
			allowing(errorsMock).hasErrors(); will(returnValue(false));
			oneOf(builder).addConstructor(with(any(ObjectCtor.class)));
		}});
		TDAObjectElementsParser oep = new TDAObjectElementsParser(errorsMock, builder);
		TDAMethodMessageParser nested = (TDAMethodMessageParser) oep.tryParsing(TDABasicIntroParsingTests.line("ctor testMe"));
		nested.tryParsing(TDABasicIntroParsingTests.line("<- ds.getReady"));
		nested.tryParsing(TDABasicIntroParsingTests.line("x <- 'hello'"));
	}
}
