const groundUri = function(uri) {
	try {
        if (uri instanceof FLURI) {
            uri = uri.resolve(window.location);
        } else if (typeof(uri) === 'string') {
            uri = new URL(uri, window.location);
        } else if (!(uri instanceof URL)) {
            _cxt.log("not a valid uri", uri);
            return;
        }
    } catch (e) {
		_cxt.log("error in resolving uri from", uri, "inside", window.location);
		return;
	}
    return uri;
}

const FlasckServices = function() {
}

const LiveAjaxService = function() {
}

LiveAjaxService.prototype.subscribe = function(_cxt, uri, options, handler) {
    uri = groundUri(uri);
    if (uri) {
        console.log("want to subscribe to", uri);
        this.ajax(_cxt, uri, this.feedback(_cxt.env, uri, options, handler));
    }
}

LiveAjaxService.prototype.ajax = function(_cxt, uri, handler) {
    var verb = "GET";
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = handler;
    xhr.open(verb, uri, true);
    xhr.send();
}
  
LiveAjaxService.prototype.feedback = function(env, uri, options, handler) {
    var self = this;
    var fb = function() {
        if (this.readyState == 4) {
            _cxt = env.newContext();
            if (Math.floor(this.status / 100) != 2) {
                console.log("error from ajax:", this.status);
            } else {
                // A lot of this code is duplicated here and in the mock ...
                var msg = new AjaxMessage(_cxt);
                msg.state.set('headers', []);
                msg.state.set('body', this.responseText);

                env.queueMessages(_cxt, Send.eval(_cxt, handler, "message", [msg], null));
                env.dispatchMessages(_cxt);
            }

            var ms = options.state.get('subscribeRepeat').asJs();
            setTimeout(() => {
                self.ajax(_cxt, uri, fb);
            }, ms);
        }
    };
    return fb;
}

const LiveNavigationService = function() {
}

LiveNavigationService.prototype.redirect = function(_cxt, uri) {
    uri = groundUri(uri);
    if (uri) {
        _cxt.log("redirecting to", uri);    
        window.location = uri;
    }
}

FlasckServices.configure = function(env) {
    env.broker.register("Ajax", new LiveAjaxService());
    env.broker.register("Navigation", new LiveNavigationService());
}

