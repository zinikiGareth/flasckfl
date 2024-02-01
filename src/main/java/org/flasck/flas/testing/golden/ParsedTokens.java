package org.flasck.flas.testing.golden;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarStep;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ParsedTokens implements Iterable<GrammarStep> {

	public interface GrammarStep {
		InputPosition location();
	}

	public static class GrammarToken implements GrammarStep, Comparable<GrammarToken> {
		public final InputPosition pos;
		public final String type;
		public final String text;

		public GrammarToken(InputPosition pos, String type, String text) {
			this.pos = pos;
			this.type = type;
			this.text = text;
		}

		@Override
		public int compareTo(GrammarToken o) {
			return pos.compareTo(o.pos);
		}

		@Override
		public InputPosition location() {
			return pos;
		}

		public int lineNo() {
			return pos.lineNo;
		}

		public int tabs() {
			return pos.indent.tabs;
		}

		public int spaces() {
			return pos.indent.spaces;
		}

		public int offset() {
			return pos.off;
		}
		
		public boolean isComment() {
			return "comment".equals(type);
		}
		
		@Override
		public String toString() {
			return pos.toString() + " " + type + ":***" + text + "***";
		}
	}

	public static class ReductionRule implements GrammarStep {
		private final String rule;
		private InputPosition first;
		private InputPosition last;
		private int lineNumber;
		private boolean mostReduced;

		public ReductionRule(String rule) {
			this.rule = rule;
		}

		public void makeMostReduced() {
			if (first.indent == null)
				throw new CantHappenException("Can't make rule " + rule + " with null indent most reduced");
			if (first.indent.tabs != 1 || first.indent.spaces != 0)
				throw new CantHappenException("Can't make rule " + rule + " most reduced with indent " + first.indent + " at line " + first.lineNo);
			if (first.off != 0)
				throw new CantHappenException("Can't make rule " + rule + " most reduced with offset " + first.off + " at line " + first.lineNo);
			this.mostReduced = true;
		}
		
		public boolean isMostReduced() {
			return this.mostReduced;
		}
		
		public void range(InputPosition first, InputPosition last) {
			this.first = first;
			this.last = last;
		}
		
		public void lineNo(int lineNumber) {
			this.lineNumber = lineNumber;
		}

		@Override
		public InputPosition location() {
			return first;
		}
		
		public InputPosition start() {
			return first;
		}
		
		public InputPosition last() {
			return last;
		}

		public boolean includes(InputPosition pos) {
			return pos.compareTo(first) >= 0 && pos.compareTo(last) <= 0;
		}
		
		public String ruleName() {
			return rule;
		}

		@Override
		public String toString() {
			return rule + ": " + first + " -- " + last;
		}
	}

	private Set<GrammarToken> tokens = new TreeSet<>();
	private List<ReductionRule> reductionsInFileOrder = new ArrayList<>();

	private ParsedTokens() {
	}

	public static ParsedTokens read(File tokens) {
		Set<ReductionRule> starting = new TreeSet<ReductionRule>(new Comparator<ReductionRule>() {
			public int compare(ReductionRule o1, ReductionRule o2) {
				int cmp = o1.start().compareTo(o2.start());
				if (cmp != 0) return cmp;
				return Integer.compare(o1.lineNumber, o2.lineNumber);
			}
		});
		String inFile = tokens.getName();
		ParsedTokens ret = new ParsedTokens();
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(tokens))) {
			String s;
			InputPosition pos = null;
			ReductionRule pendingRule = null;
			while ((s = lnr.readLine()) != null) {
				if (pendingRule != null) {
					pendingRule.range(pos, readPos(inFile, s));
					starting.add(pendingRule);
					pendingRule = null;
					pos = null;
				} else if (pos == null)
					pos = readPos(inFile, s);
				else {
					GrammarStep step = readToken(pos, s);
					if (step instanceof GrammarToken) {
						GrammarToken tok = (GrammarToken) step;
						if (ret.tokens.contains(tok)) {
							ret.tokens.remove(tok);
						}
						ret.tokens.add(tok);
						pos = null;
					} else if (step instanceof ReductionRule) {
						pendingRule = (ReductionRule)step;
						pendingRule.lineNo(lnr.getLineNumber());
					} else
						throw new NotImplementedException();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		ret.sortReductionsIntoFileContainingOrder(starting);
		return ret;
	}

	private void sortReductionsIntoFileContainingOrder(Set<ReductionRule> starting) {
		reductionsInFileOrder.addAll(starting);
		outer:
		for (int i=0;i<reductionsInFileOrder.size();) {
			ReductionRule moveDown = reductionsInFileOrder.get(i);
			for (int j=i+1;j<reductionsInFileOrder.size();j++) {
				ReductionRule after = reductionsInFileOrder.get(j);
				
				// if they're equal, go round again
				if (moveDown.first.compareTo(after.first) == 0 && moveDown.last.compareTo(after.last) == 0) {
					System.out.println("Not moving " + i + ": " + moveDown + " -- " + j + ": " + after + " because they are the same");
					continue;
				}

				// if after is around moveDown, it's not moving
				if (moveDown.first.compareTo(after.first) > 0 && moveDown.last.compareTo(after.last) < 0) {
					System.out.println("Not moving " + i + ": " + moveDown + " -- " + j + ": " + after + " because it is inside it");
					continue;
				}

				boolean move = false;
				// if moveDown is around after, it needs to moveDown
				if (moveDown.first.compareTo(after.first) < 0 && moveDown.last.compareTo(after.last) > 0)
					move = true;
				
				// if they moveDown ends beyond where after starts, and either
				//   * it starts before it OR
				//   * it ends before it
				// it needs to move down
				if (moveDown.last.equals(after.first) && 
						(moveDown.first.compareTo(after.first) <= 0 ||
						 moveDown.last.compareTo(after.last) < 0)) {
					move = true;
				}
				
				if (move) {
					System.out.println("Moving " + i + ": " + moveDown + " after " + j + ": " + after);
					for (int k=i+1;k<=j;k++) {
						reductionsInFileOrder.set(k-1, reductionsInFileOrder.get(k));
					}
					reductionsInFileOrder.set(j, moveDown);
					continue outer;
				}
			}
			i++;
		}
	}

	private static InputPosition readPos(String file, String s) {
		int idx1 = s.indexOf(":");
		int idx2 = s.indexOf(".", idx1);
		int idx3 = s.indexOf(":", idx2);
		int line = Integer.parseInt(s.substring(0, idx1));
		int tabs = Integer.parseInt(s.substring(idx1+1, idx2));
		int spaces = Integer.parseInt(s.substring(idx2+1, idx3));
		int offset = Integer.parseInt(s.substring(idx3+1));
		return new InputPosition(file, line, offset, new Indent(tabs, spaces), s);
	}

	private static GrammarStep readToken(InputPosition pos, String s) {
		if (s.startsWith("token ")) {
			int idx = s.indexOf(" ")+1;
			int idx2 = s.indexOf(" ", idx);
			return new GrammarToken(pos, s.substring(idx, idx2), s.substring(idx2+1));
		} else if (s.startsWith("reduction ")) {
			int idx = s.indexOf(" ")+1;
			return new ReductionRule(s.substring(idx));
		} else
			throw new CantHappenException("what is this? " + s);
	}

	public void write(File file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		for (GrammarStep s : this) {
			if (s instanceof GrammarToken)
				pw.println("   SHIFT  " + s);
			else {
				ReductionRule rr = (ReductionRule) s;
				if (rr.isMostReduced())
					pw.print("MR ");
				else
					pw.print("   ");
				pw.println("REDUCE " + rr);
			}
		}
		pw.close();
	}

	public Iterator<GrammarStep> iterator() {
		return new Iterator<ParsedTokens.GrammarStep>() {
			Iterator<ReductionRule> rit = reductionsInFileOrder().iterator();
			Iterator<GrammarToken> tit = tokens().iterator();
			ReductionRule r = null;
			GrammarToken t = null;
			
			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return rit.hasNext() || tit.hasNext() || r != null || t != null;
			}
			
			@Override
			public GrammarStep next() {
				GrammarStep ret = null;
				if (r == null && rit.hasNext())
					r= rit.next();
				if (t == null && tit.hasNext())
					t = tit.next();
				if (r == null || (t != null && t.pos.compareTo(r.last) <= 0)) {
					ret = t;
					t = null;
				} else {
					ret = r;
					r = null;
				}
				return ret;
			}
		};
	}
	
	public Iterable<GrammarToken> tokens() {
		return tokens;
	}
	
	public Iterable<ReductionRule> reductionsInFileOrder() {
		return reductionsInFileOrder;
	}

	public Iterable<ReductionRule> mostReduced() {
		return new Iterable<>() {
			@Override
			public Iterator<ReductionRule> iterator() {
				return new Iterator<ReductionRule>() {
					Iterator<ReductionRule> it = reductionsInFileOrder.iterator();
					ReductionRule next = findNext();
					
					private ReductionRule findNext() {
						while (it.hasNext()) {
							ReductionRule ret = it.next();
							if (ret.isMostReduced())
								return ret;
						}
						return null;
					}

					@Override
					public boolean hasNext() {
						return next != null;
					}

					@Override
					public ReductionRule next() {
						ReductionRule ret = next;
						next = findNext();
						return ret;
					}
				};
			}
		};
	}
}
