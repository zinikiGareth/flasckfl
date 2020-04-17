package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ConstPattern;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.compiler.DuplicateNameException;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TuplePattern;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAPatternParser implements TDAParsing {
	private final ErrorReporter errors;
	private final VarNamer namer;
	private final Consumer<Pattern> consumer;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAPatternParser(ErrorReporter errors, VarNamer namer, Consumer<Pattern> consumer, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		return tryParsing(toks, errors.mark());
	}

	public TDAParsing tryParsing(Tokenizable toks, ErrorMark currErr) {
		if (currErr.hasMoreNow())
			return null;
		
		// This case is for the simple case such as "Nil" where a no-arg constructor can match by itself
		TypeNameToken qn = TypeNameToken.qualified(toks);
		if (qn != null)
			return handleASimpleConstructor(qn);
		
		PattToken initial = PattToken.from(errors, toks);
		if (initial == null)
			return null;

		switch (initial.type) {
			case PattToken.NUMBER:
			case PattToken.STRING:
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
				return handleORBCases(initial, toks);
			}
			case PattToken.OSB: { // special list syntax
				return handleListCases(initial, toks);
			}
			default: return invalidPattern(toks);
		}
	}

	public TDAParsing handleORBCases(PattToken orb, Tokenizable toks) {
		List<Pattern> tuples = new ArrayList<>();
		TDAPatternParser delegate = new TDAPatternParser(errors, namer, patt -> {
			tuples.add(patt);
		}, topLevel);
		PattToken crb;
		while (true) {
			TDAParsing success = delegate.handleOneORBMemberCase(toks);
			if (success == null)
				return null;
			crb = PattToken.from(errors, toks);
			if (crb == null)
				return invalidPattern(toks);
			if (crb.type == PattToken.CRB)
				break;
			if (crb.type == PattToken.COMMA)
				continue; // now a tuple
			return invalidPattern(toks);
		}
		if (tuples.isEmpty())
			return invalidPattern(toks);
		if (tuples.size() == 1)
			consumer.accept(tuples.get(0));
		else
			consumer.accept(new TuplePattern(orb.location.copySetEnd(crb.location.off), tuples));
		return this;
	}

	public TDAParsing handleOneORBMemberCase(Tokenizable toks) {
		int mark = toks.at();
		TypeNameToken qn = TypeNameToken.qualified(toks);
		if (qn != null) {
			toks.reset(mark); // let it read it again
			return handleCasesStartingWithAType(toks, qn);
		}
		else {
			PattToken inside = PattToken.from(errors, toks);
			if (inside == null)
				return invalidPattern(toks);
			switch (inside.type) {
				case PattToken.NUMBER:
				case PattToken.TRUE:
				case PattToken.FALSE: // Constants in parens
				{
					return handleConst(inside);
				}
				case PattToken.VAR: {
					return handleASimpleVar(inside);
				}
				case PattToken.TYPE: {
					throw new RuntimeException("should be handled above");
				}
				default: return invalidPattern(toks);
			}
		}
	}

	private TDAParsing handleConst(PattToken constant) {
		switch (constant.type) {
			case PattToken.NUMBER: {
				consumer.accept(new ConstPattern(constant.location, ConstPattern.INTEGER, constant.text));
				return this;
			}
			case PattToken.STRING: {
				consumer.accept(new ConstPattern(constant.location, ConstPattern.STRING, constant.text));
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
		final VarPattern vp = new VarPattern(initial.location, namer.nameVar(initial.location, initial.text));
		consumer.accept(vp);
		try {
			topLevel.argument(errors, vp);
		} catch (DuplicateNameException ex) {
			errors.message(vp.location(), "duplicate function argument " + vp.var);
		}
		return this;
	}

	public TDAParsing handleASimpleConstructor(TypeNameToken type) {
		consumer.accept(new ConstructorMatch(type.location, type.text));
		return this;
	}

	public TDAParsing handleCasesStartingWithAType(Tokenizable toks, TypeNameToken type) {
		List<TypeReference> ref = new ArrayList<>();
		if (new TDATypeReferenceParser(errors, x->ref.add(x)).tryParsing(toks) == null) {
			// it didn't parse, so give up hope
			return null;
		}
		TypeReference tr = ref.get(0);
		int beforeChecking = toks.at();
		
		// Now, see what else we've got ...
		PattToken tok = PattToken.from(errors, toks);
		if (tok.type == PattToken.VAR) {
			TypedPattern m = new TypedPattern(type.location, tr, namer.nameVar(tok.location, tok.text));
			consumer.accept(m);
			topLevel.argument(errors, m);
			return this;
		} else if (tr.hasPolys()) {
			errors.message(toks, "type parameters can only be used with type patterns");
			return null;
		} else if (tok.type == PattToken.OCB) {
			return handleAConstructorMatch(type, toks);
		} else if (tok.type == PattToken.CRB) {
			toks.reset(beforeChecking);
			return handleASimpleConstructor(type);
		} else
			return invalidPattern(toks);
	}

	public TDAParsing handleAConstructorMatch(TypeNameToken type, Tokenizable toks) {
		ConstructorMatch m = new ConstructorMatch(type.location, type.text);
		while (true) {
			PattToken fld = PattToken.from(errors, toks);
			if (fld == null)
				return invalidPattern(toks);
			if (fld.type == PattToken.CCB)
				break;
			if (fld.type != PattToken.VAR)
				return invalidPattern(toks);
			PattToken colon = PattToken.from(errors, toks); // :
			if (colon == null || colon.type != PattToken.COLON)
				return invalidPattern(toks);
			TDAParsing success = new TDAPatternParser(errors, namer, patt -> {
				m.args.add(m.new Field(fld.location, fld.text, patt));
			}, topLevel).tryParsing(toks);
			if (success == null)
				return null;
			PattToken ccb = PattToken.from(errors, toks);
			if (ccb == null)
				return invalidPattern(toks);
			if (ccb.type == PattToken.CCB)
				break;
			else if (ccb.type != PattToken.COMMA)
				return invalidPattern(toks);
		}
		consumer.accept(m);
		return this;
	}

	private TDAParsing handleListCases(PattToken osb, Tokenizable toks) {
		List<Pattern> members = new ArrayList<>();
		TDAPatternParser inner = new TDAPatternParser(errors, namer, patt -> {
			members.add(0, patt); // store them in reverse order
		}, topLevel);
		while (true) {
			int from = toks.at();
			PattToken nx = PattToken.from(errors, toks);
			if (nx.type == PattToken.CSB) {
				break;
			}
			toks.reset(from);
			if (inner.tryParsing(toks) == null)
				return null;
			PattToken comma = PattToken.from(errors, toks);
			if (comma.type == PattToken.CSB)
				break;
			else if (comma.type != PattToken.COMMA)
				return invalidPattern(toks);
		}
		ConstructorMatch ret = new ConstructorMatch(osb.location, "Nil");
		
		while (!members.isEmpty()) {
			Pattern m = members.remove(0);
//			if (!(m instanceof ConstPattern)) {
//				toks.reset(m.location().off);
//				return invalidPattern(toks);
//			}
			ConstructorMatch prev = ret;
			ret = new ConstructorMatch(osb.location, "Cons");
			ret.args.add(ret.new Field(m.location(), "head", m));
			ret.args.add(ret.new Field(ret.location(), "tail", prev));
		}

		consumer.accept(ret);
		return this;
	}

	public TDAParsing invalidPattern(Tokenizable toks) {
		errors.message(toks, "invalid pattern");
		return null;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
