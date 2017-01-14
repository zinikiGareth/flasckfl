package org.flasck.flas;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.builder.droid.DroidBuilder;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResultException;

public class Main {
	public static void main(String[] args) {
		setLogLevels();
		FLASCompiler compiler = new FLASCompiler();
		List<File> inputs = new ArrayList<File>();
		boolean argError = false;
		boolean unitjvm = false, unitjs = false;
		try {
			for (int i=0;i<args.length;i++) {
				String arg = args[i];
				int hasMore = args.length-i-1;
				if (arg.startsWith("-")) {
					if (arg.equals("--dump"))
						compiler.dumpTypes();
					else if (arg.equals("--flim")) {
						if (hasMore == 0) {
							System.out.println("--flim <dir>");
							System.exit(1);
						}
						compiler.searchIn(new File(args[++i]));
					} else if (arg.equals("--wflim")) {
						if (hasMore == 0) {
							System.out.println("--wflim <dir>");
							System.exit(1);
						}
						compiler.writeFlimTo(new File(args[++i]));
					} else if (arg.equals("--hsie")) {
						if (hasMore == 0) {
							System.out.println("--hsie <dir>");
							System.exit(1);
						}
						compiler.writeHSIETo(new File(args[++i]));
					} else if (arg.equals("--jsout")) {
						if (hasMore == 0) {
							System.out.println("--jsout <dir>");
							System.exit(1);
						}
						compiler.writeJSTo(new File(args[++i]));
					} else if (arg.equals("--unitjs")) {
						unitjs = true;
					} else if (arg.equals("--unitjvm")) {
						unitjvm = true;
					} else if (arg.equals("--android")) {
						if (hasMore == 0) {
							System.out.println("--android <build-dir>");
							System.exit(1);
						}
						compiler.writeDroidTo(new File(args[++i]), true);
					} else if (arg.equals("--jvm")) {
						if (hasMore == 0) {
							System.out.println("--jvm <build-dir>");
							System.exit(1);
						}
						compiler.writeDroidTo(new File(args[++i]), false);
					} else {
						boolean matched = false;
						DroidBuilder builder = compiler.getBuilder();
						if (builder != null) { // consider droid build options
							matched = true;
							if (arg.equals("--clean")) {
								builder.cleanFirst();
							} else
								matched = false;
						}
						if (!matched) {
							System.out.println("unknown option: " + arg);
							argError = true;
							break;
						}
					}
					continue;
				} else
					inputs.add(new File(arg));
			}
		} catch (ArgumentException ex) {
			System.err.println(ex.getMessage());
			argError = true;
		}
		if (inputs.isEmpty()) {
			System.err.println("No input directories specified");
			argError = true;
		}
		if (argError) {
			System.exit(1);
		}
		if (unitjs || !unitjvm)
			compiler.unitjs(true);
		if (unitjvm)
			compiler.unitjvm(true);
		boolean failed = false;
		for (File input : inputs) {
			try {
				failed |= compiler.compile(input) == null;
			} catch (ErrorResultException ex) {
				try {
					ex.errors.showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
				} catch (IOException ex2) {
					ex2.printStackTrace();
				}
				failed = true;
				break;
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}
		if (!failed && compiler.getBuilder() != null)
			compiler.getBuilder().build();
		System.exit(failed?1:0);
	}

	public static void setLogLevels() {
		LogManager.getLogger("Compiler").setLevel(Level.WARN);
		LogManager.getLogger("DroidGen").setLevel(Level.WARN);
		LogManager.getLogger("Generator").setLevel(Level.WARN);
		LogManager.getLogger("HSIE").setLevel(Level.WARN);
		LogManager.getLogger("Rewriter").setLevel(Level.ERROR);
		LogManager.getLogger("TypeChecker").setLevel(Level.WARN);
	}
}
