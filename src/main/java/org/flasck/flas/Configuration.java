package org.flasck.flas;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
	private boolean unitjvm = false, unitjs = false;
	private boolean dumpTypes;
	private List<File> searchFlim = new ArrayList<>();
	private File writeFlim;
	private File writeHSIE;
	private File writeJS;
	private File writeDroid;
	private File writeJVM;
	private File webZipDownloads;
	private File webZipDir;
	private List<String> useWebZips = new ArrayList<>();
	private boolean buildDroid = true;
	boolean tda = true;

	public Configuration() {
		// TODO Auto-generated constructor stub
	}

	public List<File> process(String[] args) {
		List<File> inputs = new ArrayList<File>();
		for (int i=0;i<args.length;i++) {
			String arg = args[i];
			int hasMore = args.length-i-1;
			if (arg.startsWith("-")) {
				if (arg.equals("--dump"))
					dumpTypes = true;
				else if (arg.equals("--flim")) {
					if (hasMore == 0) {
						System.out.println("--flim <dir>");
						System.exit(1);
					}
					searchFlim.add(new File(args[++i]));
				} else if (arg.equals("--wflim")) {
					if (hasMore == 0) {
						System.out.println("--wflim <dir>");
						System.exit(1);
					}
					writeFlim = new File(args[++i]);
				} else if (arg.equals("--hsie")) {
					if (hasMore == 0) {
						System.out.println("--hsie <dir>");
						System.exit(1);
					}
					writeHSIE = new File(args[++i]);
				} else if (arg.equals("--jsout")) {
					if (hasMore == 0) {
						System.out.println("--jsout <dir>");
						System.exit(1);
					}
					writeJS = new File(args[++i]);
				} else if (arg.equals("--unitjs")) {
					unitjs = true;
				} else if (arg.equals("--unitjvm")) {
					unitjvm = true;
				} else if (arg.equals("--android")) {
					if (hasMore == 0) {
						System.out.println("--android <build-dir>");
						System.exit(1);
					}
					writeDroid = new File(args[++i]);
				} else if (arg.equals("--jvm")) {
					if (hasMore == 0) {
						System.out.println("--jvm <build-dir>");
						System.exit(1);
					}
					writeJVM = new File(args[++i]);
				} else if (arg.equals("--webzipdownloads")) {
					if (hasMore == 0) {
						System.out.println("--webzipdownloads <download-dir>");
						System.exit(1);
					}
					webZipDownloads = new File(args[++i]);
				} else if (arg.equals("--webzipdir")) {
					if (hasMore == 0) {
						System.out.println("--webzipdir <dir>");
						System.exit(1);
					}
					webZipDir = new File(args[++i]);
				} else if (arg.equals("--webzip")) {
					if (hasMore == 0) {
						System.out.println("--webzip <name>");
						System.exit(1);
					}
					useWebZips.add(args[++i]);
				} else if (arg.equals("--legacy")) {
					tda = false;
				} else {
					boolean matched = false;
					/*
					DroidBuilder builder = compiler.getBuilder();
					if (builder != null) { // consider droid build options
						matched = true;
						if (arg.equals("--clean")) {
							builder.cleanFirst();
						} else
							matched = false;
					}
					*/
					if (!matched) {
						System.out.println("unknown option: " + arg);
						return null;
					}
				}
			} else
				inputs.add(new File(arg));
		}
		return inputs;
	}

	public void visit(ConfigVisitor visitor) {
		for (File f : searchFlim)
			visitor.searchIn(f);
		visitor.dumpTypes(dumpTypes);
		visitor.writeFlimTo(writeFlim);
		visitor.writeHSIETo(writeHSIE);
		visitor.writeJSTo(writeJS);
		visitor.writeJVMTo(writeJVM);
		visitor.writeDroidTo(writeDroid, buildDroid );
		visitor.webZipDir(webZipDir);
		visitor.webZipDownloads(webZipDownloads);
		for (String s : useWebZips)
			visitor.useWebZip(s);
		if (unitjs || !unitjvm)
			visitor.unitjs(true);
		if (unitjvm)
			visitor.unitjvm(true);
	}

}
