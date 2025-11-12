package test.lsp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.flasck.flas.lsp.FLASLanguageClient;
import org.flasck.flas.lsp.FLASLanguageServer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.hfs.FakeHierarchicalFileSystem;

public class TestInitialLoading {
	protected Synchroniser synchronizer = new Synchroniser();
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery() {{
		setThreadingPolicy(synchronizer);
	}};
	
	FakeHierarchicalFileSystem hfs = new FakeHierarchicalFileSystem();
	
	@Test
	public void loadingAnEmptyWorkspace() throws InterruptedException {
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		InitializeParams params = new InitializeParams();
		List<WorkspaceFolder> wfs = new ArrayList<WorkspaceFolder>();
		params.setWorkspaceFolders(wfs);
		server.initialize(params);
		server.waitForTaskQueueToDrain();
	}

	@Test
	public void loadingAnWorkspaceWithOneProject() throws InterruptedException {
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		MessageParams mp = new MessageParams(MessageType.Log, "opening root /fred/bert");
		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(client).logMessage(mp);
			oneOf(client).publishDiagnostics(pdp);
		}});
		
		InitializeParams params = new InitializeParams();
		List<WorkspaceFolder> wfs = new ArrayList<WorkspaceFolder>();
		wfs.add(new WorkspaceFolder("file:///fred/bert", "bert"));
		params.setWorkspaceFolders(wfs);
		server.initialize(params);
		server.waitForTaskQueueToDrain();
	}
}
