package org.flasck.flas.lsp;

import java.io.Writer;
import java.util.Collection;

import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.tokenizers.Tokenizable;

public class LSPErrorForwarder implements ErrorReporter {
	private LanguageClient client;

	public LSPErrorForwarder() {
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	@Override
	public ErrorReporter message(InputPosition pos, String msg) {
		return this;
	}

	@Override
	public ErrorReporter message(InputPosition pos, Collection<InputPosition> locs, String msg) {
		return this;
	}

	@Override
	public ErrorReporter message(Tokenizable line, String msg) {
		return this;
	}

	@Override
	public ErrorReporter message(FLASError e) {
		return this;
	}

	@Override
	public ErrorReporter reportException(Throwable ex) {
		return this;
	}

	@Override
	public void merge(ErrorReporter o) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasErrors() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ErrorMark mark() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showFromMark(ErrorMark mark, Writer pw, int ind) {
		// TODO Auto-generated method stub
		
	}

}
