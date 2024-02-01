package org.flasck.flas.testing.golden;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.CantHappenException;
import org.zinutils.exceptions.NotImplementedException;

public class ParsedTokens {

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

		public ReductionRule(String rule) {
			this.rule = rule;
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

	Comparator<ReductionRule> lineOrder = new Comparator<>() {
		@Override
		public int compare(ReductionRule o1, ReductionRule o2) {
//			int cmp;
//			cmp = this.last.compareTo(o.last);
//			if (cmp != 0)
//				return cmp;
//			cmp = this.first.compareTo(o.first);
//			if (cmp != 0)
//				return cmp;
			return Integer.compare(o1.lineNumber, o2.lineNumber);
		}
	};
	/*
	Comparator<ReductionRule> startOrder = new Comparator<>() {
		@Override
		public int compare(ReductionRule o1, ReductionRule o2) {
			if (o1 == o2)
				return 0;
			
//			int cmp;
			
			// If one ends where the other starts, it owns it and must come after it
			if (o1.last.compareTo(o2.first) == 0)
				return 1;
			else if (o2.last.compareTo(o1.first) == 0)
				return -1;
			
			// the one that ends first comes first
			if (o1.last.compareTo(o2.last) < 0)
				return -1;
			else if (o2.last.compareTo(o1.last) < 0)
				return 1;

			//			cmp = 
//			if (cmp != 0)
//				return cmp;
//			cmp = o1.first.compareTo(o2.first);
//			if (cmp != 0)
//				return cmp;
//			return Integer.compare(o1.lineNumber, o2.lineNumber);
			throw new NotImplementedException();
		}
	};
	*/

	
	private Set<GrammarToken> tokens = new TreeSet<>();
	private Set<ReductionRule> reductionsInLineOrder = new TreeSet<>(lineOrder);
	private List<ReductionRule> reductionsInFileOrder = new ArrayList<>();

	private ParsedTokens() {
	}

	public static ParsedTokens read(File tokens) {
		String inFile = tokens.getName();
		ParsedTokens ret = new ParsedTokens();
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(tokens))) {
			String s;
			InputPosition pos = null;
			ReductionRule pendingRule = null;
			while ((s = lnr.readLine()) != null) {
				if (pendingRule != null) {
					pendingRule.range(pos, readPos(inFile, s));
					ret.reductionsInLineOrder.add(pendingRule);
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
		ret.sortReductionsIntoFileContainingOrder();
		return ret;
	}

	private void sortReductionsIntoFileContainingOrder() {
		Set<ReductionRule> starting = new TreeSet<ReductionRule>(new Comparator<ReductionRule>() {
			public int compare(ReductionRule o1, ReductionRule o2) {
				int cmp = o1.start().compareTo(o2.start());
				if (cmp != 0) return cmp;
//				cmp = o1.last.compareTo(o2.last);
//				if (cmp != 0) return cmp;
				return Integer.compare(o1.lineNumber, o2.lineNumber);
			}
		});
		starting.addAll(reductionsInLineOrder);
		reductionsInFileOrder.addAll(starting);
		outer:
		for (int i=0;i<reductionsInFileOrder.size();) {
			ReductionRule moveDown = reductionsInFileOrder.get(i);
			for (int j=i+1;j<reductionsInFileOrder.size();j++) {
				ReductionRule after = reductionsInFileOrder.get(j);
				if (moveDown.last.equals(after.first) && 
						(moveDown.first.compareTo(after.first) < 0 ||
						 moveDown.last.compareTo(after.last) < 0)) {
					for (int k=i+1;k<=j;k++) {
						reductionsInFileOrder.set(k-1, reductionsInFileOrder.get(k));
					}
					reductionsInFileOrder.set(j, moveDown);
					continue outer;
				}
			}
			i++;
		}
		System.out.println(reductionsInLineOrder.size() + " == " + reductionsInFileOrder.size());
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

	public void write(File file) {
		// The idea here is that we could write the tokens back out once we have sorted them
		for (ReductionRule e : this.reductionsInFileOrder) {
			System.out.println("  " + e);
		}
	}
	
	public Iterable<GrammarToken> tokens() {
		return tokens;
	}
	
	public Iterable<ReductionRule> reductionsInLineOrder() {
		return reductionsInLineOrder;
	}
	
	public Iterable<ReductionRule> reductionsInFileOrder() {
		return reductionsInFileOrder;
	}
}
