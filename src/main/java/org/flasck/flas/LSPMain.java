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
		LSPErrorForwarder errors = new LSPErrorForwarder();
		Configuration config = new Configuration(errors, new String[] {});
        Repository repository = new Repository();
		FLASCompiler compiler = new FLASCompiler(config, errors, repository);
		LSPTaskQueue taskQ = new LSPTaskQueue();
		compiler.taskQueue(taskQ);
        compiler.loadFLIM();
        FLASLanguageServer server = new FLASLanguageServer(compiler, taskQ);
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        server.connect(client);
        compiler.connect(client);

        launcher.startListening();
	}

}
