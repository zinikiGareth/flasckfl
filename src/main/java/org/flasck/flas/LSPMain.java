package org.flasck.flas;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.flasck.flas.lsp.FLASLanguageServer;
import org.flasck.flas.lsp.LSPErrorForwarder;

/** The Language Server main class */
public class LSPMain {

	public static void run(String[] args) {
		String fh = System.getenv("FLAS_HOME");
		if (fh == null) {
			System.out.println("FLAS_HOME not set");
			return;
		}
		File flasHome = new File(fh);
		if (args.length > 1) {
			// args[1] is a port
			int port = Integer.parseInt(args[1]);
			try (ServerSocket sock = new ServerSocket(port)) {
				while (true) {
					Socket accept = sock.accept();
					launchServer(flasHome, accept.getInputStream(), accept.getOutputStream());
				}
			} catch (IOException ex) {
				ex.printStackTrace(System.out);
			}
		} else {
			launchServer(flasHome, System.in, System.out);
		}
	}

	private static void launchServer(File flasHome, InputStream in, OutputStream out) {
		LSPErrorForwarder errors = new LSPErrorForwarder();
        FLASLanguageServer server = new FLASLanguageServer(errors, flasHome);
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        errors.connect(client);

        launcher.startListening();
	}

}
