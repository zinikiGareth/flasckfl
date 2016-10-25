package org.flasck.flas.golden;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Indenter {
	private final PrintWriter pw;
	private final int ind;
	private boolean atStart = true;

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

	public void print(String s) {
		if (atStart) {
			for (int i=0;i<ind;i++)
				pw.print(" ");
		}
		pw.print(s);
		pw.flush();
		atStart = false;
	}
	
	public void println(String s) {
		print(s);
		pw.println();
		pw.flush();
		atStart = true;
	}
	
	public void flush() {
		pw.flush();
	}
	
	public void close() {
		pw.close();
	}
}
