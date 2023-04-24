package org.flasck.flas.lsp;

import java.net.URI;
import java.util.List;

import org.flasck.flas.errors.FLASError;

public interface LSPErrorHandler {

	void handle(FLASError e);

	void done(List<URI> broken);

	int errorCount();

}
