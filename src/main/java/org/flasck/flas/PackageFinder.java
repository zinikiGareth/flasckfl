package org.flasck.flas;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.PackageDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnionTypeDefn;
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
					ois = new ObjectInputStream(new FileInputStream(new File(pkg, pkgName + ".flim")));
					@SuppressWarnings("unchecked")
					List<StructDefn> structs = (List<StructDefn>) ois.readObject();
					@SuppressWarnings("unchecked")
					List<UnionTypeDefn> types = (List<UnionTypeDefn>) ois.readObject();
					@SuppressWarnings("unchecked")
					List<ContractDecl> contracts = (List<ContractDecl>)ois.readObject();
					for (ContractDecl c : contracts) {
						int idx = c.contractName.lastIndexOf(".");
						scope.define(c.contractName.substring(idx+1), c.contractName, c);
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
			throw new UtilException("Can only search for FLIMs in directories");
		dirs.add(file);
	}
}
