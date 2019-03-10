package test.parsing;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ParsedLineConsumer;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDACardElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ParsedLineConsumer consumer;

	public TDACardElementsParser(ErrorReporter errors, ParsedLineConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
