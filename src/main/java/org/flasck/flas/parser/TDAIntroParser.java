package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FieldsDefn;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAIntroParser implements TDAParsing {
	private final ErrorReporter errors;
	private final TopLevelDefnConsumer consumer;

	public TDAIntroParser(ErrorReporter errors, TopLevelDefnConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null)
			return null; // in the "nothing doing" sense

		switch (kw.text) {
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
			final StructDefn sd = new StructDefn(kw.location, tn.location, FieldsDefn.FieldsType.valueOf(kw.text.toUpperCase()), consumer.qualifyName(tn.text), true, polys);
			consumer.newStruct(sd);
			return new TDAStructFieldParser(errors, sd);
		}
		case "contract": {
			TypeNameToken tn = TypeNameToken.unqualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid or missing type name");
				return new IgnoreNestedParser();
			}
			return new NoNestingParser(errors);
		}
		default:
			return null;
		}
	}

}
