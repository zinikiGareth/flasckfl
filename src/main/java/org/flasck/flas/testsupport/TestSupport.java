package org.flasck.flas.testsupport;

import java.net.URI;

import org.flasck.flas.blockForm.ContinuedLine;
import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.SingleLine;
import org.flasck.flas.tokenizers.Tokenizable;

public class TestSupport {

	public static ContinuedLine line(String string) {
		ContinuedLine ret = new ContinuedLine();
		ret.lines.add(new SingleLine(URI.create("file:/fred"), 1, new Indent(1,0), string));
		return ret;
	}

	public static Tokenizable tokline(String string) {
		return new Tokenizable(line(string));
	}

}
