package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

// TODO: introduce a "just one" error reporter that we can use to make sure we don't overly complain about missing parens, etc.
public class TDAStackReducer implements ExprTermConsumer {
	public class ParentConsumer implements ExprTermConsumer {

		@Override
		public void term(Expr term) {
			stack.remove(0);
			if (stack.isEmpty())
				throw new RuntimeException("Stack underflow - should be error");
			stack.get(0).term(term);
		}

		@Override
		public void done() {
			throw new org.zinutils.exceptions.NotImplementedException();
		}

		@Override
		public void showStack(StackDumper d) {
		}
	}

	private final ErrorReporter errors;
	private final List<ExprTermConsumer> stack = new ArrayList<>();
	private InputPosition lineStart;

	public TDAStackReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.errors = errors;
		this.stack.add(new TDAExprReducer(errors, builder));
	}

	@Override
	public void term(Expr term) {
		if (lineStart == null)
			lineStart = term.location();
		if (term instanceof Punctuator) {
			final Punctuator punc = (Punctuator)term;
			if (punc.is("(")) {
				this.stack.add(0, punc.openParenParser(errors, new ParentConsumer()));
				return;
			} else if (punc.is("[")) {
				this.stack.add(0, punc.openSquareParser(errors, new ParentConsumer()));
				return;
			} else if (punc.is("{")) {
				this.stack.add(0, punc.openCurlyParser(errors, new ParentConsumer()));
				return;
			} 
		}
		this.stack.get(0).term(term);
	}

	@Override
	public void done() {
		if (this.stack.size() != 1) {
			errors.message(lineStart, "syntax error");
			return;
		}
		this.stack.get(0).done();
	}

	@Override
	public void showStack(StackDumper d) {
		d.levels(stack.size());
		for (int i=stack.size()-1;i>=0;i--) {
			stack.get(i).showStack(d.indent(stack.size() - i));
		}
	}
}
