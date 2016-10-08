package org.flasck.flas.golden;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Indenter {
	private final PrintWriter pw;
	private final int ind;

	public Indenter(PrintWriter pw) {
		this.pw = pw;
		ind = 0;
	}
	
	public Indenter(File file) throws FileNotFoundException {
		this.pw = new PrintWriter(file);
		ind = 0;
	}
	
	private Indenter(PrintWriter pw, int ind) {
		this.pw = pw;
		this.ind = ind;
	}

	public Indenter indent() {
		return new Indenter(pw, ind+2);
	}
	
	public void println(String s) {
		for (int i=0;i<ind;i++)
			pw.print(" ");
		pw.println(s);
	}
	
	public void flush() {
		pw.flush();
	}
	
	public void close() {
		pw.close();
	}
}
