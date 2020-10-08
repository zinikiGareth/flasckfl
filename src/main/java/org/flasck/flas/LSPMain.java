package org.flasck.flas;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.lsp.FLASLanguageServer;
import org.flasck.flas.lsp.LSPErrorForwarder;
import org.flasck.flas.repository.LoadBuiltins;
import org.flasck.flas.repository.Repository;

/** The Language Server main class */
public class LSPMain {

	public static void run() {
        InputStream in = System.in; // socket.getInputStream();
        OutputStream out = System.out; // socket.getOutputStream();

        Repository repository = new Repository();
        LSPErrorForwarder errors = new LSPErrorForwarder();
        LoadBuiltins.applyTo(errors, repository);
        FLASCompiler compiler = new FLASCompiler(errors, repository);
        FLASLanguageServer server = new FLASLanguageServer(repository, compiler);
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);
        errors.connect(client);

        launcher.startListening();
	}

}
