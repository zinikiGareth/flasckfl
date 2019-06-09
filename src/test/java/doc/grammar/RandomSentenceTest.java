package doc.grammar;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;
import org.zinutils.utils.FileUtils;

// The idea here is to produce random sentences according to the grammar and see what happens.
// In general, a valid sentence according to the grammar should at least parse
// (there are many reasons it wouldn't get further, like undefined references, but I can't see why it wouldn't parse short of hitting limits of one kind or another)
public class RandomSentenceTest {

	@Test
	public void testRandomSentenceProduction() throws Throwable {
		File td = Files.createTempDirectory("flas").toFile();
		SentenceProducer p = new SentenceProducer(td, "/gh-grammar/grammar.xml");
		File tmp = p.sentence(23254, used -> System.out.println(used));
		FileUtils.cat(tmp);
		boolean failed = org.flasck.flas.Main.noExit(new String[] { "--phase", "PARSING", td.toString() });
		assertFalse(failed);
		FileUtils.deleteDirectoryTree(td);
	}
}
