package org.flasck.flas.lsp;

import java.io.Writer;
import java.net.URI;
import java.util.List;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.errors.FatErrorAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LSPErrorForwarder extends FatErrorAPI implements ErrorReporter {
	private static final Logger logger = LoggerFactory.getLogger("FLASLSP");
	private LanguageClient client;
	private LSPErrorHandler handler;

	public LSPErrorForwarder() {
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	public void beginPhase1(URI uri) {
		logger.info("beginning processing for " + uri);
		handler = new Phase1Handler(client, uri);
	}
	
	@Override
	public void beginPhase2(URI uri) {
		logger.info("beginning phase2 processing for " + uri);
		handler = new Phase2Handler(client, uri.resolve("."));
	}

	@Override
	public ErrorReporter message(FLASError e) {
		if (handler == null) {
			logger.info("message arrived with no handler: " + e);
		} else {
			handler.handle(e);
		}
		return this;
	}

	public void doneProcessing(List<URI> broken) {
		handler.done(broken);
		handler = null;
	}

	@Override
	public boolean hasErrors() {
		if (handler == null)
			return false;
		return handler.errorCount() > 0;
	}

	@Override
	public ErrorMark mark() {
		int cnt = handler == null ? 0 : handler.errorCount();
		return new ErrorMark() {
			@Override
			public boolean hasMoreNow() {
				return handler.errorCount() > cnt;
			}
		};
	}

	@Override
	public void showFromMark(ErrorMark mark, Writer pw, int ind) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void logMessage(String s) {
		synchronized (client) {
			client.logMessage(new MessageParams(MessageType.Log, s));
		}
	}

}
