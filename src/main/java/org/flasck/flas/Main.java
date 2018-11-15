package org.flasck.flas;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.flasck.flas.compiler.FLASCompiler;

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
			compiler.errorWriter(new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true));
			compiler.scanWebZips();
			for (File input : inputs)
				processInput(compiler, input);
			if (!compiler.hasErrors() && compiler.getBuilder() != null)
				compiler.getBuilder().build();
			failed = compiler.hasErrors();
		} catch (ArgumentException ex) {
			System.err.println(ex.getMessage());
			failed = true;
		}
		System.exit(failed?1:0);
	}

	private static void processInput(FLASCompiler compiler, File input) {
		try {
			compiler.parse(input);
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
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
