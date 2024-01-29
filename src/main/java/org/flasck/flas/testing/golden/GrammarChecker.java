package org.flasck.flas.testing.golden;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.GrammarSupport;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.flasck.flas.testing.golden.ParsedTokens.ReductionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.utils.FileUtils;

public class GrammarChecker {
	public static final Logger logger = LoggerFactory.getLogger("GrammarChecker");
	
	public class MyPreferredTestSorting implements Comparator<String> {
		String[] exts = { ".fl", ".fa", ".ut", ".st" };
		@Override
		public int compare(String lhs, String rhs) {
			String lext = FileUtils.extension(lhs);
			String rext = FileUtils.extension(rhs);
			int lp = Arrays.binarySearch(exts, 0, exts.length, lext);
			int rp = Arrays.binarySearch(exts, 0, exts.length, rext);
			if (lp == -1 || rp == -1)
				throw new CantHappenException("can't find " + lext + " = " + lp + " or " + rext + " = " + rp);
			if (lp < rp)
				return -1;
			else if (rp > lp)
				return 1;
			else
				return lhs.compareTo(rhs);
		}
	}

	private final File parseTokens;
	private final File reconstruct;
	private final Grammar grammar;

	public GrammarChecker(File parseTokens, File reconstruct) {
		this.parseTokens = parseTokens;
		this.reconstruct = reconstruct;
		this.grammar = GrammarSupport.loadGrammar();
	}

	public Map<String, GrammarTree> checkParseTokenLogic(boolean expectErrors) {
		if (parseTokens == null || reconstruct == null)
			return null;
		Map<String, GrammarTree> ret = new TreeMap<>(new MyPreferredTestSorting());
		for (File f : FileUtils.findFilesMatching(parseTokens, "*")) {
			ParsedTokens toks = ParsedTokens.read(f);
			toks.write(new File(f.getParentFile(), f.getName()+"-sorted"));
			reconstructFile(toks, new File(reconstruct, f.getName()));
			if (!expectErrors) {
				String ext = FileUtils.extension(f.getName());
				GrammarTree reduced = computeReductions(getTopRule(ext), toks);
				dumpTree(reduced);
				ret.put(f.getName(), reduced);
			}
		}
		return ret;
	}

	private void reconstructFile(ParsedTokens toks, File output) {
		FileReconstructor r = new FileReconstructor(toks, output);
		r.reconstruct();
	}
	
	// TODO: this should:
	// (a) internally assert that everything was reduced to a TLF
	// (b) internally assert that every token was part of some reduction
	// (c) internally assert that the final rules do not overlap
	// (c) return an orchard of reductions & tokens with a TLF at the top of each tree and tokens at the leaves
	private GrammarTree computeReductions(String topRule, ParsedTokens toks) {
		Map<InputPosition, ReductionRule> mostReduced = calculateMostReduced(toks);
		assertNoOverlappingRules(mostReduced);
		assertTLFs(mostReduced);
		assertAllTokensReduced(toks);
		return reduceToSingleTree(topRule, toks, mostReduced);
	}

