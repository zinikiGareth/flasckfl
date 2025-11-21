package test.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.flasck.flas.lsp.FLASLanguageClient;
import org.flasck.flas.lsp.FLASLanguageServer;
import org.jmock.Expectations;
import org.jmock.States;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.hfs.FakeHFSFolder;
import org.zinutils.hfs.FakeHierarchicalFileSystem;

public class ErrorReportingTests {
	protected Synchroniser synchronizer = new Synchroniser();
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery() {{
		setThreadingPolicy(synchronizer);
	}};
	
	FakeHierarchicalFileSystem hfs = new FakeHierarchicalFileSystem();

	@Test
	public void testInvalidCardDefinitionTypeIsReported() throws URISyntaxException, InterruptedException {
		States finished = context.states("finished").startsAs("waiting");
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/invalidCardType.html"));
		hfs.provideFolder(hff);
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(client).logMessage(with(any(MessageParams.class)));
			oneOf(client).publishDiagnostics(pdp);
			oneOf(client).sendCardInfo(with(CardInfoMatcher.ui("file:///fred/bert")));
			oneOf(client).publishDiagnostics(with(PDPMatcher.uri("file:/fred/bert/ui/index.html").diagnostic(6, 17, 34, "invalid flas- id tag: invalid"))); then(finished.is("done"));
		}});
		
		InitializeParams params = new InitializeParams();
		List<WorkspaceFolder> wfs = new ArrayList<WorkspaceFolder>();
		wfs.add(new WorkspaceFolder("file:///fred/bert", "bert"));
		params.setWorkspaceFolders(wfs);
		server.initialize(params);

		server.getWorkspaceService().executeCommand(new ExecuteCommandParams("flas/readyForNotifications", new ArrayList<>()));
		
		synchronizer.waitUntil(finished.is("done"), 1000);
		server.waitForTaskQueueToDrain();
	}

}
