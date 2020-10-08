package org.flasck.flas.lsp;

import java.net.URI;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.assembly.BuildAssembly;
import org.flasck.flas.repository.Repository;

public class CompileFA extends AbstractCompilation {
	public CompileFA(URI uri) {
		super(uri);
	}

	@Override
	public void compile(LanguageClient client, ErrorReporter errors, Repository repository) {
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		client.logMessage(new MessageParams(MessageType.Log, "compiling " + file.getName() + " in " + inPkg));
		fap.process(file);
	}
}