	private Map<InputPosition, ReductionRule> calculateMostReduced(ParsedTokens toks) {
		Map<InputPosition, ReductionRule> mostReduced = new TreeMap<>();
		for (ReductionRule rr : toks.reductions()) {
//			System.out.println(rr);
			ReductionRule mr = null;
			for (GrammarToken t : toks.tokens()) {
				if (rr.includes(t.pos)) {
					if (mr != null && mr.includes(t.pos))
						continue;
					mr = null;
					if (mostReduced.containsKey(t.pos)) {
						mr = mostReduced.get(t.pos);
//						System.out.println("  !! " + mr);
						continue;
					}
//					System.out.println("  " + t);
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
		return mostReduced;
	}

	private void assertNoOverlappingRules(Map<InputPosition, ReductionRule> mostReduced) {
		InputPosition lastEndedAt = null;
		for (ReductionRule rr : mostReduced.values()) {
			if (lastEndedAt != null && lastEndedAt.compareTo(rr.start()) >= 0)
				fail("overlapping reductions: " + lastEndedAt + " X " + rr);
			lastEndedAt = rr.last();
		}
	}

	private void assertTLFs(Map<InputPosition, ReductionRule> mostReduced) {
		for (ReductionRule rr : mostReduced.values()) {
			if (rr.start().indent.tabs != 1 || rr.start().indent.spaces != 0) {
				// This cannot be a TLF
				fail("TLFs must have an indent of (1,0): " + rr);
			}
			// TODO: check against the master list of TLF names
		}
	}

	private void assertAllTokensReduced(ParsedTokens toks) {
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
	}

	private GrammarTree reduceToSingleTree(String topRule, ParsedTokens toks, Map<InputPosition, ReductionRule> mostReduced) {
		List<GrammarTree> ret = new ArrayList<>();
		
		// the reduction rules are in order, and the tokens too ...
		List<GrammarStep> srstack = new ArrayList<>();
		Iterator<GrammarToken> tokens = toks.tokens().iterator();
		Iterator<ReductionRule> rules = toks.reductions().iterator();
		Iterator<ReductionRule> mit = mostReduced.values().iterator();
		ReductionRule rr = null;
		ReductionRule mr = null;
		GrammarToken nt = null;
		while (mr != null || mit.hasNext()) {
			if (mr == null) {
				mr = mit.next();
//				System.out.println("mr = " + mr);
			}

			if (rr == null && rules.hasNext()) {
				rr = rules.next();
//				System.out.println("considering rule " + rr);
			}

			// should we just shift a token
			if (nt != null || tokens.hasNext()) {
				if (nt == null)
					nt = tokens.next();
				if (nt.isComment()) {
					nt = null;
					continue;
				}
//				System.out.println("have token " + nt);
	
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
		
		return new GrammarTree(topRule, ret);
	}

	public void dumpTree(GrammarTree top) {
		PrintWriter pw = new PrintWriter(System.out);
		top.dump(pw, "", false);
		pw.flush();
	}

	public void checkGrammar(Map<String, GrammarTree> fileOrchards) {
		for (Entry<String, GrammarTree> e : fileOrchards.entrySet()) {
			String name = e.getKey();
			String ext = FileUtils.extension(name);
			String topRule = getTopRule(ext);
			logger.info("checking file " + name + " against " + topRule);
			checkProductionsAgainstGrammar(e.getValue(), topRule);
		}
	}

	private String getTopRule(String ext) {
		switch (ext) {
		case ".fl":
			return "source-file";
		case ".fa":
			return "assembly-unit";
		case ".ut":
			return "unit-test-unit";
		case ".st":
			return "system-test-unit";
		default:
			throw new CantHappenException("there is no top rule for file type " + ext);
		}
	}

	private void checkProductionsAgainstGrammar(GrammarTree file, String currRule) {
		Production grammarRule = grammar.findRule("file");
		if (grammarRule == null)
			throw new CantHappenException("there is no rule in the grammar for the file production");
		if (!(grammarRule instanceof OrProduction))
			throw new CantHappenException("file rule is not an OrProduction");
		OrProduction options = (OrProduction) grammarRule;
		List<Definition> choices = options.allOptions();
		RefDefinition defn = null;
		for (Definition d : choices) {
			if (d instanceof RefDefinition && ((RefDefinition)d).refersTo(currRule))
				defn = (RefDefinition) d;
		}
		if (defn == null)
			throw new CantHappenException("couldn't find a case in file rule for " + currRule);
		
		//		Iterator<GrammarStep> it = trees.iterator();
		/*		String reducedTo = tree.reducedToRule();
		if (reducedTo.equals(currRule)) {
			recursivelyCompareItems(grammarRule.defn, tree);
		} else {
			System.out.println("Looking for rule " + reducedTo + " inside " + currRule);
			Production use = searchDownTo(grammarRule, reducedTo);
			if (use == null)
				fail("cannot find " + reducedTo + " inside the grammar rule for " + currRule);
		}
*/
//		System.out.println(grammarRule);
		DefinitionIterator defnItr = new DefinitionIterator(grammar, currRule, defn);
		logger.info("have defn " + defnItr.current());
		recursivelyCompareItems(defnItr, Arrays.asList((GrammarStep)file).iterator());
		// we would have to hope that the defn has come to an end
		if (!defnItr.isAtEnd())
			fail("defn was not at end");
	}

	private void recursivelyCompareItems(DefinitionIterator defn, Iterator<GrammarStep> trees) {
//		System.out.println("Compare " + defn + " to " + trees);
		while (trees.hasNext()) {
			GrammarStep step = trees.next();
			logger.info("Comparing tree " + step + " with " + defn.current());
			if (defn.canHandle(step)) {
//				System.out.println("handled " + step);
				if (step instanceof GrammarTree)
					recursivelyCompareItems(defn, ((GrammarTree) step).members());
			} else
				fail("cannot handle " + step);
		}
	}
}
