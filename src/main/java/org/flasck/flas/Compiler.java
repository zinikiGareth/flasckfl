package org.flasck.flas;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.vcode.hsieForm.HSIEForm;

public class Compiler {
	public static void main(String[] args) {
		Compiler compiler = new Compiler();
		for (String f : args)
			compiler.compile(new File(f));
	}

	private final Generator gen = new Generator();
	
	public void compile(File file) {
		// TODO: figure out the package thing
		FileWriter w = null;
		FileReader r = null;
		try {
			File writeTo = new File(file.getParentFile(), file.getName().replace(".fl", ".js"));
			w = new FileWriter(writeTo);
			r = new FileReader(file);
			List<Block> blocks = Blocker.block(r);
			Object obj = new FLASStory().process(blocks);
			if (obj instanceof Scope) {
				System.out.println(obj);
				for (Entry<String, Object> x : (Scope)obj) {
					if (x.getValue() instanceof FunctionDefinition) {
						HSIEForm hsie = HSIE.handle((FunctionDefinition) x.getValue());
						JSForm js = gen.generate(hsie);
						js.writeTo(w);
					}
					else
						System.out.println("Cannot handle " + x);
				}
			} else
				System.err.println("Failed to parse; got " + obj);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (w != null) try { w.close(); } catch (IOException ex) {}
			if (r != null) try { r.close(); } catch (IOException ex) {}
		}
	}
}
