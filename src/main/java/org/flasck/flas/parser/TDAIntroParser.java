package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.ObjectDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.stories.TDAParserConstructor;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAIntroParser implements TDAParsing, ScopeReceiver {
	private final ErrorReporter errors;
	private final TopLevelDefinitionConsumer consumer;
	private IScope scope;

	public TDAIntroParser(ErrorReporter errors, TopLevelDefinitionConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
		consumer.scopeTo(this);
	}
	
	@Override
	public void provideScope(IScope scope) {
		this.scope = scope;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null)
			return null; // in the "nothing doing" sense

		switch (kw.text) {
		case "card": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			CardName qn = (CardName)consumer.cardName(tn.text);
			CardDefinition card = new CardDefinition(errors, kw.location, tn.location, scope, qn);
			consumer.newCard(card);
			return new TDACardElementsParser(errors, null);
		}
		case "struct":
		case "entity":
		case "deal":
		case "offer": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(toks);
				if (ta == null) {
					errors.message(toks, "invalid type argument");
					return null;
				} else
					polys.add(new PolyType(ta.location, ta.text));
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			final StructDefn sd = new StructDefn(kw.location, tn.location, FieldsDefn.FieldsType.valueOf(kw.text.toUpperCase()), consumer.qualifyName(tn.text), true, polys);
			consumer.newStruct(sd);
			return new TDAStructFieldParser(errors, sd);
		}
		case "object": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			List<PolyType> polys = new ArrayList<>();
			while (toks.hasMore()) {
				PolyTypeToken ta = PolyTypeToken.from(toks);
				if (ta == null) {
					errors.message(toks, "syntax error");
					return new IgnoreNestedParser();
				} else
					polys.add(new PolyType(ta.location, ta.text));
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			ObjectDefn od = new ObjectDefn(kw.location, tn.location, consumer.qualifyName(tn.text), true, polys);
			consumer.newObject(od);
			return new TDAObjectElementsParser(errors, od, consumer);
		}
		case "contract": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "tokens after end of line");
				return new IgnoreNestedParser();
			}
			ContractDecl decl = new ContractDecl(kw.location, tn.location, consumer.qualifyName(tn.text));
			consumer.newContract(decl);
			return new ContractMethodParser(errors, decl);
		}
		default:
			return null;
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

	public static TDAParserConstructor constructor(TopLevelDefinitionConsumer consumer) {
		return new TDAParserConstructor() {
			@Override
			public TDAParsing construct(ErrorReporter errors) {
				return new TDAIntroParser(errors, consumer);
			}
		};
	}

}
