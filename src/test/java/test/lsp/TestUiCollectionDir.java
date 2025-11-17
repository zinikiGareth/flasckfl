package test.lsp;

import java.net.URI;
import java.net.URISyntaxException;

import org.flasck.flas.lsp.Root;
import org.junit.Test;
import org.zinutils.hfs.FakeHFSFolder;
import org.zinutils.hfs.FakeHierarchicalFileSystem;

public class TestUiCollectionDir {
	FakeHierarchicalFileSystem hfs = new FakeHierarchicalFileSystem();

	@Test
	public void noFilesMeansNoUIDir() throws URISyntaxException {
		FakeHFSFolder hff = new FakeHFSFolder(new URI("file:///fred/bert/"));
//		hff.subfolder("ui").provideFile("index.html", new File("src/test/resources/lsp-files/index.html"));
		hfs.provideFolder(hff);

		URI uri = new URI("file:/fred/bert/");
		Root r = new Root(null, null, null, hfs, uri);
		r.gatherFiles();
	}
}
