package org.flasck.flas.stories;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parser.TryParsing;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.reflection.Reflection;


public class MultiParser {
	private final Class<? extends TryParsing>[] klz;
	private Scope scope;

	@SafeVarargs
	public MultiParser(Scope scope, Class<? extends TryParsing>... klz) {
		this.scope = scope;
		this.klz = klz;
	}

	public Object parse(Block b) {
		Tokenizable line = new Tokenizable(b);
		ErrorResult firstError = null;
		for (Class<? extends TryParsing> k : klz) {
			line.reset(0);
			Object o = Reflection.create(k, scope).tryParsing(line);
			if (o == null)
				continue;
			else if (o instanceof ErrorResult && firstError == null)
				firstError = (ErrorResult) o;
			else
				return o;
				
		}
		return firstError; // either first error or "null"
	}
}
