package org.flasck.flas.errors;

import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public interface ErrorReporter {

	ErrorReporter message(InputPosition pos, String msg);
	ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg);
	ErrorReporter message(Tokenizable line, String msg);
	ErrorReporter message(FLASError e);
	ErrorReporter reportException(Throwable ex);

	boolean hasErrors();
	ErrorMark mark();
	void showFromMark(ErrorMark mark, Writer pw, int ind);

	// LSP related features
	default void connect(LanguageClient client) {}
	default void beginProcessing(URI uri) {}
	default void doneProcessing() {}
	default List<URI> getAllBrokenURIs() { return new ArrayList<>(); }
}
