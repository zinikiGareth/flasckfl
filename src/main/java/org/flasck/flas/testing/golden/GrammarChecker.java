package org.flasck.flas.testing.golden;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.zinutils.exceptions.WrappedException;
import org.zinutils.utils.FileUtils;

public class GrammarChecker {
	private final File parseTokens;
	private final File reconstruct;

	public GrammarChecker(File parseTokens, File reconstruct) {
		this.parseTokens = parseTokens;
		this.reconstruct = reconstruct;
	}

	public void checkParseTokenLogic() {
		if (parseTokens == null || reconstruct == null)
			return;
		for (File f : FileUtils.findFilesMatching(parseTokens, "*")) {
			reconstructFile(f, new File(reconstruct, f.getName()));
		}
	}

	private void reconstructFile(File tokens, File output) {
		ParsedTokens toks = ParsedTokens.read(tokens);
		try (PrintWriter pw = new PrintWriter(output)) {
			int lineNo = 1;
			boolean indented = false;
			int offset = 0;
			for (GrammarToken t : toks) {
				while (t.lineNo() > lineNo) {
					pw.println();
					lineNo++;
					indented = false;
					offset = 0;
				}
				if (!indented) {
					for (int i=0;i<t.tabs();i++) {
						pw.print("\t");
					}
					for (int i=0;i<t.spaces();i++) {
						pw.print(" ");
					}
					indented = true;
				}
				while (offset < t.offset()) {
					pw.print(" ");
					offset++;
				}
				pw.print(t.text);
				offset += t.text.length();
			}
			pw.println();
		} catch (FileNotFoundException e) {
			throw WrappedException.wrap(e);
		}
	}

	public void checkGrammar() {
		if (parseTokens == null)
			return;
	}

}
