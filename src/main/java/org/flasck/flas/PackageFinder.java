package org.flasck.flas;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
import org.flasck.flas.typechecker.CardTypeInfo;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

public class PackageFinder {
	private final List<File> dirs = new ArrayList<File>();
	
	public Scope loadFlim(Scope rootScope, String pkgName) {
		for (File d : dirs) {
			File pkg = new File(d, pkgName);
			if (pkg.isDirectory()) {
				// Create the scope
				String tmp = pkgName;
				Scope scope = rootScope;
				while (tmp != null) {
					int idx = tmp.indexOf('.');
					String pp = (idx == -1)?tmp:tmp.substring(0, idx);
					Object o = scope.get(pp);
					if (o == null) {
						o = new PackageDefn(new InputPosition(pkg.getName(), 0, 0, pkgName), scope, pp);
					}
					scope = ((PackageDefn)o).innerScope();
					if (idx == -1)
						tmp = null;
					else
						tmp = tmp.substring(idx+1);
				}
				
				// Load definitions into it
				ObjectInputStream ois = null;
				try {
					File file = new File(pkg, pkgName + ".flim");
					System.out.println("Loading definitions for " + pkgName + " from " + file);
					ois = new ObjectInputStream(new FileInputStream(file));
					@SuppressWarnings("unchecked")
					List<StructDefn> structs = (List<StructDefn>) ois.readObject();
					for (StructDefn sd : structs) {
						int idx = sd.name().lastIndexOf(".");
						scope.define(sd.name().substring(idx+1), sd.name(), sd);
					}
					@SuppressWarnings("unchecked")
					List<UnionTypeDefn> types = (List<UnionTypeDefn>) ois.readObject();
					for (UnionTypeDefn td : types) {
						int idx = td.name().lastIndexOf(".");
						scope.define(td.name().substring(idx+1), td.name(), td);
					}
					@SuppressWarnings("unchecked")
					List<ContractDecl> contracts = (List<ContractDecl>)ois.readObject();
					for (ContractDecl c : contracts) {
						int idx = c.name().lastIndexOf(".");
						scope.define(c.name().substring(idx+1), c.name(), c);
					}
					@SuppressWarnings("unchecked")
					List<CardTypeInfo> cards = (List<CardTypeInfo>) ois.readObject();
					for (CardTypeInfo c : cards) {
						int idx = c.name.lastIndexOf(".");
						scope.define(c.name.substring(idx+1), c.name, c);
					}
					@SuppressWarnings("unchecked")
					Map<String, Type> knowledge = (Map<String, Type>) ois.readObject();
					for (Entry<String, Type> t : knowledge.entrySet()) {
						int idx = t.getKey().lastIndexOf(".");
						scope.define(t.getKey().substring(idx+1), t.getKey(), t.getValue());
					}
				} catch (Exception ex) {
					throw UtilException.wrap(ex);
				} finally {
					try { if (ois != null) ois.close(); } catch (Exception ex) {}
				}
				return scope;
			}
		}
		return null;
	}

	public void searchIn(File file) {
		if (!file.isDirectory())
			throw new ArgumentException("Cannot search for FLIMs in " + file + " as it is not a valid directory");
		dirs.add(file);
	}
}
