package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.assembly.ApplicationRouting;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
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
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null) {
			errors.message(toks, "expected assembly keyword");
			return new IgnoreNestedParser();
		}
		
		switch (kw.text) {
		case "title": {
			String s = StringToken.from(errors, toks);
			consumer.title(s);
			return new NoNestingParser(errors);
		}
		case "routes": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			if (this.routing != null) {
				errors.message(kw.location, "cannot specify routing table twice");
				return new IgnoreNestedParser();
			}
			routing = new ApplicationRouting(errors, kw.location, namer.assemblyName(null), namer.assemblyName("Routing"), consumer);
			consumer.routes(routing);
			return new TDARoutingParser(errors, routing);
		}
		default: {
			errors.message(toks, "expected 'title' or 'routes'");
			return new IgnoreNestedParser();
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
