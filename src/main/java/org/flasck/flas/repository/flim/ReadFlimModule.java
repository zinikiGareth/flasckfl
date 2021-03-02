package org.flasck.flas.repository.flim;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public interface ReadFlimModule {
	TDAParsing readLine(ErrorReporter errors, Repository repository, KeywordToken kw, NameOfThing container, Tokenizable toks);
}
