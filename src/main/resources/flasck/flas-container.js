

const Application = function(_cxt, topdiv) {
	this.topdiv = topdiv;
	this.cards = {};
	this.currentRoute = null; // TODO: this should not just be a list of strings, but of UNDO actions
}

Application.prototype.baseUri = function(_cxt) {
	return ''; // could be something like https://foo.com/app; set in the actual application object
}

Application.prototype.gotoRoute = function(_cxt, r) {
	var routing = this._routing();
	console.log("routing ", routing);
	if (this.currentRoute == null) {
		this.currentRoute = [];
		this._createCards(_cxt, [{ name: 'main', card: this.mainCard }]);
		this._enterRoute(_cxt, routing.enter);
		this.cards.main._renderInto(_cxt, this.topdiv);
	}
	var path = this.parseRoute(_cxt, r);
	console.log("have", this.currentRoute, "want", path);

	// remove and ignore any common elements
	// move "up" if current has anything left
	// move "down" the new path
	this.moveDown(_cxt, routing, path);
}

Application.prototype.parseRoute = function(_cxt, r) {
	if (r instanceof Location) {
		r = r.href;
	}
	try {
		if (this.currentPath)
			r = new URL(r, this.currentPath).href;
		else 
			r = new URL(r, this.baseUri()).href;
	} catch (e) {}
	this.currentPath = r;
	var url = r.replace(this.baseUri(), '').replace(/^#*/, '').replace(/^\/*/, '');
	var parts = url.split("/").filter(x => !!x);
	return parts;
}

Application.prototype.moveDown = function(_cxt, table, path) {
	if (path.length == 0)
		return;

	var first = path[0];
	for (var i=0;i<table.routes.length;i++) {
		var rr = table.routes[i];
		if (rr.path == first) {
			this.currentRoute.push({ action: rr });
			this._createCards(_cxt, rr.cards);
			this._enterRoute(_cxt, rr.enter);
			return; // don't consider any other matches or fall through to parameter logic
		}
	}
	// TODO: maybe it is a parameter
}

Application.prototype._createCards = function(_cxt, cards) {
	for (var i=0;i<cards.length;i++) {
		var ci = cards[i];
		this.cards[ci.name] = new ci.card(_cxt);
	}
}

Application.prototype._enterRoute = function(_cxt, enter) {
	for (var i=0;i<enter.length;i++) {
		var a = enter[i];
		var card = this.cards[a.card];
		var ctr = _cxt.findContractOnCard(card, "Lifecycle");
		if (ctr) {
			var m = a.action;
			if (ctr[m]) {
				var msgs;
				if (a.str) {
					msgs = ctr[m](_cxt, a.str);
				} else if (a.ref) {
					msgs = ctr[m](_cxt, this.cards[a.ref]);
				}
				// TODO: else if (a.parameter)
				else
					msgs = ctr[m](_cxt);
				_cxt.env.queueMessages(_cxt, msgs);
			}
		}
	}
}

Application.prototype._currentRenderTree = function() {
	var card = this.cards.main;
	if (card == null)
		return null;
	return card._currentRenderTree();
}

Application.prototype._updateDisplay = function(_cxt, rt) {
	var card = this.cards.main;
	if (card == null)
		return;
	card._updateDisplay(_cxt, rt);
}

	window.Application = Application;

const ContainerRepeater = function() {
}

ContainerRepeater.prototype.callMe = function(cx, callback) {
    return Send.eval(cx, callback, "call", []);
}


