package org.flasck.flas.stories;

import org.flasck.flas.ErrorResult;
import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parser.TryParsing;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.reflection.Reflection;


public class MultiParser {
	private final Class<? extends TryParsing>[] klz;

	@SafeVarargs
	public MultiParser(Class<? extends TryParsing>... klz) {
		this.klz = klz;
	}

	public Object parse(Block b) {
		Tokenizable line = new Tokenizable(b.line.text());
		ErrorResult firstError = null;
		for (Class<? extends TryParsing> k : klz) {
			line.reset(0);
			Object o = Reflection.create(k).tryParsing(line);
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
