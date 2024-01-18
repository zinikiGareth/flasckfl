package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class ApplicationElementParser implements TDAParsing {
	private final ErrorReporter errors;
	private final InputPosition startPos;
	private final TopLevelNamer namer;
	private final ApplicationElementConsumer consumer;
	private ApplicationRouting routing;

	public ApplicationElementParser(ErrorReporter errors, InputPosition startPos, TopLevelNamer namer, ApplicationElementConsumer consumer) {
		this.errors = errors;
		this.startPos = startPos;
		this.namer = namer;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "expected assembly keyword");
			return new IgnoreNestedParser(errors);
		}
		
		switch (kw.text) {
		case "title": {
			int mark = toks.at();
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			errors.logParsingToken(new ExprToken(pos, ExprToken.STRING, s).original(toks.fromMark(mark)));
			consumer.title(s);
			return new NoNestingParser(errors);
		}
		case "baseuri": {
			int mark = toks.at();
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			errors.logParsingToken(new ExprToken(pos, ExprToken.STRING, s).original(toks.fromMark(mark)));
			consumer.baseuri(s);
			return new NoNestingParser(errors);
		}
		case "routes": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			if (this.routing != null) {
				errors.message(kw.location, "cannot specify routing table twice");
				return new IgnoreNestedParser(errors);
			}
			routing = new ApplicationRouting(errors, kw.location, namer.assemblyName(null), namer.assemblyName("Routing"));
			consumer.routes(routing);
			return new TDARoutingParser(errors, routing);
		}
		default: {
			errors.message(toks, "expected 'title', 'baseuri' or 'routes'");
			return new IgnoreNestedParser(errors);
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (routing == null)
			errors.message(startPos, "assembly must declare routing");
		else if (!routing.sawMainCard)
			errors.message(startPos, "assembly must identify a main card");
	}

}
