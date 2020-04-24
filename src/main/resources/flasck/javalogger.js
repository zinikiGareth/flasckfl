const JavaLogger = {};
JavaLogger.log = function() {
	var ret = '';
	var sep = '';
	for (var i=0;i<arguments.length;i++) {
		ret += sep + arguments[i];
		sep = ' ';
	}
	callJava.log(ret);
};

window.JavaLogger = JavaLogger;
