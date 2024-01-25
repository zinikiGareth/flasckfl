package org.flasck.flas.parser.st;

import java.net.URI;
import java.net.URISyntaxException;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class AjaxCreateActionsParser extends BlockLocationTracker implements TDAParsing {
	private final AjaxCreate ac;
	protected InputPosition lastInner;

	public AjaxCreateActionsParser(ErrorReporter errors, AjaxCreate ac, LocationTracker locTracker) {
		super(errors, locTracker);
		this.ac = ac;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		switch (kw.text) {
		case "subscribe": {
			int mark = toks.at();
			InputPosition loc = toks.realinfo();
			String sl = StringToken.from(errors, toks);
			if (sl == null) {
				errors.message(toks, "subscribe requires a path");
				return new IgnoreNestedParser(errors);
			}
			errors.logParsingToken(new ExprToken(loc, ExprToken.STRING, sl).original(toks.fromMark(mark)));
			try {
				URI uri = new URI(sl);
				if (uri.getScheme() != null || uri.getHost() != null || uri.getPort() != -1 || uri.getFragment() != null) {
					errors.message(loc, "only path permitted");
					return new IgnoreNestedParser(errors);
				}
				if (uri.getPath() == null || uri.getPath().equals("")) {
					errors.message(loc, "subscribe requires a path");
					return new IgnoreNestedParser(errors);
				}
				if (!uri.getPath().startsWith("/")) {
					errors.message(loc, "path must be absolute");
					return new IgnoreNestedParser(errors);
				}
			} catch (URISyntaxException ex) {
				errors.message(loc, "invalid path uri: " + sl);
				return new IgnoreNestedParser(errors);
			}
			StringLiteral pathUrl = new StringLiteral(loc, sl);
			lastInner = kw.location;
			errors.logReduction("ajax-subscribe-line", kw.location, pathUrl.location);
			AjaxSubscribe sub = new AjaxSubscribe(kw.location, pathUrl);
			ac.subscribe(errors, sub);
			return new TDAParsingWithAction(
				new AjaxSubscribeOptionsParser(errors, sub, this),
//				() -> {
					reduction(kw.location, "ajax-subscribe-block")
//					errors.logReduction("ajax-subscribe-block", kw.location, lastInner);
//					locTracker.updateLoc(kw.location);
//				}
					);
		}
		default: {
			errors.message(kw.location, "unrecognized ajax action " + kw.text);
			return new IgnoreNestedParser(errors);
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
