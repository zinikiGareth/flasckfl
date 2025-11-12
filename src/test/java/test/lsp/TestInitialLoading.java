package test.lsp;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.flasck.flas.lsp.FLASLanguageClient;
import org.flasck.flas.lsp.FLASLanguageServer;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

public class TestInitialLoading {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	
	@Test
	public void loadingAnEmptyWorkspace() {
		File flasHome = null;
		FLASLanguageServer server = new FLASLanguageServer(flasHome);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		InitializeParams params = new InitializeParams();
		List<WorkspaceFolder> wfs = new ArrayList<WorkspaceFolder>();
		params.setWorkspaceFolders(wfs);
		server.initialize(params);
	}

	@Test
	public void loadingAnWorkspaceWithOneProject() {
		File flasHome = null;
		FLASLanguageServer server = new FLASLanguageServer(flasHome);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		MessageParams mp = new MessageParams(MessageType.Log, "opening root /fred/bert");
		context.checking(new Expectations() {{
			oneOf(client).logMessage(mp);
		}});
		
		InitializeParams params = new InitializeParams();
		List<WorkspaceFolder> wfs = new ArrayList<WorkspaceFolder>();
		wfs.add(new WorkspaceFolder("file:///fred/bert", "bert"));
		params.setWorkspaceFolders(wfs);
		server.initialize(params);
	}
}
