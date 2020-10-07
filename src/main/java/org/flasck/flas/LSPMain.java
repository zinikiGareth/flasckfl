package org.flasck.flas;

import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.lsp.FLASLanguageServer;

/** The Language Server main class */
public class LSPMain {

	public static void run() {
        InputStream in = System.in; // socket.getInputStream();
        OutputStream out = System.out; // socket.getOutputStream();

        FLASLanguageServer server = new FLASLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);

        launcher.startListening();
	}

}
