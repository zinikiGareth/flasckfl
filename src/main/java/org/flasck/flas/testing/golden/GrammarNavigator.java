package org.flasck.flas.testing.golden;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.IndentDefinition;
import org.flasck.flas.grammar.Lexer;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.OptionalDefinition;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.SequenceDefinition;
import org.flasck.flas.grammar.TokenDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.zinutils.exceptions.NotImplementedException;

public class GrammarNavigator {
	public static class TaggedDefinition {
		private final String tag;
		private final Definition defn;
		private int offset;

		public TaggedDefinition(Production grammarRule) {
			this(grammarRule.name, grammarRule.defn);
		}

		public TaggedDefinition(String tag, Definition defn) {
			this.tag = tag;
			this.defn = defn;
			if (defn instanceof SequenceDefinition)
				offset = 0;
			else
				offset = -1;
		}
	}

	public static class Stash {
		private final int depth;

		public Stash(int depth) {
			this.depth = depth;
		}

	}

	private final Grammar grammar;
	private final List<TaggedDefinition> stack = new ArrayList<>();
	private final List<Stash> stashes = new ArrayList<>();

	public GrammarNavigator(Grammar grammar, String ruleName, Definition grammarRule) {
		this.grammar = grammar;
		push(ruleName, grammarRule);
	}

	private void push(String ruleName, Definition grammarRule) {
		stack.add(0, new TaggedDefinition(ruleName, grammarRule));
	}

	public boolean isAtEnd() {
		// I'm not really even sure this is necessary as errors will have occurred
		// trying to unstash ...
		return stack.size() == 1;
	}

	public void stashHere() {
		System.out.println("stashHere");
		stashes.add(0, new Stash(stack.size()));
	}

	public void unstash() {
		System.out.println("unstash");
		Stash curr = stashes.remove(0);
		while (stack.size() > curr.depth) {
			TaggedDefinition td = stack.get(0);
			if (td.defn instanceof SequenceDefinition)
				moveToEndOfRule();
			else if (td.defn instanceof RefDefinition) {
				// one and done
			} else if (td.defn instanceof IndentDefinition) { // also ManyDefinition
				// safe to assume we have completed these
			} else
				throw new NotImplementedException("what do we do with " + td.defn.getClass());
			stack.remove(0);
		}
	}

