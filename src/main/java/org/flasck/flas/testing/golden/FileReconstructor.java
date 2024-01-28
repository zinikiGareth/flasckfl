package org.flasck.flas.testing.golden;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.flasck.flas.tokenizers.FreeTextToken;
import org.zinutils.exceptions.WrappedException;

public class FileReconstructor {
	private final ParsedTokens toks;
	private final File output;
	int lineNo = 1;
	boolean indented = false;
	int offset = 0;

	public FileReconstructor(ParsedTokens toks, File output) {
		this.toks = toks;
		this.output = output;
	}

	public void reconstruct() {
		try (PrintWriter pw = new PrintWriter(output)) {
			for (GrammarToken t : toks.tokens()) {
				showToken(pw, t);
			}
			pw.println();
		} catch (FileNotFoundException e) {
			throw WrappedException.wrap(e);
		}
	}

	private void showToken(PrintWriter pw, GrammarToken t) {
//				System.out.println(t);
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

}
