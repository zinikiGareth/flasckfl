package test.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
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

public class TestSaveHTML {
	protected Synchroniser synchronizer = new Synchroniser();
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery() {{
		setThreadingPolicy(synchronizer);
	}};
	
	FakeHierarchicalFileSystem hfs = new FakeHierarchicalFileSystem();

	@Test
	public void loadingAWorkspaceWithOneProjectWithASingleHTMLFile() throws InterruptedException, URISyntaxException {
		States finished = context.states("finished").startsAs("waiting");
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/index.html"));
		hfs.provideFolder(hff);
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		PublishDiagnosticsParams pdpih = new PublishDiagnosticsParams("file:/fred/bert/ui/index.html", new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(client).logMessage(with(any(MessageParams.class)));
			oneOf(client).sendCardInfo(with(CardInfoMatcher.ui("file:///fred/bert")));
			oneOf(client).publishDiagnostics(pdp);
			oneOf(client).publishDiagnostics(pdpih);then(finished.is("initialized"));
		}});
		
		InitializeParams params = new InitializeParams();
		List<WorkspaceFolder> wfs = new ArrayList<WorkspaceFolder>();
		wfs.add(new WorkspaceFolder("file:///fred/bert", "bert"));
		params.setWorkspaceFolders(wfs);
		server.initialize(params);
		
		server.getWorkspaceService().executeCommand(new ExecuteCommandParams("flas/readyForNotifications", new ArrayList<>()));

		synchronizer.waitUntil(finished.is("initialized"), 1000);

		PublishDiagnosticsParams pdpih2 = new PublishDiagnosticsParams("file:///fred/bert/ui/index.html", new ArrayList<>());
		context.checking(new Expectations() {{
//			oneOf(client).sendCardInfo(with(CardInfoMatcher.ui("file:///fred/bert")));
			oneOf(client).publishDiagnostics(pdpih2);then(finished.is("done"));
//			oneOf(client).publishDiagnostics(pdp1); 
		}});
		
		TextDocumentIdentifier tdi = new TextDocumentIdentifier("file:///fred/bert/ui/index.html");
		VersionedTextDocumentIdentifier vtdi = new VersionedTextDocumentIdentifier("file:///fred/bert/ui/index.html", 13);
		List<TextDocumentContentChangeEvent> changes = new ArrayList<>();
		changes.add(new TextDocumentContentChangeEvent(null, ""));
		DidChangeTextDocumentParams dctdp = new DidChangeTextDocumentParams(vtdi, changes);
		DidSaveTextDocumentParams dstdp = new DidSaveTextDocumentParams(tdi);
		server.getTextDocumentService().didChange(dctdp);
		server.getTextDocumentService().didSave(dstdp);
		
		synchronizer.waitUntil(finished.is("done"), 1000);
		server.waitForTaskQueueToDrain();
	}

}
