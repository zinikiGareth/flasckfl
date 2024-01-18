package org.flasck.flas.tokenizers;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;

public class StringToken {

	/* Parse a string literal using either single or double quotes (must be paired)
	 * until the matching quote is found.  Allow the containing quotes to be included
	 * in the string if doubled, e.g. 'Fred''s world' is the same as "Fred's world"
	 */
	public static String from(ErrorReporter errors, Tokenizable line) {
		line.skipWS(errors);
		if (!line.hasMore())
			return null;
		char oq = line.nextChar();
		if (oq != '"' && oq != '\'')
			return null;
		InputPosition start = line.realinfo();
		int actualLine = line.actualLine();
		
		StringBuilder ret = new StringBuilder();
		while (true) {
			line.advance();
			int mark = line.at();
			while (line.hasMore() && line.nextChar() != oq) {
				if (line.actualLine() > actualLine) {
					errors.message(start, "unterminated string");
					return null;
				}
				line.advance();
			}
			if (!line.hasMore()) {
				errors.message(start, "unterminated string");
				return null;
			}
			if (line.at() > mark)
				ret.append(line.fromMark(mark));
			line.advance();
			if (!line.hasMore() || line.nextChar() != oq)
				return ret.toString();
			ret.append(oq);
		}
	}

}
