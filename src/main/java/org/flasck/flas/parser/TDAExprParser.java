package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExprParser implements TDAParsing {

	private final ExprTermConsumer builder;

	public TDAExprParser(ErrorReporter errors, ExprTermConsumer builder) {
		this.builder = builder;
	}

	public TDAParsing tryParsing(Tokenizable line) {
		while (true) {
			int mark = line.at();
			ExprToken tok = ExprToken.from(line);
			if (tok == null) {
				builder.done();
				return null;
			}
			switch (tok.type) {
			case ExprToken.NUMBER:
				builder.term(new NumericLiteral(tok.location, tok.text, -1));
				break;
			case ExprToken.STRING:
				builder.term(new StringLiteral(tok.location, tok.text));
				break;
			case ExprToken.IDENTIFIER:
				builder.term(new UnresolvedVar(tok.location, tok.text));
				break;
			case ExprToken.SYMBOL:
				// A "declaration" operator ends an expression without being consumed
				if ("=".equals(tok.text)) {
					line.reset(mark);
					builder.done();
					return null;
				}
				builder.term(new UnresolvedOperator(tok.location, tok.text));
				break;
			case ExprToken.PUNC:
				if (tok.text.equals("."))
					builder.term(new UnresolvedOperator(tok.location, tok.text)); // can we just resolve it - it's . after all !
				else
					builder.term(new Punctuator(tok.location, tok.text));
				break;
			default:
				throw new RuntimeException("Not found");
			}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
