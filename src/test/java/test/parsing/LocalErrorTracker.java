package test.parsing;

import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
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

	public void merge(ErrorReporter o) {
		seenErrors();
		other.merge(o);
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

			@Override
			public boolean contains(FLASError e) {
				throw new org.zinutils.exceptions.NotImplementedException();
			}
		};
	}
}
