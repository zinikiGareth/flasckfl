package org.flasck.flas.testing.golden;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.flasck.flas.testing.golden.ParsedTokens.ReductionRule;
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
			for (GrammarToken t : toks.tokens()) {
				System.out.println(t);
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
		
		Map<InputPosition, ReductionRule> mostReduced = new TreeMap<>();
		for (ReductionRule rr : toks.reductions()) {
			System.out.println(rr);
			ReductionRule mr = null;
			for (GrammarToken t : toks.tokens()) {
				if (rr.includes(t.pos)) {
					if (mr != null && mr.includes(t.pos))
						continue;
					mr = null;
					if (mostReduced.containsKey(t.pos)) {
						mr = mostReduced.get(t.pos);
						System.out.println("  !! " + mr);
						continue;
					}
					System.out.println("  " + t);
				}
			}
			Iterator<Entry<InputPosition, ReductionRule>> it = mostReduced.entrySet().iterator();
			while (it.hasNext()) {
				Entry<InputPosition, ReductionRule> e = it.next();
				if (rr.includes(e.getKey()))
					it.remove();
			}
			mostReduced.put(rr.start(), rr);
		}
		System.out.println("most reduced = " + mostReduced.values());
	}

	public void checkGrammar() {
		if (parseTokens == null)
			return;
	}

}
