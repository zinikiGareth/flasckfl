package org.flasck.flas.errors;

import java.io.File;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.tokenizers.Tokenizable;

// This tracks errors and expects a Mock ErrorReporter
// It will pass through errors being raised, but will answer
// the question of "have errors been raised" purely locally,
// thus allowing tests to handle that
public class LocalErrorTracker implements ErrorReporter {
	private final ErrorReporter other;
	boolean seenLocalErrors = false;
	private Set<AtomicBoolean> marks = new HashSet<>();
	
	public LocalErrorTracker(ErrorReporter other) {
		this.other = other;
	}

	@Override
	public void showFromMark(ErrorMark mark, Writer pw, int ind) {
		other.showFromMark(mark, pw, ind);
	}

	public ErrorReporter message(InputPosition pos, String msg) {
		seenErrors();
		return other.message(pos, msg);
	}

	@Override
	public ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg) {
		seenErrors();
		return other.message(pos, locs, msg);
	}

	public ErrorReporter message(Tokenizable line, String msg) {
		seenErrors();
		return other.message(line, msg);
	}

	public ErrorReporter message(FLASError e) {
		seenErrors();
		return other.message(e);
	}

	public void seenErrors() {
		seenLocalErrors = true;
		for (AtomicBoolean b : marks)
			b.set(true);
	}

	@Override
	public ErrorReporter reportException(Throwable ex) {
		seenErrors();
		return other.reportException(ex);
	}

	public void fakeErrorWithoutNeedingAssertion() {
		seenErrors();
	}
	
	public boolean hasErrors() {
		return seenLocalErrors;
	}

	@Override
	public ErrorMark mark() {
		AtomicBoolean ab = new AtomicBoolean(false);
		marks.add(ab);
		return new ErrorMark() {
			@Override
			public boolean hasMoreNow() {
				return ab.get();
			}
		};
	}

	public void track(File f) {
		other.track(f);
	}

	@Override
	public <T extends LoggableToken> T logParsingToken(T token) {
		return other.logParsingToken(token);
	}

	@Override
	public void logReduction(String ruleId, Locatable from, Locatable to) {
		logReduction(ruleId, from.location(), to.location());
	}

	@Override
	public void logReduction(String ruleId, InputPosition from, InputPosition to) {
		other.logReduction(ruleId, from, to);
	}
}