	public boolean canHandle(GrammarStep step) {
		if (step instanceof GrammarToken) {
			return handlesToken((GrammarToken) step);
		} else if (step instanceof GrammarTree)
			return handlesTree((GrammarTree)step);
		else
			throw new NotImplementedException(step.getClass().getName());
	}

	
	private boolean handlesToken(GrammarToken token) {
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			Definition nd = sd.nth(td.offset);
			if (nd instanceof TokenDefinition) {
				TokenDefinition tokd = (TokenDefinition)nd;
				Lexer lexer = tokd.lexer(grammar);
				String patt = lexer.pattern;
				if (token.text.matches(patt)) {
					advanceToNext(null);
					return true;
				}
			} else if (nd instanceof RefDefinition) {
				return moveToTag(token.type);
			} else if (nd instanceof OptionalDefinition) {
				boolean matched = moveToTag(token.type);
				if (matched) {
					return true;
				} else {
					// The nature of option is that failure IS an option ... just move on and try again
					advanceToNext(null);
					return handlesToken(token);
				}
			} else {
				System.out.println("did not handle defn type " + nd.getClass());
			}
		} else {
			System.out.println("definition was not SD but " + td.defn.getClass());
		}
		return false;
	}

	private boolean handlesTree(GrammarTree tree) {
		return moveToTag(tree.reducedToRule());
	}

	private boolean moveToTag(String rule) {
		List<TaggedDefinition> prods = new ArrayList<>();
		boolean ret = navigateTo(stack.get(0), rule, prods);
		
		for (int i=0;i<prods.size();i++) {
			stack.add(0, prods.get(i));
		}
		return ret;
	}

	private boolean navigateTo(TaggedDefinition from, String rule, List<TaggedDefinition> prods) {
		Definition d = from.defn;
		
		if (d instanceof TokenDefinition) {
			// Q1a: Are we there yet? (Token version)
			TokenDefinition tokd = (TokenDefinition) d;
			boolean ret = tokd.isToken(rule);
			if (ret)
				advanceToNext(prods);
			return ret;
		}
		if (d instanceof RefDefinition) {
			// Q1b: Are we there yet? (Ref version)
			RefDefinition rd = (RefDefinition)d;
			Production defn = rd.production(grammar); 
			if (defn.refersTo(rule)) {
				// still need to push the nested defn
				prods.add(new TaggedDefinition(defn));
				return true;
			}
			
			// Q2: are we nested deep within this production?
			if (navigateNext(new TaggedDefinition(defn), rule, prods))
				return true;
		}
		
		// Q3: if we are in a many definition, can the inner one handle it?
		if (d instanceof ManyDefinition) {
			ManyDefinition m = (ManyDefinition) d;
			if (navigateNext(new TaggedDefinition("many", m.repeats()), rule, prods))
				return true;
		}
		
		// Q4: if we are (somewhere) in a sequence, does the current token/rule match?
		if (d instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) d;
			if (from.offset == 0 && sd.canReduceAs(rule)) {
				return true;
			}
			while (from.offset < sd.length()) {
				Definition nth = sd.nth(from.offset);
				if (navigateNext(new TaggedDefinition("" + from.offset, nth), rule, prods))
					return true;
				if (nth instanceof ManyDefinition || nth instanceof OptionalDefinition)
					from.offset++;
				else
					return false;
			}
		}
		
		// Q5: if it's an indent definition, then I think that includes a RefDefinition, so follow it down ...
		if (d instanceof IndentDefinition) {
			IndentDefinition id = (IndentDefinition) d;
			Definition nested = id.indented();
			if (navigateNext(new TaggedDefinition("indented", nested), rule, prods))
				return true;
		}
		
		return false;
	}

	private void advanceToNext(List<TaggedDefinition> prods) {
		TaggedDefinition top;
		if (prods == null) {
			top = stack.get(0);
		} else {
			top = prods.get(prods.size()-1);
		}
		if (top.defn instanceof SequenceDefinition) {
			top.offset++;
			if (top.offset < ((SequenceDefinition)top.defn).length())
				return;
			// if we reach the end of the SD, then we need to pop it and try the thing above
			advanceToNext(pop(prods));
		} else if (top.defn instanceof TokenDefinition || top.defn instanceof RefDefinition) {
			// We have matched the definition and that's all there is to see here,
			// so pop it off the stack and try the next level down
			advanceToNext(pop(prods));
		} else {
			System.out.println("need to handle advance for " + top.defn.getClass());
		}
	}

	private List<TaggedDefinition> pop(List<TaggedDefinition> prods) {
		if (prods == null) {
			stack.remove(0);
			return null;
		} else {
			prods.remove(prods.size()-1);
			if (prods.isEmpty())
				return null;
			else
				return prods;
		}
	}

	private boolean navigateNext(TaggedDefinition td, String rule, List<TaggedDefinition> prods) {
		prods.add(td);
		if (navigateTo(td, rule, prods))
			return true;
		prods.remove(prods.size()-1);
		return false;
	}

	public String current() {
		if (stack.isEmpty())
			return "***END***";
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition)td.defn;
			return td.tag + ":" + td.offset + " " + (td.offset < sd.length() ? sd.nth(td.offset) : "OOB");
		} else
			return td.tag;
	}

	public boolean requiresNoTokens() {
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof ManyDefinition) {
			ManyDefinition md = (ManyDefinition) td.defn;
			if (md.repeats() instanceof RefDefinition)
				return true;
		}
		return false;
	}
	
	public void moveToEndOfRule() {
		TaggedDefinition td = stack.get(0);
		System.out.println("move to end of rule " + td);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			while (td.offset < sd.length()) {
				Definition curr = sd.nth(td.offset);
				if (curr instanceof IndentDefinition)
					return;
				else if (curr instanceof ManyDefinition || curr instanceof OptionalDefinition) {
					td.offset++;
					continue;
				} else
					fail("what is " + curr.getClass() + "?");
			}
			// we have reached the end of the rule
			
		} else
			throw new NotImplementedException("td.defn is a " + td.defn.getClass());
	}

	public boolean hasIndents() {
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			Definition d = sd.nth(td.offset);
			return d instanceof IndentDefinition;
		}
		return false;
	}

	public boolean isMany() {
		TaggedDefinition td = stack.get(0);
		return td.defn instanceof ManyDefinition;
	}
	
	@Override
	public String toString() {
		return "stack-" + stack.size();
	}
}
