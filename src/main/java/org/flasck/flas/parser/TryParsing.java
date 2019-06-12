package org.flasck.flas.parser;

import org.flasck.flas.tokenizers.Tokenizable;

@Deprecated
public interface TryParsing {

	Object tryParsing(Tokenizable line);

}
