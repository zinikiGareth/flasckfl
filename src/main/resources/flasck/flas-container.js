

const Application = function(_cxt) {
	this.cards = {};
}

Application.prototype.gotoRoute = function(_cxt, r) {
	var card = new this.mainCard(_cxt);

	var ctr = _cxt.findContractOnCard(card, "Lifecycle");
	if (ctr && ctr.load) {
		_cxt.env.queueMessages(_cxt, ctr.load(_cxt, "loaded"));
	}

	// the main card is always called "main"
	this.cards.main = card;
}

	window.Application = Application;

const ContainerRepeater = function() {
}

ContainerRepeater.prototype.callMe = function(cx, callback) {
    return Send.eval(cx, callback, "call", []);
}


