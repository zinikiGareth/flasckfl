package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.parsedForm.ApplyExpr;
import org.flasck.flas.parsedForm.ItemExpr;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class Expression implements TryParsing {
	// TODO: I think this may need breaking into two functions, one of which checks that we've used all the available
	// space or something.  Or maybe that's the caller's job.
	@Override
	public Object tryParsing(Tokenizable line) {
		int mark = line.at(); // do this now in case we need it later
		ExprToken s = ExprToken.from(line);
		System.out.println("Start " + s);
		if (s.type == ExprToken.NUMBER)
			return new ItemExpr(s);
		if (s.type == ExprToken.IDENTIFIER || s.type == ExprToken.SYMBOL) {
			ItemExpr op = new ItemExpr(s);
			List<Object> args = new ArrayList<Object>();
			while (line.hasMore()) {
				mark = line.at();
				s = ExprToken.from(line);
				System.out.println("Inner " + s);
				if (s.type == ExprToken.PUNC && s.text.equals("(")) {
					args.add(tryParsing(line));
					ExprToken crb = ExprToken.from(line);
					if (crb.type != ExprToken.PUNC || !crb.text.equals(")"))
						System.out.println("this would be an error");
				} else if (s.type == ExprToken.PUNC && s.text.equals(")")) {
					line.reset(mark);
					break;
				} else if (s.type == ExprToken.PUNC) {
					System.out.println("Random PUNC");
					return null; // an error
				} else
					args.add(new ItemExpr(s));
			}
			if (args.isEmpty())
				return op;
			else
				return new ApplyExpr(op, args);
		} else if (s.type == ExprToken.PUNC && s.text.equals("(")) {
			System.out.println("nested");
			Object ret = tryParsing(line);
			ExprToken crb = ExprToken.from(line);
			if (crb.type != ExprToken.PUNC || !crb.text.equals(")"))
				System.out.println("this would be an error");
			return ret;
		} else {
			// error reporting - some sort of syntax error
			System.out.println("What was this? " + s);
			return null;
		}
	}
}
