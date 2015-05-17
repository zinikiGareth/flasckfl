package org.flasck.flas.hsieForm;

public class FunctionDefinition {
	// So, basically an HSIE definition consists of
	// Fn "name" [formal-args] [external-vars]
	//   HEAD var
	//   SWITCH var Type/Constructor|Type|Type/Constructor
	//     BIND new-var var "field"
	//     IF boolean-expr
	//       EVAL En
	//   Er
	// If there is no general case, then add "E?" to indicate an error in switching
	
	// There is no notion of "Else", you just drop down to the next statement at a not-indented level and pick up from there.
	
	// Each of the Expressions En is modified to be just a simple apply-tree
	
	// FunctionDefinition also has nested Scope
}
