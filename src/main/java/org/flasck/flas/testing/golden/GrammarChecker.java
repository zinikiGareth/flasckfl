package org.flasck.flas.testing.golden;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
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

	public void checkParseTokenLogic(boolean expectErrors) {
		if (parseTokens == null || reconstruct == null)
			return;
		for (File f : FileUtils.findFilesMatching(parseTokens, "*")) {
			ParsedTokens toks = ParsedTokens.read(f);
			reconstructFile(toks, new File(reconstruct, f.getName()));
			if (!expectErrors)
				computeReductions(toks);
		}
	}

	private void reconstructFile(ParsedTokens toks, File output) {
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
	}
	
	// TODO: this should:
	// (a) internally assert that everything was reduced to a TLF
	// (b) internally assert that every token was part of some reduction
	// (c) internally assert that the final rules do not overlap
	// (c) return an orchard of reductions & tokens with a TLF at the top of each tree and tokens at the leaves
	private GrammarOrchard computeReductions(ParsedTokens toks) {
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
				if (rr.includes(e.getKey())) {
					it.remove();
				}
			}
			mostReduced.put(rr.start(), rr);
		}
		
		// Assert that they do not overlap
		InputPosition lastEndedAt = null;
		for (ReductionRule rr : mostReduced.values()) {
			if (lastEndedAt != null && lastEndedAt.compareTo(rr.start()) >= 0)
				fail("overlapping reductions: " + lastEndedAt + " X " + rr);
			lastEndedAt = rr.last();
		}
		
		// TODO: assert that all of these are TLFs
//		System.out.println("most reduced = " + mostReduced.values());
		
		for (ReductionRule rr : mostReduced.values()) {
			if (rr.start().indent.tabs != 1 || rr.start().indent.spaces != 0) {
				// This cannot be a TLF
				fail("TLFs must have an indent of (1,0): " + rr);
			}
			// TODO: check against the master list of TLF names
		}
		
		tokenLoop:
		for (GrammarToken t : toks.tokens()) {
			for (ReductionRule rr : toks.reductions()) {
				if (rr.includes(t.pos))
					continue tokenLoop;
			}
			
			// comments are, for want of a better word, TLFs.
			if (t.type.equals("comment"))
				continue;
			
			// TODO: this is an error
			fail("token not reduced: " + t);
		}

		// TODO: return an orchard of reductions with a TLF at the top of each tree
		GrammarOrchard ret = new GrammarOrchard();
		
		// the reduction rules are in order, and the tokens too ...
		List<GrammarStep> srstack = new ArrayList<>();
		Iterator<GrammarToken> tokens = toks.tokens().iterator();
		Iterator<ReductionRule> rules = toks.reductions().iterator();
		Iterator<ReductionRule> mit = mostReduced.values().iterator();
		ReductionRule rr = null;
		ReductionRule mr = null;
		GrammarToken nt = null;
		while (mr != null || mit.hasNext()) {
			if (mr == null)
				mr = mit.next();

			if (rr == null && rules.hasNext())
				rr = rules.next();

			// should we just shift a token
			if (nt != null || tokens.hasNext()) {
				if (nt == null)
					nt = tokens.next();
				if (nt.isComment()) {
					nt = null;
					continue;
				}
//				System.out.println(nt);
	
				if (rr.includes(nt.pos) || rr.location().compareTo(nt.pos) > 0) {
					srstack.add(0, nt);
					nt = null;
					continue;
				}
			}
			
			// Now we need to do the reductions, if any
//			System.out.println("considering rule " + rr + " with " + srstack);
			GrammarTree tree = new GrammarTree(rr);
			while (!srstack.isEmpty()) {
				GrammarStep si = srstack.get(0);
//				System.out.println("trying to reduce " + si + " with " + rr);
				if (rr.includes(si.location())) {
					tree.push(si);
					srstack.remove(0);
				} else
					break;
			}
			srstack.add(0, tree);
			if (rr == mr) {
				ret.add(tree);
				mr = null;
			}
			rr = null;
		}
		
		PrintWriter pw = new PrintWriter(System.out);
		ret.dump(pw);
		pw.flush();
		
		return ret;
	}

	public void checkGrammar() {
		if (parseTokens == null)
			return;
	}

}
