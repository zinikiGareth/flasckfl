package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class SingleLineFunctionCase implements TryParsing {

	// Random notes:
	// 1. We need a separate phase of the compiler that pulls together these separate definitions and makes a single function
	// 2. That single function then needs to take NEW arguments and add an HSIE switch statement
	@Override
	public Object tryParsing(Tokenizable line) {
		// Read the function name
		String name = ValidIdentifierToken.from(line);
		
		// Collect patterns into an argument
		List<Object> args = new ArrayList<Object>();
		
		// Read all the function's declared arguments
		PatternParser pp = new PatternParser();
		while (true) {
			int mark = line.at();
			Object o = pp.tryParsing(line);
			if (o != null) {
				args.add(o);
			} else {
				line.reset(mark);
				ExprToken tok = ExprToken.from(line);
				if (tok != null && tok.text.equals("="))
					break;
			}
		}
		
		// Now parse the expression on the right hand side
		Object expr = new Expression().tryParsing(line);
		if (line.hasMore())
			return null; // should return error

		// Build a response object
		return new FunctionCaseDefn(name, args, expr);
	}

}
