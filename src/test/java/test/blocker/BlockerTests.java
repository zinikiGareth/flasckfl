package test.blocker;

import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.BlockConsumer;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.support.jmock.ReturnInvoker;

import flas.matchers.LineMatcher;

public class BlockerTests {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	private ErrorReporter errors;
	private BlockConsumer consumer;
	private Blocker blocker;
	private int lineNo = 1;
	private InputPosition pos = new InputPosition("-", lineNo, 0, new Indent(1, 0), "at");
	private InputPosition pos3 = new InputPosition("-", 3, 0, new Indent(1, 0), "at");
	private InputPosition pos5 = new InputPosition("-", 5, 0, new Indent(1, 0), "at");

	@Before
	public void setup() {
		errors = context.mock(ErrorReporter.class);
		consumer = context.mock(BlockConsumer.class);
		context.checking(new Expectations() {{
			allowing(consumer).flush();
			allowing(errors).logParsingToken(with(any(LoggableToken.class))); will(ReturnInvoker.arg(0));
		}});
		blocker = new Blocker(errors, consumer);
	}
	
	@Test
	public void anEmptyFileProducesNoOutut() {
		context.checking(new Expectations() {{
		}});
		blocker.flush();
	}

	@Test
	public void oneLineComesAcrossCorrectly() {
		context.checking(new Expectations() {{
			oneOf(consumer).line(with(1), with(LineMatcher.match("package org.ziniki")));
		}});
		line("\tpackage org.ziniki");
		blocker.flush();
	}

	@Test
	public void oneLineNestedInsideAnotherIsHandledCorrectly() {
		context.checking(new Expectations() {{
			oneOf(consumer).line(with(1), with(LineMatcher.match("package org.ziniki")));
			oneOf(consumer).line(with(2), with(LineMatcher.match("fib 1 = 1")));
		}});
		line("\tpackage org.ziniki");
		line("\t\tfib 1 = 1");
		blocker.flush();
	}

	@Test
	public void aLineWithSpacesIsContinued() {
		context.checking(new Expectations() {{
			oneOf(consumer).line(with(1), with(LineMatcher.match("start\ncontinue")));
		}});
		line("\tstart");
		line("\t  continue");
		blocker.flush();
	}
	
	@Test
	public void testCommentsDontGetLost() {
		context.checking(new Expectations() {{
			oneOf(consumer).comment(pos, "hello");
			oneOf(consumer).line(with(1), with(LineMatcher.match("package")));
			oneOf(consumer).comment(pos3, "fred");
			oneOf(consumer).line(with(2), with(LineMatcher.match("decl")));
			oneOf(consumer).comment(pos5, "bert");
		}});
		line("hello");
		line("\tpackage");
		line("fred");
		line("\t\tdecl");
		line("bert");
		blocker.flush();
	}
	
	@Test
	public void testBlockerCanHandleExdenting() {
		context.checking(new Expectations() {{
			oneOf(consumer).line(with(1), with(LineMatcher.match("level1")));
			oneOf(consumer).line(with(2), with(LineMatcher.match("level2")));
			oneOf(consumer).line(with(1), with(LineMatcher.match("level1 again")));
		}});
		line("\tlevel1");
		line("\t\tlevel2");
		line("\tlevel1 again");
		blocker.flush();
	}
	
	@Test
	public void testBlockerCanHandleMultipleDefnsAtSameScope() {
		context.checking(new Expectations() {{
			oneOf(consumer).line(with(1), with(LineMatcher.match("scope")));
			oneOf(consumer).line(with(2), with(LineMatcher.match("nest")));
			oneOf(consumer).line(with(2), with(LineMatcher.match("same")));
		}});
		line("\tscope");
		line("\t\tnest");
		line("\t\tsame");
		blocker.flush();
	}

	@Test
	public void indentingRestartsAfterNewFile() {
		context.checking(new Expectations() {{
			oneOf(consumer).newFile();
			oneOf(consumer).line(with(1), with(LineMatcher.match("level1")));
			oneOf(consumer).line(with(2), with(LineMatcher.match("level2")));
			oneOf(consumer).newFile();
			oneOf(consumer).line(with(1), with(LineMatcher.match("level1 again")));
		}});
		blocker.newFile();
		line("\tlevel1");
		line("\t\tlevel2");
		blocker.flush();
		blocker.newFile();
		line("\tlevel1 again");
		blocker.flush();
	}
	
	@Test
	public void indentingRestartsAfterNewFileAtTopLevel() {
		context.checking(new Expectations() {{
			oneOf(consumer).newFile();
			oneOf(consumer).line(with(1), with(LineMatcher.match("level1")));
			oneOf(consumer).newFile();
			oneOf(consumer).line(with(1), with(LineMatcher.match("level1 again")));
		}});
		blocker.newFile();
		line("\tlevel1");
		blocker.flush();
		blocker.newFile();
		line("\tlevel1 again");
		blocker.flush();
	}
	
	private void line(String line) {
		blocker.present("-", lineNo++, line);
	}
}
