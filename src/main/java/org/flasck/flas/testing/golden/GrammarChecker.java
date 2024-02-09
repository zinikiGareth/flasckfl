package org.flasck.flas.testing.golden;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.utils.FileUtils;

import doc.grammar.GenerateGrammarDoc;

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
		GenerateGrammarDoc.checkTokens(grammar);
		GenerateGrammarDoc.checkProductions(grammar);
	}

	public Map<String, GrammarTree> checkParseTokenLogic(boolean expectErrors){
		if (parseTokens == null || reconstruct == null)
			return null;
		Map<String, GrammarTree> ret = new TreeMap<>(new MyPreferredTestSorting());
		for (File f : FileUtils.findFilesMatching(parseTokens, "*")) {
			ParsedTokens toks = ParsedTokens.read(f);
			if (!expectErrors)
				calculateMostReduced(toks);
			try {
				toks.write(new File(f.getParentFile(), f.getName()+"-sorted"));
			} catch (FileNotFoundException ex) {
				fail("could not write sorted tokens to " + ex.getMessage());
			}
			reconstructFile(toks, new File(reconstruct, f.getName()));
			if (!expectErrors) {
				String ext = FileUtils.extension(f.getName());
				GrammarTree reduced = computeReductions(getTopRule(ext), toks);
				try {
				dumpTree(new File(f.getParentFile(), f.getName()+"-tree"), reduced);
				} catch (FileNotFoundException ex) {
					fail("could not write parse tree to " + ex.getMessage());
				}
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
		assertNoOverlappingRules(toks);
		assertTLFs(toks);
		assertAllTokensReduced(toks);
		return reduceToSingleTree(topRule, toks);
	}

	private void calculateMostReduced(ParsedTokens toks) {
		Map<InputPosition, ReductionRule> mostReduced = new TreeMap<>();
		for (ReductionRule rr : toks.reductionsInFileOrder()) {
			ReductionRule mr = null;
			for (GrammarToken t : toks.tokens()) {
				if (rr.includes(t.pos)) {
					if (mr != null && mr.includes(t.pos))
						continue;
					mr = null;
					if (mostReduced.containsKey(t.pos)) {
						mr = mostReduced.get(t.pos);
						continue;
					}
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
		for (ReductionRule mr : mostReduced.values())
			mr.makeMostReduced();
	}

	private void assertNoOverlappingRules(ParsedTokens toks) {
		InputPosition lastEndedAt = null;
		for (ReductionRule rr : toks.mostReduced()) {
			if (lastEndedAt != null && lastEndedAt.compareTo(rr.start()) >= 0)
				fail("overlapping reductions: " + lastEndedAt + " X " + rr);
			lastEndedAt = rr.last();
		}
	}

	private void assertTLFs(ParsedTokens toks) {
		for (ReductionRule rr : toks.mostReduced()) {
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
			for (ReductionRule rr : toks.reductionsInFileOrder()) {
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

	private GrammarTree reduceToSingleTree(String topRule, ParsedTokens toks) {
		List<GrammarTree> ret = new ArrayList<>();
		
		// the reduction rules are in order, and the tokens too ...
		List<GrammarStep> srstack = new ArrayList<>();
		Iterator<GrammarStep> sit = toks.iterator();
		while (sit.hasNext()) {
			GrammarStep s = sit.next();
//			System.out.println("have " + s);

			if (s instanceof GrammarToken) {
				GrammarToken nt = (GrammarToken) s;
				if (!nt.isComment()) {
					srstack.add(0, nt);
				}
			} else {
				// It's a reduction
//				System.out.println("considering rule " + rr + " with " + srstack);
				ReductionRule rr = (ReductionRule) s;
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

				if (rr.isMostReduced()) {
					ret.add(tree);
				}
			}
		}
		return new GrammarTree(topRule, ret);
	}

	public void dumpTree(File file, GrammarTree top) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		top.dump(pw, "", false);
		pw.close();
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
			return "assembly-file";
		case ".ut":
			return "unit-test-file";
		case ".st":
			return "system-test-file";
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
		
		GrammarNavigator gn = new GrammarNavigator(grammar, currRule, defn);
		logger.info("have defn " + gn.current());
		handleFileOfType(file, gn);
		// we would have to hope that the defn has come to an end
		if (!gn.isAtEnd())
			fail("defn was not at end");
	}
	
	/* This is all very complicated, but I think it all follows a pattern.
	 * 
	 * The parsed form is (I think) always of the form
	 *   GrammarTree [GrammarTree [one-line-of-members] []] [GrammarTree indents]
	 *   
	 * Meanwhile, the grammar file has rules all over the place, but they are often of the form
	 *   Rule ::= [list-of-tokens] [rule-that-is-often-an-or-of-indented rules]
	 *   
	 * And then it gets more complicated when these indented things aren't scopes.
	 * But let's try and match the same thing.
	 * 
	 */

	// handle an entire file of definitions, matching either against a ManyDefinition of a scope
	// or against a multi-step process (true of system tests, for example)
	private void handleFileOfType(GrammarTree tree, GrammarNavigator gn) {
		gn.stashHere();
		if (gn.canHandle(tree)) {
			if (gn.isMany()) {
				assertTrue(!tree.hasMembers());
				handleScopeWithScopeRule(tree.indents(), gn);
			} else if (gn.isSeq()) {
				gn.skipActions();
				if (gn.isMany()) {
					assertTrue(!tree.hasMembers());
					handleScopeWithScopeRule(tree.indents(), gn);
				}
			} else
				throw new NotImplementedException("can only handle Many definitions at the moment");
		}
		gn.unstash();
	}
	
	// handle a scope where the "root" grammar definition is a many definition of
	// a ref definition, which probably points to an OrDefinition of other cases
	private void handleScopeWithScopeRule(Iterator<GrammarTree> trees, GrammarNavigator gn) {
		while (trees.hasNext()) {
			gn.stashHere();
			GrammarTree tree = trees.next();
			logger.info("Comparing tree " + tree + " with " + gn.current());
			matchCompoundRule(tree, gn);
			gn.unstash();
		}
	}

	// By a "compound rule" what I mean is the common pattern in the parsed form,
	// where there is one entry in the member which is a tree which is the actual line
	// and then there may be indents (but only if supported by the grammar)
	private void matchCompoundRule(GrammarTree tree, GrammarNavigator gn) {
		logger.info("Matching compound rule " + tree + " with " + gn.current());
		if (gn.canHandle(tree)) {
			// CASE A: it's a compound rule
			if (tree.isSingleton()) {
				GrammarTree reducedAs = tree.singleton();
//				System.out.println("reduced as " + reducedAs.reducedToRule());
				if (!gn.canHandle(reducedAs))
					fail("cannot handle " + reducedAs.reducedToRule() + " in " + gn.current());
				int depth = gn.depth();
				matchLine(reducedAs.members(), gn);
				gn.moveToEndOfLine(depth);
				if (gn.hasIndents()) {
					matchIndents(tree.indents(), gn);
				} else {
					assertFalse(tree.hasIndents());
				}
			} else {
				// case B: it's just a simple rule with no indents at all
				int depth = gn.depth();
				matchLine(tree.members(), gn);
				gn.moveToEndOfLine(depth);
				assertFalse(tree.hasIndents());
			}
		} else {
			System.out.println("At " + tree.location());
			System.out.println("Grammar is at: " + gn.current());
			PrintWriter pw = new PrintWriter(System.out);
			gn.showIndentedOptionsIfApplicable(pw);
			pw.flush();
			System.out.println("Tree has rule: " + tree.reducedToRule());
			if (tree.isSingleton()) {
				System.out.println("   >> Singleton tree: " + tree.singleton().reducedToRule());
			} else {
				System.out.print("   >> Simple rule tree:");
				Iterator<GrammarStep> it = tree.members();
				while (it.hasNext()) {
					GrammarStep s = it.next();
					if (s instanceof GrammarToken)
						System.out.print(" " + ((GrammarToken) s).text);
					else if (s instanceof GrammarTree)
						System.out.print(" " + ((GrammarTree) s).reducedToRule());
					else
						System.out.print(" " + s);
				}
				System.out.println();
			}
			fail("cannot handle " + tree + " with defn " + gn.current());
		}
	}

	private void matchLine(Iterator<GrammarStep> members, GrammarNavigator gn) {
		while (members.hasNext()) {
			GrammarStep s = members.next();
			logger.info("matching line token " + s + " with " + gn.current());
			if (gn.canHandle(s)) {
//				System.out.println("handled " + s);
				if (s instanceof GrammarTree) {
					GrammarTree gt = (GrammarTree) s;
					matchLine(gt.members(), gn);
				}
				// need to handle nesting and things ...
				// can we call matchLine recursively or do we need to have something inside this?
			} else {
				System.out.println("At " + s.location());
				System.out.println("Grammar is at: " + gn.current());
				if (s instanceof GrammarTree)
					System.out.println("Tree has: " + ((GrammarTree) s).reducedToRule());
				else if (s instanceof GrammarToken)
					System.out.println("Token is: " + ((GrammarToken) s).type + " " + ((GrammarToken) s).text);
				else
					System.out.println("File is at " + s);
				fail("cannot match token in line " + s + " with " + gn.current());
			}
		}
	}

	// I'm not sure if this is exactly the same as handleScope or if there are
	// two (or more) possibilities depending on whether it's just a nested scope or
	// specific nested definitions (such as struct members or guarded equations)
	private void matchIndents(Iterator<GrammarTree> indents, GrammarNavigator gn) {
		handleScopeWithScopeRule(indents, gn);
	}
}
