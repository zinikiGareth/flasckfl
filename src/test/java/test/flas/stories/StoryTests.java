package test.flas.stories;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.MessagesHandler;
import org.flasck.flas.parsedForm.MethodMessage;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.stories.FLASStory.State;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class StoryTests {
	public @Rule JUnitRuleMockery context = new JUnitRuleMockery();
	ErrorReporter er = context.mock(ErrorReporter.class);
	MessagesHandler mcd = context.mock(MessagesHandler.class);
	IScope scope = context.mock(IScope.class);
	
	@Test
	public void weCanTriviallyHandleNoMessagesAtAll() {

		FLASStory flas = new FLASStory();
		List<Block> blocks = new ArrayList<Block>();
		flas.handleMessageMethods(er, new State(scope, "ME"), mcd, blocks);
	}

	@Test
	public void weCanHandleASimpleAssignMessage() {

		context.checking(new Expectations() {{
			oneOf(mcd).addMessage(with(any(MethodMessage.class)));
		}});
		
		FLASStory flas = new FLASStory();
		List<Block> blocks = new ArrayList<Block>();
		blocks.add(new Block(4, "x <- \"hello\""));
		flas.handleMessageMethods(er, new State(scope, "ME"), mcd, blocks);
	}

	@Test
	public void weCanHandleASimpleInvokeMessage() {

		context.checking(new Expectations() {{
			oneOf(mcd).addMessage(with(any(MethodMessage.class)));
		}});
		
		FLASStory flas = new FLASStory();
		List<Block> blocks = new ArrayList<Block>();
		blocks.add(new Block(4, "<- svc.echo \"hello\""));
		flas.handleMessageMethods(er, new State(scope, "ME"), mcd, blocks);
	}

	@Test
	public void weCanHandleTwoMessages() {

		context.checking(new Expectations() {{
			oneOf(mcd).addMessage(with(any(MethodMessage.class)));
			oneOf(mcd).addMessage(with(any(MethodMessage.class)));
		}});
		
		FLASStory flas = new FLASStory();
		List<Block> blocks = new ArrayList<Block>();
		blocks.add(new Block(4, "x <- \"hello\""));
		blocks.add(new Block(4, "<- svc.echo \"hello\""));
		flas.handleMessageMethods(er, new State(scope, "ME"), mcd, blocks);
	}

	@Test
	public void weCanHandleNestedDefinitionsAfterTheFinalMessage() {

		context.checking(new Expectations() {{
			allowing(mcd).caseName(); will(returnValue(FunctionName.contractMethod(null, new CSName(new CardName(new PackageName("ME"), "Card"), "Contract"), "m")));
			oneOf(mcd).addMessage(with(any(MethodMessage.class)));
			oneOf(mcd).addMessage(with(any(MethodMessage.class)));
			oneOf(mcd).innerScope(); will(returnValue(scope));

			oneOf(scope).caseName("ME.Card.Contract.m.v"); will(returnValue(0));
			oneOf(scope).define(with("v"), with(any(FunctionCaseDefn.class)));
			
			allowing(er).hasErrors(); will(returnValue(false));
		}});
		
		FLASStory flas = new FLASStory();
		List<Block> blocks = new ArrayList<Block>();
		blocks.add(new Block(4, "x <- v"));
		final Block blk2 = new Block(4, "<- svc.echo v");
		blocks.add(blk2);
		blk2.nested.add(new Block(5, "v = \"hello\""));
		flas.handleMessageMethods(er, new State(scope, "ME"), mcd, blocks);
	}

	// we cant handle inner defns on non-final messages
}
