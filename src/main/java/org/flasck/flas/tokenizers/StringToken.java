package org.flasck.flas.tokenizers;

public class StringToken {

	/* Parse a string literal using either single or double quotes (must be paired)
	 * until the matching quote is found.  Allow the containing quotes to be included
	 * in the string if doubled, e.g. 'Fred''s world' is the same as "Fred's world"
	 */
	public static String from(Tokenizable line) {
		if (!line.hasMore())
			return null;
		char oq = line.nextChar();
		if (oq != '"' && oq != '\'')
			return null;
		
		StringBuilder ret = new StringBuilder();
		while (true) {
			line.advance();
			int mark = line.at();
			while (line.hasMore() && line.nextChar() != oq) {
				line.advance();
			}
			if (!line.hasMore())
				return null;
			if (line.at() > mark)
				ret.append(line.fromMark(mark));
			line.advance();
			if (!line.hasMore() || line.nextChar() != oq)
				return ret.toString();
			ret.append(oq);
		}
	}

}
