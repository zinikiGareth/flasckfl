package org.flasck.flas.blocker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.CommentToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDANester implements BlockConsumer {
	private final List<TDAParsing> stack = new ArrayList<>();
	private InputPosition lastloc;
	private final ErrorReporter errors;
	private final TDAParsing topLevel;

	public TDANester(ErrorReporter errors, TDAParsing topLevel) {
		this.errors = errors;
		this.topLevel = topLevel;
	}

	@Override
	public void newFile() {
		lastloc = null;
		stack.add(topLevel);
	}

	@Override
	public void comment(InputPosition location, String text) {
		errors.logParsingToken(new CommentToken(location, text));
	}

	@Override
	public void line(int depth, ContinuedLine currline) {
		if (depth > stack.size())
			return;
		// need to store that as the parser for depth+1
		// we may want to tell the nested parsers we're closing them
		final Tokenizable tkz = new Tokenizable(currline);
		while (stack.size() > depth) {
			TDAParsing endScope = stack.remove(stack.size()-1);
			endScope.scopeComplete(lastloc);
		}
		lastloc = tkz.realinfo();
		TDAParsing nesting = stack.get(depth-1).tryParsing(tkz);
		if (nesting != null)
			stack.add(nesting);
	}

	@Override
	public void flush() {
		while (stack.size() > 0) {
			TDAParsing endScope = stack.remove(stack.size()-1);
			endScope.scopeComplete(lastloc);
		}
	}
}
