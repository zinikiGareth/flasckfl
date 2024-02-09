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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
	private List<GrammarStep> readingOrder = new ArrayList<>();

	private ParsedTokens() {
	}

	public static ParsedTokens read(File tokens) {
		Set<ReductionRule> starting = new TreeSet<ReductionRule>(
			new Comparator<ReductionRule>() {
				public int compare(ReductionRule o1, ReductionRule o2) {
					int cmp = o1.start().compareTo(o2.start());
					if (cmp != 0) return cmp;
					return Integer.compare(o1.lineNumber, o2.lineNumber);
				}
			}
		);
		String inFile = tokens.getName();
		ParsedTokens ret = new ParsedTokens();
		
		// One of the problems is that tokens are logged multiple times.
		// First, get the DEFINITIVE list of the tokens that apply by reading the file and using a set based on location
		
		Map<InputPosition, Integer> tokenLines = new TreeMap<>();
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(tokens))) {
			String s;
			InputPosition pos = null;
			GrammarStep pendingRule = null;
			while ((s = lnr.readLine()) != null) {
				if (pos == null)
					pos = readPos(inFile, s);
				else if (pendingRule != null) {
					// ignore this
					readPos(inFile, s);
					pos = null;
					pendingRule = null;
				} else {
					GrammarStep step = readToken(pos, s);
					if (step instanceof GrammarToken) {
						tokenLines.put(pos, lnr.getLineNumber());
						pos = null;
					} else {
						pendingRule = step;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Thus figure out which lines have valid tokens on them
		Set<Integer> onlyTokenLines = new TreeSet<>(tokenLines.values());
		
		// Now go through again and process all the tokens and reductions in order,
		// ignoring any tokens which are not valid
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(tokens))) {
			String s;
			InputPosition pos = null;
			ReductionRule pendingRule = null;
			while ((s = lnr.readLine()) != null) {
				if (pos == null)
					pos = readPos(inFile, s);
				else if (pendingRule != null) {
					InputPosition endPos = readPos(inFile, s);
					pendingRule.range(pos, endPos);
					starting.add(pendingRule);
					ret.readingOrder.add(pendingRule);
					pendingRule = null;
					pos = null;
				} else {
					GrammarStep step = readToken(pos, s);
					if (step instanceof GrammarToken) {
						if (!onlyTokenLines.contains(lnr.getLineNumber())) {
							pos = null;
							continue;
						}
						GrammarToken tok = (GrammarToken) step;
						if (ret.tokens.contains(tok)) {
							throw new CantHappenException("should have skipped the other one");
						}
						ret.tokens.add(tok);
						ret.readingOrder.add(tok);
						pos = null;
					} else if (step instanceof ReductionRule) {
						pendingRule = (ReductionRule)step;
						pendingRule.lineNo(lnr.getLineNumber());
					} else
						throw new NotImplementedException();
				}
			}
			File ff = new File(tokens.getParentFile(), tokens.getName() + "-reading");
			PrintWriter pw = new PrintWriter(ff);
			for (GrammarStep gs : ret.readingOrder) {
				pw.println(gs);
			}
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
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
		return readingOrder.iterator();
	}
	
	public Iterable<GrammarToken> tokens() {
		return tokens;
	}
	
	public Iterable<ReductionRule> mostReduced() {
		return new Iterable<>() {
			@Override
			public Iterator<ReductionRule> iterator() {
				return new Iterator<ReductionRule>() {
					Iterator<GrammarStep> it = readingOrder.iterator();
					ReductionRule next = findNext();
					
					private ReductionRule findNext() {
						while (it.hasNext()) {
							GrammarStep ret = it.next();
							if (ret instanceof ReductionRule && ((ReductionRule)ret).isMostReduced())
								return (ReductionRule) ret;
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

	public Iterable<ReductionRule> reductions() {
		return new Iterable<>() {
			@Override
			public Iterator<ReductionRule> iterator() {
				return new Iterator<ReductionRule>() {
					Iterator<GrammarStep> it = readingOrder.iterator();
					ReductionRule next = findNext();
					
					private ReductionRule findNext() {
						while (it.hasNext()) {
							GrammarStep ret = it.next();
							if (ret instanceof ReductionRule)
								return (ReductionRule) ret;
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
