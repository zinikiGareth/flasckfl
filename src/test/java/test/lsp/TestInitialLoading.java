package test.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
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

import com.google.gson.JsonObject;

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
		
		server.getWorkspaceService().executeCommand(new ExecuteCommandParams("flas/readyForNotifications", new ArrayList<>()));
		
		server.waitForTaskQueueToDrain();
	}

	@Test
	public void loadingAWorkspaceWithOneEmptyProject() throws InterruptedException, URISyntaxException {
		hfs.provideFolder(new FakeHFSFolder(new URI("file:///fred/bert/")));
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		MessageParams mp = new MessageParams(MessageType.Log, "opening root /fred/bert/");
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

		server.getWorkspaceService().executeCommand(new ExecuteCommandParams("flas/readyForNotifications", new ArrayList<>()));
		
		server.waitForTaskQueueToDrain();
	}

	@Test
	public void loadingAWorkspaceWithOneProjectWithASingleFLFile() throws InterruptedException, URISyntaxException {
		States finished = context.states("finished").startsAs("waiting");
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("flas").subfolder("org.zinutils.main").provideFile("basic.fl", new File("src/test/resources/lsp-files/basic.fl"));
		hfs.provideFolder(hff);
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		MessageParams mp = new MessageParams(MessageType.Log, "opening root /fred/bert/");
		MessageParams gmp = new MessageParams(MessageType.Log, "gathered basic.fl");
		MessageParams c1mp = new MessageParams(MessageType.Log, "compiling basic.fl in org.zinutils.main");
		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		PublishDiagnosticsParams p1dp = new PublishDiagnosticsParams("file:/fred/bert/flas/org.zinutils.main/basic.fl", new ArrayList<>());
		PublishDiagnosticsParams pdp2 = new PublishDiagnosticsParams("file:/fred/bert/flas/org.zinutils.main/", new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(client).logMessage(mp);
			oneOf(client).logMessage(gmp);
			oneOf(client).logMessage(c1mp);
			oneOf(client).publishDiagnostics(pdp); when(finished.is("waiting")); then(finished.is("started"));
			oneOf(client).publishDiagnostics(pdp2); when(finished.is("started")); then(finished.is("done"));
			oneOf(client).publishDiagnostics(p1dp);
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

	@Test
	public void loadingAWorkspaceWithOneProjectWithASingleHTMLFile() throws InterruptedException, URISyntaxException {
		States finished = context.states("finished").startsAs("waiting");
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/index.html"));
		hfs.provideFolder(hff);
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		MessageParams mp = new MessageParams(MessageType.Log, "opening root /fred/bert/");
		MessageParams gmp = new MessageParams(MessageType.Log, "gathered index.html");
		MessageParams amp = new MessageParams(MessageType.Log, "analyzing file file:/fred/bert/ui/index.html");
		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		PublishDiagnosticsParams pdp1 = new PublishDiagnosticsParams("file:/fred/bert/ui/", new ArrayList<>());
		context.checking(new Expectations() {{
			oneOf(client).logMessage(mp);
			oneOf(client).logMessage(gmp);
			oneOf(client).logMessage(amp);
			oneOf(client).sendCardInfo(with(CardInfoMatcher.ui("file:///fred/bert")));
			oneOf(client).publishDiagnostics(pdp); 
			oneOf(client).publishDiagnostics(pdp1); then(finished.is("done"));
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

	@Test
	public void anErrorOccursWhenYouHaveACardWithNoUI() throws InterruptedException, URISyntaxException {
		States finished = context.states("finished").startsAs("waiting");
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("flas").subfolder("org.zinutils.main").provideFile("main.fl", new File("src/test/resources/lsp-files/hello.fl"));
		hfs.provideFolder(hff);
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		PublishDiagnosticsParams p1dp = new PublishDiagnosticsParams("file:/fred/bert/flas/org.zinutils.main/main.fl", new ArrayList<>());
		PublishDiagnosticsParams pdp2 = new PublishDiagnosticsParams("file:/fred/bert/flas/org.zinutils.main/", new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(client).logMessage(with(any(MessageParams.class)));
			oneOf(client).publishDiagnostics(pdp); when(finished.is("waiting")); then(finished.is("started"));
			oneOf(client).publishDiagnostics(p1dp);
			oneOf(client).publishDiagnostics(with(
					PDPMatcher
						.uri("file:/fred/bert/flas/org.zinutils.main/main.fl")
						.diagnostic(1, 11, 16, "there is no web template defined for hello")
			));
			oneOf(client).publishDiagnostics(pdp2); when(finished.is("started")); then(finished.is("done"));
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

	@Test
	public void loadingAWorkspaceWithOneProjectWithACard() throws InterruptedException, URISyntaxException {
		States finished = context.states("finished").startsAs("waiting");
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("flas").subfolder("org.zinutils.main").provideFile("main.fl", new File("src/test/resources/lsp-files/hello.fl"));
		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/card.html"));
		hfs.provideFolder(hff);
		FLASLanguageServer server = new FLASLanguageServer(hfs);
		FLASLanguageClient client = context.mock(FLASLanguageClient.class);
		server.provide(client);

		PublishDiagnosticsParams pdp = new PublishDiagnosticsParams("file:///fred/bert/", new ArrayList<>());
		PublishDiagnosticsParams pdp1 = new PublishDiagnosticsParams("file:/fred/bert/", new ArrayList<>());
		PublishDiagnosticsParams p1dp = new PublishDiagnosticsParams("file:/fred/bert/flas/org.zinutils.main/main.fl", new ArrayList<>());
		PublishDiagnosticsParams pdp2 = new PublishDiagnosticsParams("file:/fred/bert/flas/org.zinutils.main/", new ArrayList<>());
		context.checking(new Expectations() {{
			allowing(client).logMessage(with(any(MessageParams.class)));
			oneOf(client).publishDiagnostics(pdp); when(finished.is("waiting")); then(finished.is("started"));
			oneOf(client).publishDiagnostics(p1dp);
//			oneOf(client).publishDiagnostics(pdp1); when(finished.is("started")); // then(finished.is("done"));
			oneOf(client).sendCardInfo(with(CardInfoMatcher.ui("file:///fred/bert").info("hello", new JsonObject())));
			oneOf(client).publishDiagnostics(pdp2); then(finished.is("done"));
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
