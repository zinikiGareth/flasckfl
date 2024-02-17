package org.flasck.flas.parser;

import java.io.File;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ExprReducerErrors implements ErrorReporter {
	public class Composite {
		private final LoggableToken tok;
		private final String ruleId;
		private final List<Composite> collect;
		private final Locatable last;

		public <T extends LoggableToken> Composite(T token) {
			this.tok = token;
			this.ruleId = null;
			this.last = null;
			this.collect = null;
		}
		
		public Composite(String ruleId, Locatable last, List<Composite> collect) {
			this.tok = null;
			this.ruleId = ruleId;
			this.last = last;
			this.collect = collect;
		}

		@Override
		public String toString() {
			if (tok != null) {
				return "T[" + tok + "]";
			} else if (ruleId != null) {
				return "R[" + ruleId + ":" + collect + "]";
			} else
				return super.toString();
		}
	}

	private final ErrorReporter errors;
	private final Map<InputPosition, Composite> linear = new TreeMap<>();

	public ExprReducerErrors(ErrorReporter errors) {
		this.errors = errors;
	}

	public void track(File f) {
		errors.track(f);
	}

	public <T extends LoggableToken> T logParsingToken(T token) {
		errors.logParsingToken(token); // DELETE ME!
		
		
		System.out.println("token " + token.location() + ": " + token);
		linear.put(token.location(), new Composite(token));
		return token;
	}

	public void logReduction(String ruleId, Locatable first, Locatable last) {
		errors.logReduction(ruleId, first, last); // DELETE ME

		
		System.out.println("reduce " + ruleId + " " + first.location() + " - " + last.location() + " " + first + " -- " + last);
		System.out.println("before: " + linear);
		Iterator<Entry<InputPosition, Composite>> it = linear.entrySet().iterator();
		List<Composite> collect = new ArrayList<>();
		boolean collecting = false;
		while (it.hasNext()) {
			Entry<InputPosition, Composite> e = it.next();
			InputPosition loc = e.getKey();
			Composite curr = e.getValue();

			if (loc.equals(first.location())) {
				collecting = true;
			} else if (!collecting && loc.compareTo(first.location()) > 0) {
				throw new CantHappenException("write a message");
			}
			if (collecting) {
				collect.add(curr);
				it.remove();
			} else {
				continue;
			}
			if (loc.equals(last.location())) {
				collecting = false;
				break;
			} else if (curr.last != null && last.location().equals(curr.last.location())) {
				collecting = false;
				break;
			} else if (loc.compareTo(last.location()) > 0) {
				throw new CantHappenException("the token on the stack is past where we should have found the last token");
			}
		}
		if (collecting) {
			throw new CantHappenException("should be false");
		}
		if (collect.isEmpty()) {
			throw new CantHappenException("nothing matched");
		}
		linear.put(first.location(), new Composite(ruleId, last, collect));
		System.out.println("after: " + linear);
	}

	public void logReduction(String ruleId, InputPosition from, InputPosition to) {
		try {
			throw new NotImplementedException("don't use this form in expression reduction");
		} catch (NotImplementedException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public void cancelReduction() {
		System.out.println("cancelled");
	}

	public void doneReducing() {
		System.out.println("done reducing");
		System.out.println(linear);
//		if (linear.size() != 1)
//			throw new CantHappenException("Not fully reduced");
	}

	public ErrorReporter message(InputPosition pos, String msg) {
		return errors.message(pos, msg);
	}

	public ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg) {
		return errors.message(pos, locs, msg);
	}

	public ErrorReporter message(Tokenizable line, String msg) {
		return errors.message(line, msg);
	}

	public ErrorReporter message(FLASError e) {
		return errors.message(e);
	}

	public ErrorReporter reportException(Throwable ex) {
		return errors.reportException(ex);
	}

	public boolean hasErrors() {
		return errors.hasErrors();
	}

	public ErrorMark mark() {
		return errors.mark();
	}

	public void showFromMark(ErrorMark mark, Writer pw, int ind) {
		errors.showFromMark(mark, pw, ind);
	}

	public void beginPhase1(URI uri) {
		errors.beginPhase1(uri);
	}

	public void doneProcessing(List<URI> broken) {
		errors.doneProcessing(broken);
	}

	public List<URI> getAllBrokenURIs() {
		return errors.getAllBrokenURIs();
	}

	public void logMessage(String s) {
		errors.logMessage(s);
	}

	public void beginPhase2(URI uri) {
		errors.beginPhase2(uri);
	}
}
