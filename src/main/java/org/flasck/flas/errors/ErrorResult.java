package org.flasck.flas.errors;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.utils.Justification;

public class ErrorResult extends FatErrorAPI implements ErrorReporter, Iterable<FLASError> {
	private final Set<FLASError> errors = new TreeSet<FLASError>();

	public int count() {
		return errors.size();
	}
	
	@Override
	public ErrorResult message(FLASError e) {
		errors.add(e);
		return this;
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
	
	public void showFromMark(ErrorMark em, Writer pw, int ind) {
		Marker mark = (Marker) em;
		try {
			for (FLASError e : errors) {
				if (mark.contains(e))
					continue;
				for (int i=0;i<ind;i++)
					pw.append(' ');
				InputPosition l = e.loc;
				if (l != null) {
					showLine(pw, ind, l);
				}
				for (InputPosition li : e.others)
					showLine(pw, ind, li);
				pw.write(Justification.PADRIGHT.format("", 26));
				pw.write(e.msg);
				pw.write('\n');
			}
			pw.flush();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void showLine(Writer pw, int ind, InputPosition l) throws IOException {
		pw.write(Justification.PADRIGHT.format(l + ": ", 22) + (l.text == null ? "" : l.text.substring(0, l.off) + " _ " + l.text.substring(l.off)));
		pw.write('\n');
		for (int i=0;i<ind;i++)
			pw.append(' ');
	}
	
	public void showTo(Writer pw, int ind) throws IOException {
		showFromMark(new Marker(false), pw, ind);
	}
	
	public ErrorMark mark() {
		return new Marker(true);
	}

	public String singleString() throws IOException {
		Writer w = new StringWriter();
		showTo(w, 0);
		return w.toString();
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
	
	private class Marker implements ErrorMark {
		private Set<FLASError> have = new TreeSet<>();

		public Marker(boolean includeCurrent) {
			if (includeCurrent)
				for (FLASError e : errors)
					have.add(e);
		}

		@Override
		public boolean hasMoreNow() {
			return count() > have.size();
		}
		
		public boolean contains(FLASError e) {
			return have.contains(e);
		}
	}

}
