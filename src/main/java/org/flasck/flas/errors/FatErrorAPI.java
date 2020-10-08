package org.flasck.flas.errors;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public abstract class FatErrorAPI implements ErrorReporter {
	public abstract ErrorReporter message(FLASError e);

	public ErrorReporter message(Tokenizable line, String msg) {
		return message(line.realinfo(), msg);
	}

	public ErrorReporter message(InputPosition pos, String msg) {
		return message(new FLASError(pos, msg));
	}

	@Override
	public ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg) {
		FLASError e = new FLASError(pos, msg);
		if (locs != null)
			e.others.addAll(locs);
		return message(e);
	}

	public ErrorReporter reportException(Throwable ex) {
		final StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String st = sw.toString();
		int idx = st.indexOf('\n');
		idx = st.indexOf('\n', idx+1);
		return message((InputPosition)null, st.substring(0, idx).replaceAll("\n", " "));
	}
}
