package org.flasck.flas.parser;

import java.io.File;
import java.io.Writer;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class ExprReducerErrors implements ErrorReporter {
	private final ErrorReporter errors;

	public ExprReducerErrors(ErrorReporter errors) {
		this.errors = errors;
	}

	public void track(File f) {
		errors.track(f);
	}

	public <T extends LoggableToken> T logParsingToken(T token) {
		System.out.println("token " + token.location() + ": " + token);
		return errors.logParsingToken(token);
	}

	public void logReduction(String ruleId, Locatable first, Locatable last) {
		System.out.println("reduce " + ruleId + " " + first + " - " + last);
		errors.logReduction(ruleId, first, last);
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
