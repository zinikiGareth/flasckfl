package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATupleDeclarationParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ParsedLineConsumer consumer;
	private FunctionParser delegate;

	public TDATupleDeclarationParser(ErrorReporter errors, ParsedLineConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
//		consumer.scopeTo(this);
	}
	
//	@Override
//	public void provideScope(Scope scope) {
//		State state = new State(scope, scope.scopeName.uniqueName());
//		delegate = new FunctionParser(state);
//	}

	@Override
	public TDAParsing tryParsing(Tokenizable line) {
//		if (!line.hasMore())
//			return null;
		
		PattToken orb = PattToken.from(line);
		if (orb == null || orb.type != PattToken.ORB)
			return null;

		List<LocatedName> vars = new ArrayList<LocatedName>();
		boolean haveCRB = false;
		while (line.hasMore()) {
			PattToken nx = PattToken.from(line);
			if (nx.type == PattToken.CRB) {
				if (vars.isEmpty())
					errors.message(line, "missing var in tuple declaration");
				else
					errors.message(line, "syntax error");
				return null;
			}
//			if (nx.type != PattToken.VAR) {
//				if (vars.isEmpty())
//					return null;
//				else {
//					errors.message(nx.location, "syntax error parsing tuple");
//					return null;
//				}
//			}
			vars.add(new LocatedName(nx.location, nx.text));
			PattToken cm = PattToken.from(line);
			if (cm == null) {
				errors.message(line, "syntax error");
				return null;
			}
			if (cm.type == PattToken.CRB) {
				haveCRB = true;
				break;
			}
//			else if (cm.type != PattToken.COMMA) {
//			}
		}
		
		if (!haveCRB) {
			errors.message(line, "syntax error");
			return null;
		}
		if (vars.size() < 2) {
			errors.message(line, "insufficient vars to make tuple declaration");
			return null;
		}
		if (!line.hasMore()) {
			errors.message(line, "syntax error");
			return null;
		}
		ExprToken tok = ExprToken.from(line);
		if (!tok.text.equals("=")) {
			errors.message(line, "syntax error");
			return null;
		}
		if (!line.hasMore()) {
			errors.message(line, "tuple assignment requires expression");
			return null;
		}

		return TDAMultiParser.top(errors, consumer);
	}
}
