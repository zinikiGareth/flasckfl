package org.flasck.flas;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.compiler.FLASCompiler;
import org.flasck.flas.errors.ErrorResultException;

public class Main {
	public static void main(String[] args) {
		setLogLevels();
		Configuration config = new Configuration();
		boolean failed = false;
		try {
			List<File> inputs = config.process(args);
			if (inputs.isEmpty()) {
				System.err.println("No input directories specified");
				return;
			}
			FLASCompiler compiler = new FLASCompiler(config);
			for (File input : inputs)
				failed |= processInput(compiler, input);
			if (!failed && compiler.getBuilder() != null)
				compiler.getBuilder().build();
		} catch (ArgumentException ex) {
			System.err.println(ex.getMessage());
			failed = true;
		}
		System.exit(failed?1:0);
	}

	private static boolean processInput(FLASCompiler compiler, File input) {
		boolean failed = false;
		try {
			failed |= compiler.compile(input) == null;
		} catch (ErrorResultException ex) {
			try {
				ex.errors.showTo(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true), 4);
			} catch (IOException ex2) {
				ex2.printStackTrace();
			}
			failed = true;
		} catch (Throwable ex) {
			ex.printStackTrace();
			failed = true;
		}
		return failed;
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
