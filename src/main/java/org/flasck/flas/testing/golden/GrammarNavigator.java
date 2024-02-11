package org.flasck.flas.testing.golden;

import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.grammar.ActionDefinition;
import org.flasck.flas.grammar.Definition;
import org.flasck.flas.grammar.Grammar;
import org.flasck.flas.grammar.IndentDefinition;
import org.flasck.flas.grammar.ManyDefinition;
import org.flasck.flas.grammar.OptionalDefinition;
import org.flasck.flas.grammar.OrProduction;
import org.flasck.flas.grammar.Production;
import org.flasck.flas.grammar.RefDefinition;
import org.flasck.flas.grammar.SequenceDefinition;
import org.flasck.flas.grammar.TokenDefinition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class GrammarNavigator {
	public static class TaggedDefinition {
		private final String tag;
		private final Definition defn;
		private int offset;

		public TaggedDefinition(Production grammarRule) {
			this(grammarRule.name, maybeOr(grammarRule));
		}

		public TaggedDefinition(String tag, Definition defn) {
			this.tag = tag;
			this.defn = defn;
			if (defn instanceof SequenceDefinition)
				offset = 0;
			else
				offset = -1;
		}

		private static Definition maybeOr(Production prod) {
			if (prod instanceof OrProduction) {
				return new ChoiceDefinition((OrProduction)prod);
			} else {
				return prod.defn;
			}
		}
	}

	public static class Stash {
		private final int depth;

		public Stash(int depth) {
			this.depth = depth;
		}

	}

	public static final Logger logger = LoggerFactory.getLogger("GrammarChecker");
	private final Grammar grammar;
	private final List<TaggedDefinition> stack = new ArrayList<>();
	private final List<Stash> stashes = new ArrayList<>();
	private boolean haveAdvanced;
	enum FlushEnd {
		NOTHING,
		TOKEN,
		INDENT
	}

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

	public int depth() {
		return stack.size();
	}
	
	public void stashHere() {
//		System.out.println("stashHere");
		stashes.add(0, new Stash(stack.size()));
	}

	public void unstash() {
//		System.out.println("unstash");
		Stash curr = stashes.remove(0);
		boolean comingUp = false;
		while (stack.size() > curr.depth) {
			TaggedDefinition td = stack.get(0);
			if (td.defn instanceof SequenceDefinition)
				flushRule(comingUp);
			else if (td.defn instanceof RefDefinition ||
					td.defn instanceof ChoiceDefinition) {
				// one and done
			} else if (td.defn instanceof IndentDefinition ||
					td.defn instanceof ManyDefinition) {
				// safe to assume we have completed these
			} else
				throw new NotImplementedException("what do we do with " + td.defn.getClass());
			stack.remove(0);
			comingUp = true;
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
		this.haveAdvanced = false;
		while (true) {
			TaggedDefinition td = stack.get(0);
			logger.info("attempting to handle " + token + " with " + current());
			if (td.defn instanceof SequenceDefinition) {
				SequenceDefinition sd = (SequenceDefinition) td.defn;
				int init = td.offset;
				Definition nd = sd.nth(td.offset);
				if (nd instanceof ActionDefinition) {
					advanceToNext(null);
					this.haveAdvanced = false;
					continue;
				}
				if (nd instanceof TokenDefinition) {
					TokenDefinition tokd = (TokenDefinition)nd;
					if (tokd.isToken(grammar, token.type, token.text)) {
						advanceToNext(null);
						return true;
					}
				} else if (nd instanceof RefDefinition) {
					return moveToTag(token.type, token.text);
				} else if (nd instanceof OptionalDefinition) {
					boolean matched = moveToTag(token.type, token.text);
					if (matched) {
						return true;
					} else {
						// The nature of option is that failure IS an option ... just move on and try again
						advanceToNext(null);
						return handlesToken(token);
					}
				} else if (nd instanceof ManyDefinition) {
					boolean matched = moveToTag(token.type, token.text);
					if (matched) {
						advanceToNext(null);
						return true;
					} else {
						// Failure is acceptable for a Many
						// TODO: I think we need to consider the 1-or-more case and throw an error if only 0
						td.offset = init; // but move back here before advancing again
						advanceToNext(null);
						return handlesToken(token);
					}
				} else {
					// TODO: need to handle "CondDefinition" which is just a meta token
					// We should flag all of these with a marker interface MetaDefinition or something
					// and just skip over them in the matchLine code
					System.out.println("did not handle defn type " + nd.getClass());
				}
			} else if (td.defn instanceof TokenDefinition) {
				TokenDefinition tokd = (TokenDefinition)td.defn;
				if (tokd.isToken(grammar, null, token.text)) {
					advanceToNext(null);
					return true;
				}
			} else if (td.defn instanceof ManyDefinition) {
				boolean matched = moveToTag(token.type, token.text);
				if (matched) {
					advanceToNext(null);
					return true;
				} else {
					// Failure is acceptable for a Many
					// TODO: I think we need to consider the 1-or-more case and throw an error if only 0
					pop(null);
					return handlesToken(token);
				}
			} else if (td.defn instanceof ChoiceDefinition) {
				boolean matched = moveToTag(token.type, token.text);
				if (matched) {
					advanceToNext(null);
					return true;
				}
				System.out.println("Did not match any of the choices");
			} else {
				System.out.println("handlesToken does not handle " + td.defn.getClass());
			}
			return false;
		}
	}

	private boolean handlesTree(GrammarTree tree) {
		this.haveAdvanced = false;
		List<TaggedDefinition> prods = new ArrayList<>();
		boolean ret = moveToTag(tree.reducedToRule(), null);
		while (!ret) {
			prods.add(stack.remove(0));
			if (stack.isEmpty())
				break;
			TaggedDefinition td = stack.get(0);
			if (td.defn instanceof SequenceDefinition)
				td.offset++;
			ret = navigateTo(td, tree.reducedToRule(), null, prods, new TreeSet<>());
			if (ret)
				prods.clear();
		}
		stack.addAll(0, prods);
		return ret;
	}

	private boolean moveToTag(String rule, String toktext) {
		List<TaggedDefinition> prods = new ArrayList<>();
		Set<String> triedRules = new TreeSet<>();
		boolean ret = navigateTo(stack.get(0), rule, toktext, prods, triedRules);
		for (int i=0;i<prods.size();i++) {
			stack.add(0, prods.get(i));
		}
		return ret;
	}

	private boolean navigateTo(TaggedDefinition from, String rule, String toktext, List<TaggedDefinition> prods, Set<String> triedRules) {
		Definition d = from.defn;
//		System.out.println("navigate to " + d + " - " + from.offset);
		
		if (d instanceof TokenDefinition) {
			// Q1a: Are we there yet? (Token version)
			TokenDefinition tokd = (TokenDefinition) d;
			boolean ret = tokd.isToken(grammar, rule, toktext);
			if (ret)
				advanceToNext(prods);
			return ret;
		}

		if (d instanceof RefDefinition) {
			// Q1b: Are we there yet? (Ref version)
			RefDefinition rd = (RefDefinition)d;

			// don't go into an infinite loop
			if (triedRules.contains(rd.ruleName()))
				return false;
			triedRules.add(rd.ruleName());

			Production prod = rd.production(grammar); 
			if (prod.refersTo(rule)) {
				// still need to push the nested defn
				prods.add(new TaggedDefinition(prod));
				return true;
			}
			
			// Q2: are we nested deep within this production?
			if (navigateNext(new TaggedDefinition(prod), rule, toktext, prods, triedRules))
				return true;
		}
		
		// Q3: if we are in a many definition, can the inner one handle it?
		if (d instanceof ManyDefinition) {
			ManyDefinition m = (ManyDefinition) d;
			if (navigateNext(new TaggedDefinition("many", m.repeats()), rule, toktext, prods, triedRules))
				return true;
		}
		
		// Q4: if this is an optional definition, does the token appear at the front of the nested rule?
		// (But failure is an option)
		if (d instanceof OptionalDefinition) {
			OptionalDefinition od = (OptionalDefinition) d;
			if (navigateNext(new TaggedDefinition("option", od.childRule()), rule, toktext, prods, triedRules))
				return true;
		}
		
		// Q5: if we are (somewhere) in a sequence, does the current token/rule match?
		if (d instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) d;
			if (from.offset == 0 && sd.canReduceAs(rule)) {
				return true;
			}
			while (from.offset < sd.length()) {
				Definition nth = sd.nth(from.offset);
				logger.info("Looking at " + from.offset + ": " + nth);
				if (nth instanceof ActionDefinition) {
					logger.info("skipping action " + from.offset + ": " + nth.getClass());
					from.offset++;
					continue;
				}
				if (navigateNext(new TaggedDefinition("seq_" + from.offset, nth), rule, toktext, prods, triedRules))
					return true;
				if (nth instanceof ManyDefinition || nth instanceof OptionalDefinition) {
					logger.info("navigateTo fine with many/option at " + from.offset);
					from.offset++;
//					System.out.println("incremented from.offset to " + from.offset);
				} else
					return false;
			}
		}
		
		if (d instanceof IndentDefinition) {
			// Q6: an indent definition with an explicit reduction will match right here ...
			IndentDefinition id = (IndentDefinition) d;
			Definition nested = id.indented();

			if (id.canReduceAs(rule)) {
				// push the indent and the ref
				prods.add(new TaggedDefinition("indented", nested));
				RefDefinition rd = (RefDefinition)nested;
				Production prod = rd.production(grammar); 
				prods.add(new TaggedDefinition(prod));
				return true;
			}

			// Q7: if it's an indent definition that includes a RefDefinition, follow it down ...
			if (navigateNext(new TaggedDefinition("indented", nested), rule, toktext, prods, triedRules))
				return true;
		}
		
		// Q8: if it's a choice definition, we need to try all the cases in turn
		if (d instanceof ChoiceDefinition) {
			ChoiceDefinition cd = (ChoiceDefinition) d;
			for (int n=0;n<cd.quant();n++) {
				if (navigateNext(new TaggedDefinition("choice_" + n, cd.nth(n)), rule, toktext, prods, triedRules))
					return true;
			}
		}
		
		return false;
	}

	private void advanceToNext(List<TaggedDefinition> prods) {
		if (haveAdvanced)
			return;
		TaggedDefinition top;
		if (prods == null) {
			top = stack.get(0);
		} else {
			top = prods.get(prods.size()-1);
		}
		if (top.defn instanceof SequenceDefinition) {
			top.offset++;
//			try {
//				throw new Exception("advanced to next " + top.offset);
//			} catch (Exception ex) {
//				ex.printStackTrace(System.out);
//			}
			logger.info("advanced to next " + top.offset);
			if (top.offset < ((SequenceDefinition)top.defn).length()) {
				this.haveAdvanced = true;
				return;
			}
			// if we reach the end of the SD, then we need to pop it and try the thing above
//			advanceToNext(pop(prods));
		} else if (top.defn instanceof TokenDefinition || 
				top.defn instanceof RefDefinition ||
				top.defn instanceof ChoiceDefinition ||
				top.defn instanceof OptionalDefinition) {
			// We have matched the definition and that's all there is to see here,
			// so pop it off the stack and try the next level down
//			advanceToNext(pop(prods));
		} else if (top.defn instanceof ManyDefinition) {
			// this is a reasonable place to find the next token, so wait here ...
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

	private boolean navigateNext(TaggedDefinition td, String rule, String toktext, List<TaggedDefinition> prods, Set<String> triedRules) {
		prods.add(td);
		if (navigateTo(td, rule, toktext, prods, triedRules))
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
	
	public void moveToEndOfLine(int depth) {
		boolean comingUp = false;
		while (stack.size() > depth) {
			FlushEnd fr = flushRule(comingUp);
			if (fr == FlushEnd.INDENT)
				break;
			else if (fr == FlushEnd.TOKEN)
				fail("cannot move past a token");
			stack.remove(0);
			comingUp = true;
		}
		flushRule(comingUp);
	}


	public void skipActions() {
		TaggedDefinition td = stack.get(0);
		System.out.println("skip actions in " + td);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			while (sd.nth(td.offset) instanceof ActionDefinition) {
				System.out.println("SkipActions skipping " + td.offset);
				td.offset++;
			}
		} else
			throw new CantHappenException("must be a sequence definition");
	}


	public void safePop(String ruleName) {
		showStack();
		boolean comingUp = false;
		boolean readyToBreak = false;
		while (!stack.isEmpty()) {
			if (flushRule(comingUp) != FlushEnd.NOTHING)
				break;
			comingUp = true;
			TaggedDefinition td = stack.remove(0);
			System.out.println("safepop popped " + td.tag);
			if (td.tag.equals(ruleName))
				readyToBreak = true;
			if (readyToBreak) {
				td = stack.get(0);
				if (!td.tag.startsWith("seq_") && !td.tag.startsWith("choice_"))
					break;
			}
		}
	}

	private void showStack() {
		System.out.print("Stack:");
		for (TaggedDefinition td : stack) {
			System.out.print(" " + td.tag);
		}
		System.out.println();
	}

	// comingUp here says that we are coming up from a nested scope, so
	// we will have processed the first token we see in a sequence
	public FlushEnd flushRule(boolean comingUp) {
		TaggedDefinition td = stack.get(0);
//		System.out.println("move to end of rule " + td.tag);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;

			if (comingUp) {
				td.offset++; // we have processed the current item now, hence flushing ...
				logger.info("coming up increments td.offset to " + td.offset);
			}
			
			while (td.offset < sd.length()) {
				Definition curr = sd.nth(td.offset);
				if (curr instanceof ActionDefinition) {
					logger.info("flush rule skipping " + td.offset);
					td.offset++;
					continue;
				}
					
				if (curr instanceof IndentDefinition) {
//					System.out.println("Found indent definition at " + td.offset + " ... returning");
					return FlushEnd.INDENT;
				} else if (curr instanceof ManyDefinition || curr instanceof OptionalDefinition) {
					logger.info("skipping many or optional in flush rule fine with " + td.offset);
					td.offset++;
					continue;
				} else if (curr instanceof TokenDefinition) {
//					TokenDefinition tok = (TokenDefinition) curr;
//					System.out.println("missing token: " + tok);
					return FlushEnd.TOKEN;
				} else
					fail("what is " + curr + " " + curr.getClass() + "?");
			}
			// we have reached the end of the rule
			return FlushEnd.NOTHING;
		} else if (td.defn instanceof OptionalDefinition) {
			return FlushEnd.NOTHING;
		} else if (td.defn instanceof IndentDefinition) {
			// if I understand this correctly, we are not really processing a "rule" at this point, but sure, we're done ...
			return FlushEnd.NOTHING;
		} else if (td.defn instanceof ManyDefinition) {
			return FlushEnd.NOTHING;
		} else if (td.defn instanceof ChoiceDefinition) {
			return FlushEnd.NOTHING;
		} else if (td.defn instanceof RefDefinition) {
			return FlushEnd.NOTHING;
		} else if (td.defn instanceof TokenDefinition) {
			// will have been handled
			return FlushEnd.NOTHING;
		} else
			throw new NotImplementedException("td.defn is a " + td.defn.getClass());
	}

	public boolean hasIndents() {
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			if (td.offset >= sd.length())
				return false;
			Definition d = sd.nth(td.offset);
			return d instanceof IndentDefinition;
		}
		return false;
	}

	public boolean isSeq() {
		TaggedDefinition td = stack.get(0);
		return td.defn instanceof SequenceDefinition;
	}

	public boolean isMany() {
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof ManyDefinition)
			return true;
		else if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			if (sd.nth(td.offset) instanceof ManyDefinition)
				return true;
		}
		
		return false;
	}
	
	public void showIndentedOptionsIfApplicable(PrintWriter pw) {
		TaggedDefinition td = stack.get(0);
		if (td.defn instanceof SequenceDefinition) {
			SequenceDefinition sd = (SequenceDefinition) td.defn;
			if (sd.nth(td.offset) instanceof IndentDefinition) {
				IndentDefinition id = (IndentDefinition) sd.nth(td.offset);
				Definition d = id.indented();
				if (d instanceof RefDefinition) {
					RefDefinition rd = (RefDefinition) d;
					Production prod = rd.production(grammar);
					if (prod instanceof OrProduction) {
						OrProduction op = (OrProduction) prod;
						for (Definition i : op.allOptions()) {
							if (i instanceof RefDefinition) {
								RefDefinition show = (RefDefinition) i;
								pw.println(" | " + show.ruleName());
							} else if (i instanceof SequenceDefinition) {
								pw.print(" |");
								SequenceDefinition show = (SequenceDefinition) i;
								for (int q=0;q<show.length();q++) {
									Definition showd = show.nth(q);
									if (showd instanceof ActionDefinition)
										continue;
									pw.print(" ");
									if (showd instanceof RefDefinition)
										pw.print(((RefDefinition) showd).ruleName());
									else if (showd instanceof TokenDefinition)
										pw.print(((TokenDefinition) showd).token());
									else
										showd.showGrammarFor(pw);
								}
								pw.println();
							} else {
								pw.print(" ||| ");
								i.showGrammarFor(pw);
								pw.println();
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public String toString() {
		return "stack-" + stack.size();
	}
}
