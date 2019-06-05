package org.flasck.flas.errors;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.utils.Justification;

public class ErrorResult implements ErrorReporter, Iterable<FLASError> {
	private final Set<FLASError> errors = new TreeSet<FLASError>();

	public int count() {
		return errors.size();
	}
	
	public ErrorResult message(FLASError e) {
		errors.add(e);
		return this;
	}
	
	public ErrorResult message(Block b, String msg) {
		return message(new Tokenizable(b), msg);
	}
	
	public ErrorResult message(Tokenizable line, String msg) {
		return message(line.realinfo(), msg);
	}

	public ErrorResult message(InputPosition pos, String msg) {
		return message(new FLASError(pos, msg));
	}

	public ErrorResult reportException(Throwable ex) {
		final StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String st = sw.toString();
		int idx = st.indexOf('\n');
		idx = st.indexOf('\n', idx+1);
		return message((InputPosition)null, st.substring(0, idx).replaceAll("\n", " "));
	}

	public void merge(ErrorReporter from) {
		errors.addAll(((ErrorResult)from).errors);
	}
	
	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public boolean moreErrors(int mark) {
		return errors.size() > mark;
	}

	public FLASError get(int i) {
		return CollectionUtils.nth(errors, i);
	}
	
	public void showFromMark(ErrorMark mark, Writer pw, int ind) {
		try {
			for (FLASError e : errors) {
				if (mark.contains(e))
					continue;
				for (int i=0;i<ind;i++)
					pw.append(' ');
				if (e.loc != null) {
					pw.write(Justification.PADRIGHT.format(e.loc + ": ", 22) + (e.loc.text == null ? "" : e.loc.text.substring(0, e.loc.off) + " _ " + e.loc.text.substring(e.loc.off)));
					pw.write('\n');
				} else
					pw.write("<unknown location>\n");
				for (int i=0;i<ind;i++)
					pw.append(' ');
				pw.write(Justification.PADRIGHT.format("", 26));
				pw.write(e.msg);
				pw.write('\n');
			}
			pw.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void showTo(Writer pw, int ind) throws IOException {
		showFromMark(new ErrorMark(null), pw, ind);
	}
	
	public ErrorMark mark() {
		return new ErrorMark(this);
	}

	public String singleString() throws IOException {
		Writer w = new StringWriter();
		showTo(w, 0);
		return w.toString();
	}
	public static ErrorResult oneMessage(Tokenizable line, String msg) {
		return new ErrorResult().message(line, msg);
	}

	public static ErrorResult oneMessage(InputPosition location, String msg) {
		return new ErrorResult().message(location, msg);
	}

	@Override
	public String toString() {
		try {
			return "ErrorResult[" + singleString() + "]";
		} catch (IOException ex) {
			return ex.toString();
		}
	}

	@Override
	public Iterator<FLASError> iterator() {
		return errors.iterator();
	}
}
