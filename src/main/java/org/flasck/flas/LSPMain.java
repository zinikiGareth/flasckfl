package org.flasck.flas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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

	public static void run(String[] args) {
		if (args.length > 1) {
			// args[1] is a port
			int port = Integer.parseInt(args[1]);
			try (ServerSocket sock = new ServerSocket(port)) {
				while (true) {
					Socket accept = sock.accept();
					launchServer(accept.getInputStream(), accept.getOutputStream());
				}
			} catch (IOException ex) {
				ex.printStackTrace(System.out);
			}
		} else {
			launchServer(System.in, System.out);
		}

	}

	private static void launchServer(InputStream in, OutputStream out) {
        Repository repository = new Repository();
        LSPErrorForwarder errors = new LSPErrorForwarder();
        LoadBuiltins.applyTo(errors, repository);
        FLASCompiler compiler = new FLASCompiler(errors, repository);
        FLASLanguageServer server = new FLASLanguageServer(errors, repository, compiler);
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);
        errors.connect(client);

        launcher.startListening();
	}

}
