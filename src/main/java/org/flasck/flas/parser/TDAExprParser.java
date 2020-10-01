package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.DotOperator;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ut.IntroduceNamer;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExprParser implements TDAParsing {
	private final IntroduceNamer namer;
	private final ExprTermConsumer builder;
	private final ErrorReporter errors;
	private final IntroductionConsumer consumer;

	public TDAExprParser(ErrorReporter errors, IntroduceNamer namer, ExprTermConsumer builder, IntroductionConsumer consumer) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.consumer = consumer;
	}

	public TDAParsing tryParsing(Tokenizable line) {
		while (true) {
			int mark = line.at();
			ExprToken tok = ExprToken.from(errors, line);
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
			case ExprToken.IDENTIFIER: {
				Expr term;
				if (Character.isAlphabetic(tok.text.charAt(0))) {
					if (Character.isLowerCase(tok.text.charAt(0)))
						term = new UnresolvedVar(tok.location, tok.text);
					else {
						line.reset(mark);
						List<TypeReference> ltr = new ArrayList<>();
						Consumer<TypeReference> captureTR = tr -> ltr.add(tr);
						new TDATypeReferenceParser(errors, (VarNamer)namer, captureTR, (TopLevelDefinitionConsumer)consumer).tryParsing(line);
						if (!ltr.isEmpty())
							term = ltr.get(0);
						else
							return new IgnoreNestedParser();
					}
				} else if (tok.text.equals("_"))
					term = new AnonymousVar(tok.location);
				else if (consumer != null && tok.text.startsWith("_")) {
					IntroduceVar iv = new IntroduceVar(tok.location, namer, tok.text.substring(1));
					consumer.newIntroduction(errors, iv);
					term = iv;
				} else {
					errors.message(tok.location, "syntax error");
					return new IgnoreNestedParser();
				}
				builder.term(term);
				break;
			}
			case ExprToken.SYMBOL:
				// A "declaration" or "sendto" operator ends an expression without being consumed
				if ("=".equals(tok.text) || "=>".equals(tok.text) || "<-".equals(tok.text)) {
					line.reset(mark);
					builder.done();
					return null;
				}
				builder.term(new UnresolvedOperator(tok.location, tok.text));
				break;
			case ExprToken.PUNC:
				if (tok.text.equals("."))
					builder.term(new DotOperator(tok.location));
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
