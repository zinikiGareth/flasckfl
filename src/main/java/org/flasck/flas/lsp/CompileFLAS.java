package org.flasck.flas.lsp;

import java.net.URI;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.repository.Repository;

public class CompileFLAS extends AbstractCompilation {
	public CompileFLAS(URI uri) {
		super(uri);
	}

	@Override
	public void compile(LanguageClient client, ErrorReporter errors, Repository repository) {
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer)repository);
		client.logMessage(new MessageParams(MessageType.Log, "compiling " + file.getName() + " in " + inPkg));
		flp.process(file);
	}
}
