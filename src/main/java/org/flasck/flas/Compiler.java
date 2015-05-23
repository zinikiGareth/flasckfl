package org.flasck.flas;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.stories.FLASStory;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.zinutils.exceptions.UtilException;
import org.zinutils.utils.FileUtils;

public class Compiler {
	public static void main(String[] args) {
		Compiler compiler = new Compiler();
		for (String f : args)
			compiler.compile(new File(f));
	}

	private final Generator gen = new Generator();
	
	public void compile(File file) {
		// TODO: figure out the package thing
		File writeTo = new File(file.getParentFile(), file.getName().replace(".fl", ".js"));
		FileWriter w = null;
		FileReader r = null;
		try {
			w = new FileWriter(writeTo);
			r = new FileReader(file);
			w.write("function PKG() {}\n\n");
			List<Block> blocks = Blocker.block(r);
			Object obj = new FLASStory().process(blocks);
			if (obj instanceof Scope) {
				for (Entry<String, Object> x : (Scope)obj) {
					if (x.getValue() instanceof FunctionDefinition) {
						HSIEForm hsie = HSIE.handle((FunctionDefinition) x.getValue());
						JSForm js = gen.generate(hsie);
						js.writeTo(w);
						w.write("\n");
					} else if (x.getValue() instanceof StructDefn) {
						StructDefn sd = (StructDefn) x.getValue();
						JSForm js = gen.generate(sd);
						w.write("PKG.");
						js.writeTo(w);
						w.write("\n");
					} else if (x.getValue() instanceof ContractDecl) {
						// currently, I don't think anything needs to be written in this case
						continue;
					} else if (x.getValue() instanceof CardDefinition) {
						CardDefinition sd = (CardDefinition) x.getValue();
						List<JSForm> forms = gen.generate(sd);
						for (JSForm js : forms) {
							w.write("PKG.");
							js.writeTo(w);
							w.write("\n");
						}
					} else
						throw new UtilException("Need to handle " + x.getKey() + " of type " + x.getValue().getClass());
				}
				w.write("PKG;\n");
			} else if (obj instanceof ErrorResult) {
				((ErrorResult)obj).showTo(new PrintWriter(System.out));
			} else
				System.err.println("Failed to parse; got " + obj);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (w != null) try { w.close(); } catch (IOException ex) {}
			if (r != null) try { r.close(); } catch (IOException ex) {}
		}
		FileUtils.copyFileToStream(writeTo, System.out);
	}
}
