package org.flasck.flas.errors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.utils.Justification;

public class ErrorResult extends FatErrorAPI implements ErrorReporter, Iterable<FLASError> {
	private final Set<FLASError> errors = new TreeSet<FLASError>();
	private final File saveParsingTokens;
	private PrintWriter tokenStream;

	public ErrorResult() {
		this(null);
	}
	
	public ErrorResult(File saveParsingTokens) {
		this.saveParsingTokens = saveParsingTokens;
	}
	
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
		if (em == null)
			mark = new Marker(true);
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
	
	@Override
	public void track(File f) {
		System.out.println("    " + f.getName());
		if (tokenStream != null) {
			tokenStream.close();
			tokenStream = null;
		}
		try {
			if (saveParsingTokens != null)
				tokenStream = new PrintWriter(new File(saveParsingTokens, f.getName()));
		} catch (FileNotFoundException ex) {
			System.err.println("could not open " + saveParsingTokens);
			tokenStream = null;
		}
	}

	@Override
	public <T extends LoggableToken> T logParsingToken(T token) {
		if (tokenStream != null) {
			logLocation(token.location());
			tokenStream.println("token " + token.type() + " " + token.text());
		}
		return token;
	}

	@Override
	public void logReduction(String ruleId, Locatable from, Locatable to) {
		logReduction(ruleId, from.location(), to.location());
	}

	@Override
	public void logReduction(String ruleId, InputPosition from, InputPosition to) {
		if (tokenStream != null) {
			logLocation(from);
			tokenStream.println("reduction " + ruleId);
			logLocation(to);
		}
	}

	private void logLocation(InputPosition pos) {
		tokenStream.print(pos.lineNo + ":");
		if (pos.indent != null)
			tokenStream.print(pos.indent.tabs+"."+pos.indent.spaces);
		else
			tokenStream.print("0.0");
		tokenStream.println(":" + pos.off);
	}
	
	public void closeTokenStream() {
		if (tokenStream != null)
			tokenStream.close();
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
