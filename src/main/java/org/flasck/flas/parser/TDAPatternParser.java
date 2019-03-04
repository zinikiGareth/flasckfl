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
import org.flasck.flas.tokenizers.TypeNameToken;

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
		
		TypeNameToken qn = TypeNameToken.qualified(toks);
		if (qn != null)
			return handleASimpleConstructor(qn);
		
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
			case PattToken.TYPE: { // Simple constructor match: Nil, should have matched TNT above
				throw new RuntimeException("Should have matched above");
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
		TDAParsing success;
		TypeNameToken qn = TypeNameToken.qualified(toks);
		if (qn != null)
			success = handleCasesStartingWithAType(toks, qn);
		else {
			PattToken inside = PattToken.from(toks);
			if (inside == null) {
				errors.message(toks, "invalid pattern");
				return null;
			}
			switch (inside.type) {
				case PattToken.NUMBER:
				case PattToken.TRUE:
				case PattToken.FALSE: // Constants in parens
				{
					success = handleConst(inside);
					break;
				}
				case PattToken.VAR: {
					success = handleASimpleVar(inside);
					break;
				}
				case PattToken.TYPE: {
					throw new RuntimeException("should be handled above");
				}
				default: {
					errors.message(toks, "invalid pattern");
					return null;
				}
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

	public TDAParsing handleASimpleConstructor(TypeNameToken type) {
		consumer.accept(new ConstructorMatch(type.location, type.text));
		return this;
	}

	public TDAParsing handleCasesStartingWithAType(Tokenizable toks, TypeNameToken type) {
		int beforeChecking = toks.at();
		PattToken tok = PattToken.from(toks);
		if (tok.type == PattToken.OSB) {
			while (true) {
				TypeNameToken p1 = TypeNameToken.qualified(toks);
				tok = PattToken.from(toks);
				if (tok.type == PattToken.COMMA)
					continue;
				else if (tok.type == PattToken.CSB)
					break;
				else {
					errors.message(toks, "invalid pattern");
					return null;
				}
			}
			tok = PattToken.from(toks);
			if (tok.type != PattToken.VAR) {
				errors.message(toks, "type parameters can only be used with type patterns");
				return null;
			}
		}
		if (tok.type == PattToken.VAR) {
			return handleATypedReference(type, tok);
		} else if (tok.type == PattToken.OCB) {
			return handleAConstructorMatch(type, toks);
		} else if (tok.type == PattToken.CRB) {
			toks.reset(beforeChecking);
			return handleASimpleConstructor(type);
		} else {
			errors.message(toks, "invalid pattern");
			return null;
		}
	}

	public TDAParsing handleAConstructorMatch(TypeNameToken type, Tokenizable toks) {
		ConstructorMatch m = new ConstructorMatch(type.location, type.text);
		PattToken fld = PattToken.from(toks);
		if (fld != null && fld.type != PattToken.CCB) {
			PattToken colon = PattToken.from(toks); // :
			TypeNameToken ctor = TypeNameToken.qualified(toks); // ctor
			m.args.add(m.new Field(fld.location, fld.text, new ConstructorMatch(ctor.location, ctor.text)));
			fld = PattToken.from(toks); // CCB
		}
		if (fld == null || fld.type != PattToken.CCB) {
			errors.message(toks, "invalid pattern");
			return null;
		}
		consumer.accept(m);
		return this;
	}

	private TDAParsing handleATypedReference(TypeNameToken type, PattToken var) {
		TypeReference tr = new TypeReference(type.location, type.text);
		TypedPattern m = new TypedPattern(type.location, tr, var.location, var.text);
		consumer.accept(m);
		return this;
	}
}
