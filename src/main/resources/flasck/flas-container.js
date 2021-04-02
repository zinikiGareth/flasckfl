

const Application = function(_cxt) {
	this.cards = {};
}

Application.prototype.gotoRoute = function(_cxt, r) {
	var routing = this._routing();
	console.log("routing ", routing);
	var card = new this.mainCard(_cxt);

	var ctr = _cxt.findContractOnCard(card, "Lifecycle");
	if (ctr) {
		for (var i=0;i<routing.enter.length;i++) {
			var a = routing.enter[i];
			var m = a.action;
			if (ctr[m]) {
				var msgs;
				if (a.str)
					msgs = ctr[m](_cxt, a.str);
				// TODO: else if (a.parameter)
				// TODO: else if (a.ref) // a card ref
				else
					msgs = ctr[m](_cxt);
				_cxt.env.queueMessages(_cxt, msgs);
			}
		}
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


