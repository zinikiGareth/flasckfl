package org.flasck.flas.lsp;

import java.net.URI;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.flasck.flas.errors.FLASError;

public class DiagnosticHandler {

	protected Diagnostic makeDiagnostic(URI backupUri, FLASError e) {
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
        	diagnostic.setSource(backupUri.toString());
        	diagnostic.setRange(new Range(new Position(1, 1), new Position(2, 1)));
        }
        diagnostic.setMessage(e.msg);
		return diagnostic;
	}

}
