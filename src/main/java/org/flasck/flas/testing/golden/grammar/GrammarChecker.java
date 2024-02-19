package org.flasck.flas.testing.golden.grammar;

import static org.junit.Assert.assertNotNull;
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
import org.flasck.flas.grammar.GrammarSupport;
import org.flasck.flas.testing.golden.FileReconstructor;
import org.flasck.flas.testing.golden.ParsedTokens;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.flasck.flas.testing.golden.ParsedTokens.ReductionRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;
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
	private final GrammarChooser grammar;

	public GrammarChecker(File parseTokens, File reconstruct) {
		this.parseTokens = parseTokens;
		this.reconstruct = reconstruct;
		this.grammar = new GrammarChooser(GrammarSupport.loadGrammar());
	}

	public Map<String, GrammarTree> checkParseTokenLogic(boolean expectErrors){
		if (parseTokens == null || reconstruct == null)
			return null;
		Map<String, GrammarTree> ret = new TreeMap<>(new MyPreferredTestSorting());
		for (File f : FileUtils.findFilesMatching(parseTokens, "*")) {
			ParsedTokens toks = ParsedTokens.read(f);
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
//					System.out.println("pushing " + nt);
					srstack.add(0, nt);
				}
			} else {
				// It's a reduction
				ReductionRule rr = (ReductionRule) s;
//				System.out.println("considering rule " + rr + " with " + srstack);
				GrammarTree tree = new GrammarTree(rr);
				List<GrammarStep> shifted = new ArrayList<>();
				while (!srstack.isEmpty()) {
					GrammarStep si = srstack.get(0);
					if (rr.includes(si.location())) {
//						System.out.println("reducing " + si + " with " + rr);
						tree.push(si);
						srstack.remove(0);
					} else if (si.location().compareTo(rr.location()) > 0) {
//						System.out.println("shifting " + si + " to get to earlier tokens");
						shifted.add(si);
						srstack.remove(0);
					} else {
//						System.out.println("not reducing " + si + " with " + rr);
						break;
					}
				}
				srstack.add(0, tree);
//				if (!shifted.isEmpty())
//					System.out.println("shifted = " + shifted);
				srstack.addAll(0, shifted);
			}
		}
		while (!srstack.isEmpty())
			ret.add(0, (GrammarTree) srstack.remove(0));
		return new GrammarTree(topRule, ret);
	}

	public void dumpTree(File file, GrammarTree top) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		top.dump(pw, "", false);
		pw.close();
	}

	public void checkGrammar(Map<String, GrammarTree> fileOrchards) {
		for (Entry<String, GrammarTree> e : fileOrchards.entrySet()) {
//			String name = e.getKey();
//			String ext = FileUtils.extension(name);
//			String topRule = getTopRule(ext);
			checkProductionsAgainstGrammar(e.getValue());
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

	private void checkProductionsAgainstGrammar(GrammarTree file) {
//		Production grammarRule = grammar.findRule("file");
//		if (grammarRule == null)
//			throw new CantHappenException("there is no rule in the grammar for the file production");
//		if (!(grammarRule instanceof OrProduction))
//			throw new CantHappenException("file rule is not an OrProduction");
//		OrProduction options = (OrProduction) grammarRule;
//		
		GrammarNavigator gn = grammar.newNavigator();
//		gn.push(options);
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
		traverseTree(tree, gn);
	}

	// We are going to use "real" recursion to traverse the tree, and keep the GrammarNavigator in step by telling it when (and how) we are recursing
	private void traverseTree(GrammarTree tree, GrammarNavigator gn) {
		System.out.println("traverseTree: " + tree.reducedToRule() + " " + tree.hasMembers() + " " + tree.hasIndents() + " " + gn);
		String rule = tree.reducedToRule();
		TrackProduction prod = gn.findChooseableRule(rule);
		if (prod == null)
			throw new CantHappenException("there is no chooseable rule to match " + rule + " in " + gn);
		gn.push(prod);

		System.out.println("using rule " + prod);
		boolean scopeOnly = false;
		if (tree.isSingleton() && !prod.isSeqReducer(rule)) {
			traverseTree((GrammarTree) tree.members().next(), gn);
		} else if (tree.isTerminal()) {
			if (!matchTerminal(prod, tree.terminal()))
				matchLineSegment(rule, tree, gn);
		} else if (tree.hasMembers()) {
			matchLineSegment(rule, tree, gn);
		} else
			scopeOnly = true;
		
		if (tree.hasIndents()) {
			if (!scopeOnly) {
				if (!(prod instanceof SeqProduction)) {
					throw new CantHappenException("tree has indents but rule is not a SeqProduction: " + prod + " " + prod.getClass());
				}
				SeqProduction sp = (SeqProduction) prod;
				TrackProduction indent = sp.indented();
				assertNotNull("have a tree indent in " + rule + " but not in rule: " + gn, indent);
				gn.push(indent);
				System.out.println("matching indents " + indent);
			}
			Iterator<GrammarTree> it = tree.indents();
			while (it.hasNext()) {
				traverseTree(it.next(), gn);
			}
			if (!scopeOnly) {
				System.out.println("done with indents");
				gn.pop();
			}
		}
		gn.pop();
	}

	private void matchLineSegment(String rule, GrammarTree tree, GrammarNavigator gn) {
		System.out.println("match line segment: " + rule + " " + tree + " " + gn);
		Iterator<GrammarStep> mit = tree.members();
		SeqReduction sr = gn.sequence(rule);
		Iterator<SeqElement> sit = sr.iterator();
		GrammarStep mi = null;
		SeqElement si = null;;
		while ((mi != null || mit.hasNext()) && (si != null || sit.hasNext())) {
			if (mi == null)
				mi = mit.next();
			if (si == null)
				si = sit.next();
			System.out.println("Comparing " + mi + " " + mi.getClass() + " :: " + si + " " + si.getClass());
			MatchResult mr = si.matchAgainst(mi);
			System.out.println("mr = " + mr);
			switch (mr) {
			case SINGLE_MATCH_ADVANCE: {
				// they matched and there was no repetition or anything; advance both
				si = null;
				mi = null;
				break;
			}
			case MANY_MATCH_MAYBE_MORE: {
				// the token matched the rule, so move on to the next.  Try the same grammar step again as it can handle many
				mi = null;
				// si remains in force
				break;
			}
			case MANY_NO_MATCH_TRY_NEXT: {
				// the token did not match the rule, but that's OK. Try the next grammar step with the same token
				si = null;
				// mi remains unchanged
				break;
			}
			case MATCH_NESTED: {
				matchNested(gn, mi, si);
				// if things didn't match, we wouldn't have reached here
				// so they matched exactly
				mi = null;
				si = null;
				break;
			}
			case MATCH_NESTED_MAYBE_MORE: {
				matchNested(gn, mi, si);
				// if things didn't match, we wouldn't have reached here
				// so they matched exactly
				mi = null;
				// because there may be more, si remains unchanged
				break;
			}
			case SINGLE_MATCH_FAILED: {
				throw new CantHappenException("the grammar did not match at " + mi + " == " + si);
			}
			default:
				throw new NotImplementedException("match result " + mr);
			}
		}
		if (mi != null || mit.hasNext()) {
			if (mi == null)
				mi = mit.next();
			throw new CantHappenException("the tree has remaining members which were not consumed by the grammar: " + mi);
		}
		while (sit.hasNext()) {
			si = sit.next();
			if (!si.canBeSkipped())
				throw new CantHappenException("the grammar expects the tree to have more members which were not there: " + si);
		}
		System.out.println("matched line segment");
	}

	private void matchNested(GrammarNavigator gn, GrammarStep mi, SeqElement si) {
		// a grammar tree matched a REF element, but we need to (recursively) check all the elements of both
		if (mi instanceof GrammarTree) {
			GrammarTree tree = (GrammarTree)mi;
			String inRule = tree.reducedToRule();
			if (tree.isSingleton()) {
				TrackProduction prod = grammar.rule(inRule);
				GrammarTree inner = (GrammarTree) tree.members().next();
				String reducedAs = inner.reducedToRule();
				System.out.println("XX: " + reducedAs);
				TrackProduction tp = prod.choose(reducedAs);
				if (tp == null)
					throw new CantHappenException("there is no reduction for " + reducedAs + " in " + inRule);
				if (tp instanceof SeqProduction) {
					gn.push(tp);
					boolean matched = false;
					if (tree.isTerminal())
						matched = matchTerminal(tp, tree.terminal());
					if (!matched)
						matchLineSegment(reducedAs, inner, gn);
					gn.pop();
				} else if (tp instanceof OrChoice) {
					if (inner.isTerminal())
						matchTerminal(tp, inner.terminal());
					else {
						matchNested(gn, inner, si);
					}
				}
			} else if (si instanceof ManyElement) {
				ManyElement me = (ManyElement) si;
				if (me.matchesRef()) {
					gn.push(me.matchRef());
					matchLineSegment(inRule, tree, gn);
					gn.pop();
				}
			} else if (si instanceof RefElement) {
				// we should have a sequence in both the members and in the rule
				RefElement re = (RefElement) si;
				TrackProduction tp = grammar.rule(re.refersTo());
				gn.push(tp);
				boolean matched = false;
				if (tree.isTerminal())
					matched = matchTerminal(tp, tree.terminal());
				if (!matched)
					matchLineSegment(inRule, tree, gn);
				gn.pop();
			} else if (tree.isTerminal()) {
				throw new NotImplementedException("handle terminal case");
			} else
				throw new CantHappenException("what is happening in this case?");
		} else {
			throw new CantHappenException("MATCH_NESTED is only for trees, I think");
		}
	}

	private boolean matchTerminal(TrackProduction tp, GrammarToken tok) {
		System.out.println("match terminal: " + tok + " == " + tp);
		TokenProduction rule = (TokenProduction) tp.choose(tok.type);
		if (rule != null) {
			// check it matches
			if (rule.matches(tok.text))
				return true;
			else
				return false;
//				throw new CantHappenException("the token did not match the pattern " + tok.text);
		} else {
			// try seeing if it is a keyword here
			if (tp.canBeKeyword(tok.text))
				return true;
			else
				return false;
//				throw new CantHappenException("no rule could be found for " + tok.type);
		}
	}
	
	/*
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
				matchLine(reducedAs.reducedToRule(), reducedAs.members(), gn);
				gn.moveToEndOfLine(depth);
				if (gn.hasIndents()) {
					matchIndents(tree.indents(), gn);
				} else {
					assertFalse(tree.hasIndents());
				}
			} else {
				// case B: it's just a simple rule with no indents at all
				int depth = gn.depth();
				matchLine(null, tree.members(), gn);
				gn.moveToEndOfLine(depth);
				assertFalse("rule "+ gn.current() + " does not have indents, but tree does", tree.hasIndents());
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

	private void matchLine(String ruleName, Iterator<GrammarStep> members, GrammarNavigator gn) {
		while (members.hasNext()) {
			GrammarStep s = members.next();
			logger.info("matching line token " + s + " with " + gn.current());
			if (s instanceof GrammarTree) {
				GrammarTree gt = (GrammarTree) s;
				System.out.println(gt);
			}
			if (gn.canHandle(s)) {
//				System.out.println("handled " + s);
				if (s instanceof GrammarTree) {
					GrammarTree gt = (GrammarTree) s;
					matchLine(gt.reducedToRule(), gt.members(), gn);
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
		System.out.println("have no more members");
		gn.safePop(ruleName);
	}

	// I'm not sure if this is exactly the same as handleScope or if there are
	// two (or more) possibilities depending on whether it's just a nested scope or
	// specific nested definitions (such as struct members or guarded equations)
	private void matchIndents(Iterator<GrammarTree> indents, GrammarNavigator gn) {
		handleScopeWithScopeRule(indents, gn);
	}
	*/
}
