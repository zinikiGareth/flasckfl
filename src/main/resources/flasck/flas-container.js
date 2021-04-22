

const Application = function(_cxt, topdiv) {
	if (typeof(topdiv) == 'string')
		this.topdiv = document.getElementById(topdiv);
	else
		this.topdiv = topdiv;
	this.cards = {};
	this.params = {};
	this.currentRoute = null; // TODO: this should not just be a list of strings, but of UNDO actions
}

Application.prototype.baseUri = function(_cxt) {
	return ''; // could be something like https://foo.com/app; set in the actual application object
}

Application.prototype.nowLoggedIn = function(_cxt) {
	this.gotoRoute(_cxt, this.routingPendingSecure.route);
}

Application.prototype.gotoRoute = function(_cxt, r) {
	var routing = this._routing();
	if (routing.secure) {
		if (!this.securityModule.requireLogin(_cxt, this, this.topdiv)) {
			this.routingPendingSecure = { routing, route: r };
			return;
		} else {
			this.routingPendingSecure = null;
		}
	}
	if (this.currentRoute == null) {
		this.currentRoute = [];
		this._createCards(_cxt, routing.cards);
		this._enterRoute(_cxt, routing.enter);
		this._enterRoute(_cxt, routing.at);
		this._readyCards(_cxt, routing.cards);

		this.cards.main._renderInto(_cxt, this.topdiv);
	}
	var path = this.parseRoute(_cxt, r);
	var cmn = this.removeCommon(_cxt, path);
	if (this.currentRoute.length > cmn) {
		_cxt.env.queueMessages(_cxt, new MoveUpEvent(this, cmn, path));
	} else {
		_cxt.env.queueMessages(_cxt, new MoveDownEvent(this, cmn == 0 ? routing : this.currentRoute[cmn-1].routes, path));
	}
}

Application.prototype.parseRoute = function(_cxt, r) {
	if (r instanceof Location || r instanceof URL) {
		r = r.href;
	}
	if (r.startsWith("/"))
		r = this.baseUri() + r;
	if (!r.endsWith("/"))
		r = r + "/";
	try {
		if (this.currentPath)
			r = new URL(r, this.currentPath).href;
		else 
			r = new URL(r, this.baseUri()).href;
	} catch (e) {}
	this.currentPath = r;
	var url = r.replace(this.baseUri(), '').replace(/^[#/]*/, '');
	var parts = url.split("/").filter(x => !!x);
	return parts;
}

Application.prototype.removeCommon = function(_cxt, path) {
	var cmn = 0;
	while (path.length > 0 && cmn < this.currentRoute.length && path[cmn] == this.currentRoute[cmn].routes.path) {
		path.shift();
		cmn++;
	}
	return cmn;
}

Application.prototype.moveUp = function(_cxt) {
	var exiting = this.currentRoute.pop();
}

Application.prototype.moveDown = function(_cxt, table, path) {
	if (table.title != null)
		this.title = table.title;
	if (path.length == 0) {
		_cxt.env.queueMessages(_cxt, new UpdateDisplay(_cxt, this));
		return;
	}

	var first = path[0];
	for (var i=0;i<table.routes.length;i++) {
		var rr = table.routes[i];
		if (rr.path == first || rr.param) {
			if (rr.secure) {
				// TODO: create nested cards that demand security
				// then process "enter" actions so that they are nested
				// then remember where we are trying to get and return
				throw new Error("cannot handle nested secure cards yet");
			}
			if (rr.param) {
				this.params[rr.param] = first;
			}
			this.currentRoute.push({ routes: rr });
			this._createCards(_cxt, rr.cards);
			this._enterRoute(_cxt, rr.enter);
			this._enterRoute(_cxt, rr.at);
			this._readyCards(_cxt, rr.cards);
	
			path.shift();
			_cxt.env.queueMessages(_cxt, new MoveDownEvent(this, rr, path));

			break;
		}
	}
}

Application.prototype._createCards = function(_cxt, cards) {
	for (var i=0;i<cards.length;i++) {
		var ci = cards[i];
		var card = this.cards[ci.name] = new ci.card(_cxt);
		var ctr = _cxt.findContractOnCard(card, "Lifecycle");
		if (ctr && ctr.init) {
			msgs = ctr.init(_cxt);
			_cxt.env.queueMessages(_cxt, msgs);
		}
	}
}

Application.prototype._readyCards = function(_cxt, cards) {
	for (var i=0;i<cards.length;i++) {
		var card = this.cards[cards[i].name];
		var ctr = _cxt.findContractOnCard(card, "Lifecycle");
		if (ctr && ctr.ready) {
			msgs = ctr.ready(_cxt);
			_cxt.env.queueMessages(_cxt, msgs);
		}
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
				} else if (a.param) {
					msgs = ctr[m](_cxt, this.params[a.param]);
				} else
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
	if (this.title) {
		var titles = document.head.getElementsByTagName("title");
		if (titles.length == 0) {
			var t = document.createElement("title");
			document.head.appendChild(t);
			titles = [t];
		}
		titles[0].innerText = this.title;
	}
	var card = this.cards.main;
	if (card == null)
		return;
	card._updateDisplay(_cxt, rt);
}

function MoveDownEvent(appl, routing, path) {
	this.appl = appl;
	this.routing = routing;
	this.path = path;
}

MoveDownEvent.prototype.dispatch = function(_cxt) {
	this.appl.moveDown(_cxt, this.routing, this.path);
}

MoveDownEvent.prototype.toString = function() {
	return "MDE[" + this.path + "]";
}

function MoveUpEvent(appl, cmn, path) {
	this.appl = appl;
	this.cmn = cmn;
	this.path = path;
}

MoveUpEvent.prototype.dispatch = function(_cxt) {
	if (this.appl.currentRoute.length > this.cmn) {
		this.appl.moveUp(_cxt);
		_cxt.env.queueMessages(_cxt, this);
	} else {
		_cxt.env.queueMessages(_cxt, new MoveDownEvent(this.appl, this.cmn == 0 ? this.appl._routing() : this.appl.currentRoute[this.cmn].routes, this.path));
	}
}

MoveUpEvent.prototype.toString = function() {
	return "MUE[" + this.cmn + "]";
}

	window.Application = Application;

const ContainerRepeater = function() {
}

ContainerRepeater.prototype.callMe = function(cx, callback) {
    return Send.eval(cx, callback, "call", []);
}


