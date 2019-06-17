package doc.grammar;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.zinutils.utils.FileUtils;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class RandomSentenceTest {

	@Test
	public void testRandomSentenceProduction() throws Throwable {
		final int seed = 22302;
		File td = Files.createTempDirectory("flas").toFile();
		File fd = new File(td, "test.r" + seed);
		FileUtils.assertDirectory(fd);
		File repoFile = File.createTempFile("repo", ".txt");
		SentenceProducer p = new SentenceProducer(fd, "/gh-grammar/grammar.xml");
		p.debugMode();
		AtomicBoolean failed = new AtomicBoolean(false);
		p.sentence(seed, used -> {
			try {
				Map<String, String> ms = used.matchers;
				FileUtils.cat(used.file);
				boolean f = org.flasck.flas.Main.noExit(new String[] { "--phase", "PARSING", "--dumprepo", repoFile.getPath(), fd.toString() });
				if (!f) {
					if (!RepoChecker.checkRepo(repoFile, ms))
						failed.set(true);
				} else {
					System.out.println("Compilation failed");
					failed.set(true);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				failed.set(true);
			}
		});
		assertFalse(failed.get());
		FileUtils.deleteDirectoryTree(td);
		repoFile.delete();
	}
}
