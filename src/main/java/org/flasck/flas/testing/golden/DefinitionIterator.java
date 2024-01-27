package org.flasck.flas.testing.golden;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
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

public class DefinitionIterator {
	public class TaggedDefinition {
		private final String tag;
		private final Definition defn;
		private int offset = 0;

		public TaggedDefinition(Production grammarRule) {
			this.tag = grammarRule.name;
			this.defn = grammarRule.defn;
		}

		public TaggedDefinition(String tag, Definition defn) {
			this.tag = tag;
			this.defn = defn;
		}
	}

	private final Grammar grammar;
	private final List<TaggedDefinition> stack = new ArrayList<>();

	public DefinitionIterator(Grammar grammar, Production grammarRule) {
		this.grammar = grammar;
		push(grammarRule);
	}

	private void push(Production grammarRule) {
		stack.add(0, new TaggedDefinition(grammarRule));
	}

	public boolean isAtEnd() {
		slideForward();
		return stack.isEmpty();
	}

	// Just keep consuming things until there is nothing left
	// But obviously you cannot skip over tokens or anything else irreducible
	private void slideForward() {
		if (!stack.isEmpty())
			stack.remove(0);
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
					td.offset++;
					return true;
				}
			}
		}
		return false;
	}

	private boolean handlesTree(GrammarTree tree) {
		String rule = tree.reducedToRule();
		List<TaggedDefinition> prods = new ArrayList<>();
		boolean ret = navigateTo(stack.get(0), rule, prods);
		
		for (int i=0;i<prods.size();i++) {
			stack.add(0, prods.get(i));
		}
		return ret;
	}

	private boolean navigateTo(TaggedDefinition from, String rule, List<TaggedDefinition> prods) {
		Definition d = from.defn;
		
		if (d instanceof RefDefinition) {
			// Q1: Are we there yet?
			RefDefinition rd = (RefDefinition)d;
			Production defn = rd.production(grammar); 
			if (rd.refersTo(rule)) {
				// still need to push it
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
				// TODO: navigateTo should handle IndentDefinition & then we'd be fine ...
				if (navigateNext(new TaggedDefinition("" + from.offset, nth), rule, prods))
					return true;
				if (nth instanceof ManyDefinition || nth instanceof OptionalDefinition)
					from.offset++;
				else
					return false;
			}
		}
		return false;
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
			return td.tag + ":" + td.offset;
		} else
			return td.tag;
	}
	
	@Override
	public String toString() {
		return "stack-" + stack.size();
	}
}
