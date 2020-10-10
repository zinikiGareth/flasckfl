package org.flasck.flas.lsp;

import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.FLASError;
import org.flasck.flas.errors.FatErrorAPI;

public class LSPErrorForwarder extends FatErrorAPI implements ErrorReporter {
	private LanguageClient client;
	private URI uri;
	private List<Diagnostic> diagnostics;

	public LSPErrorForwarder() {
	}

	public void connect(LanguageClient client) {
		this.client = client;
	}

	public void beginProcessing(URI uri) {
		this.uri = uri;
		diagnostics = new ArrayList<>();
	}
	
	@Override
	public ErrorReporter message(FLASError e) {
		System.out.println(e);
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        if (e.loc != null) {
        	diagnostic.setSource(e.loc.file);
        	int line = e.loc.lineNo-1;
        	int ind = 0;
        	if (e.loc.indent != null)
        		ind = e.loc.indent.tabs + e.loc.indent.spaces;
			diagnostic.setRange(new Range(new Position(line, ind + e.loc.off), new Position(line, ind + e.loc.locAtEnd().off)));
        } else {
        	diagnostic.setSource(uri.toString());
        	diagnostic.setRange(new Range(new Position(1, 1), new Position(2, 1)));
        }
        diagnostic.setMessage(e.msg);
        diagnostics.add(diagnostic);
		return this;
	}

	public void doneProcessing(List<URI> broken) {
		if (diagnostics.isEmpty())
			broken.remove(uri);
		else
			broken.add(uri);
		synchronized (client) {
			client.publishDiagnostics(new PublishDiagnosticsParams(uri.toString(), diagnostics));
		}
	}
	
	@Override
	public boolean hasErrors() {
		return !diagnostics.isEmpty();
	}

	@Override
	public ErrorMark mark() {
		int cnt = diagnostics.size();
		return new ErrorMark() {
			@Override
			public boolean hasMoreNow() {
				return diagnostics.size() > cnt;
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
