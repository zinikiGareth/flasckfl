package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.List;

public class TypeScheme {
	public final List<TypeVar> schematicVars;
	public final Object typeExpr;
	// some method to return all the unknowns in typeExpr
	// some method to apply a substitution to just the unknowns in typeExpr

	public TypeScheme(List<TypeVar> scvs, Object typeExpr) {
		if (scvs == null)
			schematicVars = new ArrayList<TypeVar>();
		else
			schematicVars = scvs;
		this.typeExpr = typeExpr;
	}
	
	// See PH p173
	public TypeScheme subst(TypeVariableMappings phi) {
		TypeVariableMappings phi2 = phi.exclude(schematicVars);
		return new TypeScheme(schematicVars, phi2.subst(typeExpr));
	}
	
	@Override
	public String toString() {
		return "(SCHEME " + schematicVars + " " + typeExpr +")";
	}
}
