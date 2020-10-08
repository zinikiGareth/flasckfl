package org.flasck.flas.lsp;

import java.io.File;

import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.parser.assembly.BuildAssembly;

public class CompileFA extends AbstractCompilation implements CompileFile {
	public CompileFA(LanguageClient client, File file) {
		super(client, file);
	}

	@Override
	public void run() {
		ParsingPhase fap = new ParsingPhase(errors, inPkg, new BuildAssembly(errors, repository));
		client.logMessage(new MessageParams(MessageType.Log, "compiling " + file.getName() + " in " + inPkg));
		fap.process(file);
	}
}
