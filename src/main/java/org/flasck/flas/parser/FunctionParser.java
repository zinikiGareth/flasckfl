package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class FunctionParser implements TryParsing {
	private final Scope scope;

	public FunctionParser(Scope scope) {
		this.scope = scope;
	}
	
	@Override
	public Object tryParsing(Tokenizable line) {
		// Read the function name
		String name = ValidIdentifierToken.from(line);
		if (name == null)
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
			return new FunctionIntro(name, args);
		
		ExprToken tok = ExprToken.from(line);
		if (tok == null || !tok.text.equals("="))
			return ErrorResult.oneMessage(line, "= expected");
		// Now parse the expression on the right hand side
		Object expr = new Expression().tryParsing(line);
		if (line.hasMore())
			return ErrorResult.oneMessage(line, "unexpected tokens at end of line");

		// Build a response object
		return new FunctionCaseDefn(scope, name, args, expr);
	}

}
