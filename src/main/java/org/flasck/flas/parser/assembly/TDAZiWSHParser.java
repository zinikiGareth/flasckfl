package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDAZiWSHParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ZiwshConsumer consumer;

	public TDAZiWSHParser(ErrorReporter errors, ZiwshConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "expected ziwsh action keyword");
			return new IgnoreNestedParser(errors);
		}
		toks.skipWS(errors);

		switch (kw.text) {
		case "wsuri": {
			int mark = toks.at();
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			if (s == null) {
				errors.message(toks, "must specify a websocket uri");
				return new IgnoreNestedParser(errors);
			}
			ExprToken tok = new ExprToken(pos, ExprToken.STRING, s).original(toks.fromMark(mark));
			errors.logParsingToken(tok);
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}

			consumer.wsuri(tok);
			errors.logReduction("fa-ziwsh-wsuri", kw.location, pos);

			return new NoNestingParser(errors);
		}
		case "security": {
			ValidIdentifierToken mod = VarNameToken.from(errors, toks);
			if (mod == null) {
				errors.message(toks, "must specify a security module");
				return new IgnoreNestedParser(errors);
			}
			errors.logParsingToken(mod);
			ValidIdentifierToken clz = ValidIdentifierToken.from(errors, toks);
			if (clz == null) {
				errors.message(toks, "must specify a security module class");
				return new IgnoreNestedParser(errors);
			}
			errors.logParsingToken(clz);

			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}

			consumer.security(mod, clz);
			errors.logReduction("fa-ziwsh-security", kw.location, clz.location());

			return new NoNestingParser(errors);
		}
		case "loginflow": {
			int mark = toks.at();
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			if (s == null) {
				errors.message(toks, "must specify a login uri");
				return new IgnoreNestedParser(errors);
			}
			ExprToken tok = new ExprToken(pos, ExprToken.STRING, s).original(toks.fromMark(mark));
			errors.logParsingToken(tok);
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}

			consumer.loginflow(tok);
			errors.logReduction("fa-ziwsh-loginflow", kw.location, pos);

			return new NoNestingParser(errors);
		}

		default:
			errors.message(toks, "expected 'wsuri', 'security' or 'loginFlow'");
			return new IgnoreNestedParser(errors);
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
