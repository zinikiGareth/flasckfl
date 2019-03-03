package org.flasck.flas.parser;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.jsoup.internal.StringUtil;

public class StreamDumper implements StackDumper {
	private final PrintWriter writer;
	private String padding;

	public StreamDumper(PrintStream out) {
		writer = new PrintWriter(out);
		padding = "";
	}

	public StreamDumper(PrintWriter w, String padding) {
		writer = w;
		this.padding = padding;
	}

	@Override
	public StackDumper indent(int i) {
		return new StreamDumper(writer, StringUtil.padding(2*i));
	}

	@Override
	public void levels(int size) {
		writer.println("Have " + size + " levels");
	}

	@Override
	public void dump(List<Expr> terms) {
		writer.println(padding + terms);
		writer.flush();
	}
}
