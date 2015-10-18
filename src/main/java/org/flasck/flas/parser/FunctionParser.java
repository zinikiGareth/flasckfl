package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class FunctionParser implements TryParsing {
	private final State state;

	public FunctionParser(State state) {
		this.state = state;
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		InputPosition init = line.realinfo();
		
		// Read the function name
		ValidIdentifierToken vit = ValidIdentifierToken.from(line);
		if (vit == null)
			return null;
		
		String name = vit.text;
		// don't allow keywords to start line
		if ("card".equals(name) ||
			"contract".equals(name) ||
			"event".equals(name) ||
			"handler".equals(name) ||
			"implements".equals(name) ||
			"service".equals(name) ||
			"state".equals(name) ||
			"struct".equals(name) ||
			"template".equals(name)) // more
			return null;
		
		// Collect patterns into an argument
		List<Object> args = new ArrayList<Object>();
		
		// Read all the function's declared arguments
		PatternParser pp = new PatternParser();
		while (line.hasMore()) {
			int mark = line.at();
			Object o = pp.tryParsing(line);
			if (o != null) {
				args.add(o);
			} else {
				line.reset(mark);
				ExprToken tok = ExprToken.from(line);
				if (tok != null && tok.text.equals("=")) {
					line.reset(mark);
					break;
				}
			}
		}
		
		if (!line.hasMore())
			return new FunctionIntro(init, state.withPkg(name), args);
		
		ExprToken tok = ExprToken.from(line);
		if (tok == null || !tok.text.equals("="))
			return ErrorResult.oneMessage(line, "= expected");
		// Now parse the expression on the right hand side
		Object expr = new Expression().tryParsing(line);
		if (expr == null)
			return ErrorResult.oneMessage(line, "syntax error");
		if (line.hasMore())
			return ErrorResult.oneMessage(line, "unexpected tokens at end of line");

		// Build a response object
		return new FunctionCaseDefn(init, state.withPkg(name), args, expr);
	}

}
