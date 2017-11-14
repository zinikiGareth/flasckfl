package org.flasck.flas.stories;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.parser.TryParsing;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.reflection.Reflection;

public class MultiParser {
	private final Class<? extends TryParsing>[] klz;
	private final State state;

	@SafeVarargs
	public MultiParser(State state, Class<? extends TryParsing>... klz) {
		this.state = state;
		this.klz = klz;
	}

	public Object parse(Block b) {
		Tokenizable line = new Tokenizable(b);
		for (Class<? extends TryParsing> k : klz) {
			line.reset(0);
			Object o = Reflection.create(k, state).tryParsing(line);
			if (o == null)
				continue;
			else
				return o;
		}
		return null;
	}
}
