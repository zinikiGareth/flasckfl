package org.flasck.flas.lsp;

import java.io.File;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;

public class CompileFLAS extends AbstractCompilation implements CompileFile {
	public CompileFLAS(LanguageClient client, File file) {
		super(client, file);
	}

	@Override
	public void run() {
		ParsingPhase flp = new ParsingPhase(errors, inPkg, (TopLevelDefinitionConsumer)repository);
		client.logMessage(new MessageParams(MessageType.Log, "compiling " + file.getName() + " in " + inPkg));
		flp.process(file);
	}
}
