package org.flasck.flas.blocker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.CommentToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDANester implements BlockConsumer {
	private final List<TDAParsingWithAction> stack = new ArrayList<>();
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
		add(topLevel);
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
			pop();
		}
		lastloc = tkz.realinfo();
		TDAParsing nesting = stack.get(depth-1).tryParsing(tkz);
		if (nesting != null)
			add(nesting);
	}

	@Override
	public void flush() {
		// when flushing, we want to make sure our line is later than anything valid
		// so create a new location past the end of the file, indent 0
		
		if (lastloc != null)
			lastloc = new InputPosition(lastloc.getUri(), lastloc.lineNo+1, 0, new Indent(0, 0), "");
		
		while (stack.size() > 0) {
			pop();
		}
	}

	private void add(TDAParsing parser) {
		if (parser instanceof TDAParsingWithAction) {
			stack.add((TDAParsingWithAction) parser);
		} else {
			stack.add(new TDAParsingWithAction(parser, null));
		}
	}

	private void pop() {
		TDAParsingWithAction endScope = stack.remove(stack.size()-1);
		endScope.scopeComplete(lastloc);
		afterParsing(endScope);
	}

	private void afterParsing(TDAParsingWithAction endScope) {
		if (endScope.parser instanceof TDAParsingWithAction)
			afterParsing((TDAParsingWithAction) endScope.parser);
		if (endScope.afterParsing != null)
			endScope.afterParsing.run();
	}
}
