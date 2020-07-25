package doc.grammar;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.flasck.flas.grammar.RepoChecker;
import org.flasck.flas.grammar.SentenceData;
import org.flasck.flas.grammar.SentenceProducer;
import org.junit.Test;
import org.zinutils.utils.FileUtils;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class RandomSentenceTest {

	@Test
	public void testRandomSentenceProduction() throws Throwable {
//		final int seed = 24465;
		final int seed = 21007;
		File td = Files.createTempDirectory("flas").toFile();
		File fd = new File(td, "test.r" + seed);
		FileUtils.assertDirectory(fd);
		File repoFile = File.createTempFile("repo", ".txt");
		SentenceProducer p = new SentenceProducer(fd, "/gh-grammar/grammar.xml");
//		p.debugMode();
		AtomicBoolean failed = new AtomicBoolean(false);
		Map<String, String> matchers = new TreeMap<>();
		final Consumer<SentenceData> collector = used -> { matchers.putAll(used.matchers); };
		p.sentence(seed, "source-file", collector);
		p.sentence(seed, "unit-test-file", collector);
//		FileUtils.cat(new File(fd, "test.r" + seed + "/r" + seed + ".fl"));
//		FileUtils.cat(new File(fd, "test.r" + seed + "/r" + seed + ".ut"));
//		System.out.println("MSS = " + matchers);
		try {
			boolean f = org.flasck.flas.Main.standardCompiler(new String[] { "--phase", "PARSING", "--dumprepo", repoFile.getPath(), fd.toString() });
			if (!f) {
				if (!RepoChecker.checkRepo(repoFile, matchers))
					failed.set(true);
			} else {
				System.out.println("Compilation failed");
				failed.set(true);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			failed.set(true);
		}
		FileUtils.deleteDirectoryTree(td);
		repoFile.delete();
		assertFalse(failed.get());
	}
}
