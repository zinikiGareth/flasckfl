package test.lsp;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.flasck.flas.compiler.CompileUnit;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lsp.Root;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;
import org.zinutils.hfs.FakeHFSFolder;
import org.zinutils.hfs.FakeHierarchicalFileSystem;
import org.zinutils.hfs.HFSFolderMatcher;

public class TestUiCollectionDir {
	@Rule public JUnitRuleMockery context = new JUnitRuleMockery();
	FakeHierarchicalFileSystem hfs = new FakeHierarchicalFileSystem();
	ErrorReporter errors = context.mock(ErrorReporter.class);
	CompileUnit compiler = context.mock(CompileUnit.class);

	@Test
	public void noFilesMeansNoUIDir() throws URISyntaxException {
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hfs.provideFolder(hff);

		URI uri = new URI("file:/fred/bert/");
		Root r = new Root(null, errors, null, hfs, uri);
		r.gatherFiles();
	}

	@Test
	public void oneFileMeansParentUIDir() throws URISyntaxException {
		context.checking(new Expectations() {{
			allowing(errors).logMessage(with(any(String.class)));
			oneOf(compiler).setCardsFolder(with(HFSFolderMatcher.uri(new URI("file:/fred/bert/ui/"))));
		}});
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/index.html"));
		hfs.provideFolder(hff);

		URI uri = new URI("file:/fred/bert/");
		Root r = new Root(null, errors, null, hfs, uri);
		r.useCompiler(compiler);
		r.gatherFiles();
	}

	@Test
	public void twoFilesInTheSameDirStillMeansParentUIDir() throws URISyntaxException {
		context.checking(new Expectations() {{
			allowing(errors).logMessage(with(any(String.class)));
			oneOf(compiler).setCardsFolder(with(HFSFolderMatcher.uri(new URI("file:/fred/bert/ui/"))));
		}});
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/index.html"));
		hff.subfolder("ui").provideFile("cards.html", new File("src/test/resources/lsp-files/card.html"));
		hfs.provideFolder(hff);

		URI uri = new URI("file:/fred/bert/");
		Root r = new Root(null, errors, null, hfs, uri);
		r.useCompiler(compiler);
		r.gatherFiles();
	}

	@Test
	public void twoFilesInTwoSubdirsMeansCommonParentUIDir() throws URISyntaxException {
		context.checking(new Expectations() {{
			allowing(errors).logMessage(with(any(String.class)));
			oneOf(compiler).setCardsFolder(with(HFSFolderMatcher.uri(new URI("file:/fred/bert/ui/"))));
		}});
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
		hff.subfolder("ui").subfolder("html").provideFile("index.html", new File("src/test/resources/lsp-files/index.html"));
		hff.subfolder("ui").subfolder("css").provideFile("web.css", new File("src/test/resources/lsp-files/web.css"));
		hfs.provideFolder(hff);

		URI uri = new URI("file:/fred/bert/");
		Root r = new Root(null, errors, null, hfs, uri);
		r.useCompiler(compiler);
		r.gatherFiles();
	}
}
