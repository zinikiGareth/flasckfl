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
import org.flasck.flas.tokenizers.CommentToken;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ExprReducerErrors implements ErrorReporter {
	public class Composite {
		private final LoggableToken tok;
		private final String ruleId;
		private final Locatable first;
		private final Locatable last;
		private final List<Composite> collect;

		public <T extends LoggableToken> Composite(T token) {
			this.tok = token;
			this.ruleId = null;
			this.first = null;
			this.last = null;
			this.collect = null;
		}
		
		public Composite(String ruleId, Locatable first, Locatable last, List<Composite> collect) {
			this.tok = null;
			this.ruleId = ruleId;
			this.first = first;
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
	private final boolean reduceToOne;

	public ExprReducerErrors(ErrorReporter errors, boolean reduceToOne) {
		this.errors = errors;
		this.reduceToOne = reduceToOne;
	}

	public void track(File f) {
		errors.track(f);
	}

	public <T extends LoggableToken> T logParsingToken(T token) {
		// For reconstruction purposes, we need to log the token, but we don't want it involved in reductions
		if (token instanceof CommentToken) {
			errors.logParsingToken(token);
			return token;
		}
		
		linear.put(token.location(), new Composite(token));
		return token;
	}

	// Remove a token when we back up
	public void cancel(ExprToken tok) {
		linear.remove(tok.location);
	}

	public void logReduction(String ruleId, Locatable first, Locatable last) {
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
		linear.put(first.location(), new Composite(ruleId, first, last, collect));
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
	}

	public void doneReducing() {
		if (reduceToOne && linear.size() > 1) {
			for (Entry<InputPosition, Composite> e : linear.entrySet())
				System.out.println(e.getKey() + " ====> " + e.getValue());
			throw new CantHappenException("Not fully reduced: " + linear.size());
		}
		
		// Because of "reduceToOne", there may actually be more than one, but in that case generate them in order anyway
		for (Entry<InputPosition, Composite> e : linear.entrySet()) {
			dumpTreeInPostfixOrder(e.getValue());
		}
	}

	private void dumpTreeInPostfixOrder(Composite comp) {
		if (comp.tok != null) {
			errors.logParsingToken(comp.tok);
		} else {
			for (Composite e : comp.collect)
				dumpTreeInPostfixOrder(e);
			errors.logReduction(comp.ruleId, comp.first, comp.last);
		}
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
