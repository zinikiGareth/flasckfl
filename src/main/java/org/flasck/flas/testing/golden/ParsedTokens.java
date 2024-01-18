package org.flasck.flas.testing.golden;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.flasck.flas.blockForm.Indent;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.testing.golden.ParsedTokens.GrammarToken;

public class ParsedTokens implements Iterable<GrammarToken> {

	public static class GrammarToken implements Comparable<GrammarToken> {
		public final InputPosition pos;
		public final String text;

		public GrammarToken(InputPosition pos, String text) {
			this.pos = pos;
			this.text = text;
		}

		@Override
		public int compareTo(GrammarToken o) {
			return pos.compareTo(o.pos);
		}
		
		@Override
		public String toString() {
			return pos.toString() + ":***" + text + "***";
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
	}

	private Set<GrammarToken> tokens = new TreeSet<>();
	private ParsedTokens() {
	}

	public static ParsedTokens read(File tokens) {
		ParsedTokens ret = new ParsedTokens();
		try (LineNumberReader lnr = new LineNumberReader(new FileReader(tokens))) {
			String s;
			InputPosition pos = null;
			while ((s = lnr.readLine()) != null) {
				if (pos == null)
					pos = readPos(tokens.getName(), s);
				else {
					ret.tokens.add(readToken(pos, s));
					pos = null;
				}
			}
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

	private static GrammarToken readToken(InputPosition pos, String s) {
		int idx = s.indexOf(" ");
		return new GrammarToken(pos, s.substring(idx+1));
	}
	
	@Override
	public Iterator<GrammarToken> iterator() {
		return tokens.iterator();
	}
}
