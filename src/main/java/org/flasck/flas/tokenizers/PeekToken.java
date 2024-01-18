package org.flasck.flas.tokenizers;

import org.flasck.flas.errors.ErrorReporter;

public class PeekToken {

	public static boolean is(ErrorReporter errors, Tokenizable line, String lookFor) {
		int mark = line.at();
		line.skipWS(errors);
		boolean ret = line.still(lookFor.length()) && line.getTo(lookFor.length()).equals(lookFor);
		line.reset(mark);
		return ret;
	}

	public static boolean accept(ErrorReporter errors, Tokenizable line, String lookFor) {
		int mark = line.at();
		line.skipWS(errors);
		if (line.still(lookFor.length()) && line.getTo(lookFor.length()).equals(lookFor))
			return true;
		line.reset(mark);
		return false;
	}

}
