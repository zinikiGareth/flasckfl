package org.flasck.flas.errors;

import java.io.File;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.grammar.tracking.LoggableToken;
import org.flasck.flas.tokenizers.Tokenizable;

public interface ErrorReporter {
	void track(File f);
	<T extends LoggableToken> T logParsingToken(T token);
	void logReduction(String ruleId, Locatable first, Locatable last);
	void logReduction(String ruleId, InputPosition from, InputPosition to);
	ErrorReporter message(InputPosition pos, String msg);
	ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg);
	ErrorReporter message(Tokenizable line, String msg);
	ErrorReporter message(FLASError e);
	ErrorReporter reportException(Throwable ex);

	boolean hasErrors();
	ErrorMark mark();
	void showFromMark(ErrorMark mark, Writer pw, int ind);

	// LSP related features
	default void beginPhase1(URI uri) {}
	default void doneProcessing(List<URI> broken) {}
	default List<URI> getAllBrokenURIs() { return new ArrayList<>(); }
	default void logMessage(String s) { }
	default void beginPhase2(URI uri) { }
}
