package org.flasck.flas.blocker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefnConsumer;
import org.flasck.flas.tokenizers.Tokenizable;

/** I'm wondering if we actually need "Story" as such.
 * Some version of this class needs to exist to be a BlockConsumer and to hand off to the child
 * -- and somebody somewhere needs to maintain a stack
 * -- I'm just not sure it's at this level
 */
public class TDANester implements BlockConsumer {
	private final List<TDAParsing> stack = new ArrayList<>();

	public TDANester(TDAParsing topLevel) {
		stack.add(topLevel);
	}

	@Override
	public void newFile() {
		// need to do a state reset
	}

	@Override
	public void comment(String text) {
		// I don't think we particularly care about this ...
		// If we do, it's probably just to pass it through ...
	}

	@Override
	public void line(int depth, ContinuedLine currline) {
		if (depth > stack.size())
			return;
		// need to store that as the parser for depth+1
		// we may want to tell the nested parsers we're closing them
		while (stack.size() > depth)
			stack.remove(stack.size()-1);
		TDAParsing nesting = stack.get(depth-1).tryParsing(new Tokenizable(currline));
		if (nesting != null)
			stack.add(nesting);
	}

}
