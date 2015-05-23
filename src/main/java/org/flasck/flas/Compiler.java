package org.flasck.flas;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.Block;
import org.flasck.flas.blocker.Blocker;
import org.flasck.flas.hsie.HSIE;
import org.flasck.flas.jsform.JSForm;
import org.flasck.flas.jsgen.Generator;
import org.flasck.flas.method.MethodConvertor;
import org.flasck.flas.parsedForm.CardDefinition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.MethodDefinition;
import org.flasck.flas.parsedForm.PackageDefn;
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
		String defPkg = file.getParentFile().getName();
		System.out.println("default package would be " + defPkg);
		File writeTo = new File(file.getParentFile(), file.getName().replace(".fl", ".js"));
		FileWriter w = null;
		FileReader r = null;
		try {
			w = new FileWriter(writeTo);
			r = new FileReader(file);
			List<Block> blocks = Blocker.block(r);
			List<JSForm> forms = new ArrayList<JSForm>();
			Object obj = new FLASStory().process(defPkg, blocks);
			if (obj instanceof ErrorResult) {
				((ErrorResult)obj).showTo(new PrintWriter(System.out));
			} else if (obj instanceof Scope) {
				Scope scope = (Scope) obj;
				List<String> pkglist = emitPackages(forms, scope, defPkg);
				processScope(forms, scope);
				for (JSForm js : forms) {
					js.writeTo(w);
					w.write("\n");
				}
				if (pkglist.size() == 1)
					w.write(pkglist.get(0) + ";\n");
				else {
					w.write("{ ");
					w.write(String.join(", ", pkglist));
					w.write(" }\n");
				}
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

	private void processScope(List<JSForm> forms, Scope scope) {
		for (Entry<String, Object> x : scope) {
			String name = x.getKey();
			Object val = x.getValue();
			if (val instanceof PackageDefn) {
				processScope(forms, ((PackageDefn) val).innerScope());
			} else if (val instanceof FunctionDefinition) {
				HSIEForm hsie = HSIE.handle((FunctionDefinition) val);
				forms.add(gen.generate(hsie));
			} else if (val instanceof StructDefn) {
				StructDefn sd = (StructDefn) val;
				forms.add(gen.generate(name, sd));
			} else if (val instanceof ContractDecl) {
				// currently, I don't think anything needs to be written in this case
				continue;
			} else if (val instanceof CardDefinition) {
				CardDefinition card = (CardDefinition) val;
				
				forms.add(gen.generate(name, card));
				
				for (ContractImplements ci : card.contracts) {
					forms.add(gen.generateImplements(name, ci));
					for (MethodDefinition m : ci.methods) {
						FunctionDefinition fd = MethodConvertor.convert(name, ci.type, m);
						HSIEForm hsie = HSIE.handle(fd);
						forms.add(gen.generate(hsie));
					}
				}
				for (HandlerImplements hi : card.handlers) {
					forms.add(gen.generateImplements(name, hi));
					for (MethodDefinition m : hi.methods) {
						FunctionDefinition fd = MethodConvertor.convert(name, hi.type, m);
						HSIEForm hsie = HSIE.handle(fd);
						forms.add(gen.generate(hsie));
					}
				}
			} else
				throw new UtilException("Need to handle " + x.getKey() + " of type " + val.getClass());
		}
	}

	private List<String> emitPackages(List<JSForm> forms, Scope scope, String defPkg) {
		boolean havePkg = false;
		List<String> plist = new ArrayList<String>();
		for (Entry<String, Object> o : scope) {
			if (o.getValue() instanceof PackageDefn) {
				havePkg = true;
				assertPackage(forms, plist, o.getKey());
			}
		}
		if (!havePkg) {
			assertPackage(forms, plist, defPkg);
		}
		return plist;
	}

	private void assertPackage(List<JSForm> forms, List<String> plist, String key) {
		String keydot = key+".";
		int idx = -1;
		while ((idx = keydot.indexOf('.', idx+1))!= -1) {
			String tmp = keydot.substring(0, idx);
			forms.add(JSForm.packageForm(tmp));
//			plist.add(tmp);
			System.out.println(idx);
		}
		plist.add(key);
	}
}
