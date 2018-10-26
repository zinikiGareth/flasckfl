package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.StructDefn.StructType;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAIntroParser implements TDAParsing {
	private final TopLevelDefnConsumer consumer;

	public TDAIntroParser(ErrorReporter errors, TopLevelDefnConsumer consumer) {
		this.consumer = consumer;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (!toks.hasMore())
			return null;
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null)
			return null; // in the "nothing doing" sense

		consumer.newStruct(new StructDefn(kw.location, null, StructType.STRUCT, new SolidName(null, "Nil"), true, null));
		return new TDAStructFieldParser();
	}

}
