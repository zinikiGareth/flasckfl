package org.flasck.flas.tokenizers;

public class PeekToken {

	public static boolean is(Tokenizable line, String lookFor) {
		int mark = line.at();
		line.skipWS();
		boolean ret = line.still(lookFor.length()) && line.getTo(lookFor.length()).equals(lookFor);
		line.reset(mark);
		return ret;
	}

	public static boolean accept(Tokenizable line, String lookFor) {
		int mark = line.at();
		line.skipWS();
		if (line.still(lookFor.length()) && line.getTo(lookFor.length()).equals(lookFor))
			return true;
		line.reset(mark);
		return false;
	}

}
