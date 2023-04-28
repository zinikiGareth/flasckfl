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
import org.flasck.flas.lsp.LSPCore;
import org.flasck.flas.lsp.LSPErrorForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The Language Server main class */
public class LSPMain {
	static final Logger logger = LoggerFactory.getLogger("FLASLSP");

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
			logger.info("launching LSP server to listen on port " + port);
			try (ServerSocket sock = new ServerSocket(port)) {
				while (true) {
					Socket accept = sock.accept();
					logger.info("LSP server accepted connection on " + accept.getLocalPort());
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
		LSPCore core = new LSPCore(errors, flasHome);
        FLASLanguageServer server = new FLASLanguageServer(core);
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, in, out);

        LanguageClient client = launcher.getRemoteProxy();
        errors.connect(client);

        launcher.startListening();
	}

}
