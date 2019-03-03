package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

public class Punctuator implements Expr {
	public final InputPosition location;
	public final String punc;

	public Punctuator(InputPosition location, String punc) {
		this.location = location;
		this.punc = punc;
	}
	
	@Override
	public InputPosition location() {
		return location;
	}
	
	public ExprTermConsumer openParenParser(ErrorReporter errors, ExprTermConsumer builder) {
		if (!punc.equals("("))
			throw new RuntimeException("Can only call this with open paren");
		return new ParenTermConsumer(location, errors, builder, this);
	}
	
	public ExprTermConsumer openSquareParser(ErrorReporter errors, ExprTermConsumer builder) {
		if (!punc.equals("["))
			throw new RuntimeException("Can only call this with open square");
		return new ParenTermConsumer(location, errors, builder, this);
	}
	
	public ExprTermConsumer openCurlyParser(ErrorReporter errors, ExprTermConsumer builder) {
		if (!punc.equals("{"))
			throw new RuntimeException("Can only call this with open curly");
		return new ParenTermConsumer(location, errors, builder, this);
	}
	
	public boolean is(String punc) {
		return this.punc.equals(punc);
	}

	@Override
	public String toString() {
		return punc;
	}
}
