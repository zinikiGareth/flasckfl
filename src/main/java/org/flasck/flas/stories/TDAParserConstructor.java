package org.flasck.flas.stories;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;

public interface TDAParserConstructor {
	TDAParsing construct(ErrorReporter errors);
}
