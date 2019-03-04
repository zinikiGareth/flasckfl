package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAPatternParser implements TDAParsing {
	private final ErrorReporter errors;
	private final Consumer<Pattern> consumer;

	public TDAPatternParser(ErrorReporter errors, Consumer<Pattern> consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (errors.hasErrors())
			return null;
		PattToken initial = PattToken.from(toks);
		if (initial == null)
			return null;

		switch (initial.type) {
			case PattToken.NUMBER:
			case PattToken.TRUE:
			case PattToken.FALSE: // Constants by themselves
			{
				return handleConst(initial);
			}
			case PattToken.VAR: { // Simple pattern: x
				return handleASimpleVar(initial);
			}
			case PattToken.TYPE: { // Simple constructor match: Nil
				return handleASimpleConstructor(initial);
			}
			case PattToken.ORB: { // Complex array of cases wrapped in parens: qv 
				return handleORBCases(toks);
			}
			default: { // anything else is an error
				errors.message(toks, "invalid pattern");
				return null;
			}
		}
	}

	public TDAParsing handleORBCases(Tokenizable toks) {
		PattToken inside = PattToken.from(toks);
		if (inside == null) {
			errors.message(toks, "invalid pattern");
			return null;
		}
		TDAParsing success;
		switch (inside.type) {
			case PattToken.VAR: {
				success = handleASimpleVar(inside);
				break;
			}
			case PattToken.TYPE: {
				success = handleCasesStartingWithAType(toks, inside);
				break;
			}
			default: {
				errors.message(toks, "invalid pattern");
				return null;
			}
		}
		if (success == null)
			return null;
		PattToken crb = PattToken.from(toks);
		if (crb == null || crb.type != PattToken.CRB) {
			errors.message(toks, "invalid pattern");
			return null;
		}
		return this;
	}

	private TDAParsing handleConst(PattToken constant) {
		switch (constant.type) {
			case PattToken.NUMBER: {
				consumer.accept(new ConstPattern(constant.location, ConstPattern.INTEGER, constant.text));
				return this;
			}
			case PattToken.TRUE:
			case PattToken.FALSE:
			{
				consumer.accept(new ConstPattern(constant.location, ConstPattern.BOOLEAN, constant.text));
				return this;
			}
			default: {
				throw new RuntimeException("type " + constant.type + " should not come here and not be handled");
			}
		}
	}

	public TDAParsing handleASimpleVar(PattToken initial) {
		consumer.accept(new VarPattern(initial.location, initial.text));
		return this;
	}

	public TDAParsing handleASimpleConstructor(PattToken initial) {
		consumer.accept(new ConstructorMatch(initial.location, initial.text));
		return this;
	}

	public TDAParsing handleCasesStartingWithAType(Tokenizable toks, PattToken inside) {
		int beforeChecking = toks.at();
		PattToken var = PattToken.from(toks);
		if (var.type == PattToken.VAR) {
			return handleATypedReference(inside, var);
		} else if (var.type == PattToken.OCB) {
			return handleAConstructorMatch(inside, toks);
		} else if (var.type == PattToken.CRB) {
			toks.reset(beforeChecking);
			return handleASimpleConstructor(inside);
		} else {
			errors.message(toks, "invalid pattern");
			return null;
		}
	}

	public TDAParsing handleAConstructorMatch(PattToken inside, Tokenizable toks) {
		ConstructorMatch m = new ConstructorMatch(inside.location, inside.text);
		PattToken ccb = PattToken.from(toks); // CCB
		if (ccb == null || ccb.type != PattToken.CCB) {
			errors.message(toks, "invalid pattern");
			return null;
		}
		consumer.accept(m);
		return this;
	}

	private TDAParsing handleATypedReference(PattToken type, PattToken var) {
		TypeReference tr = new TypeReference(type.location, type.text);
		TypedPattern m = new TypedPattern(type.location, tr, var.location, var.text);
		consumer.accept(m);
		return this;
	}
}
