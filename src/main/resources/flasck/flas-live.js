const FlasckServices = function() {
}

const LiveAjaxService = function() {
}

LiveAjaxService.prototype.subscribe = function(_cxt, uri, options, handler) {
    console.log("want to subscribe to", uri);
    this.ajax(uri, this.feedback(_cxt.env, handler));
}

LiveAjaxService.prototype.ajax = function(url, handler) {
    var verb = "GET";
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = handler;
    xhr.open(verb, url, true);
    xhr.send();
}
  
LiveAjaxService.prototype.feedback = function(env, handler) {
    return function() {
        if (this.readyState == 4) {
            if (Math.floor(this.status / 100) != 2) {
                console.log("error from ajax:", this.status);
            } else {
                _cxt = env.newContext();
                // A lot of this code is duplicated here and in the mock ...
                var msg = new AjaxMessage(_cxt);
                msg.state.set('headers', []);
                msg.state.set('body', this.responseText);

                env.queueMessages(_cxt, {
                    dispatch: function(cx) {
                        var ret = handler.message(cx, msg, new LoggingIdempotentHandler());
                        if (handler._card && handler._card._updateDisplay)
                            cx.env.queueMessages(cx, [new UpdateDisplay(cx, handler._card)]);
                        return ret;
                    }
                });
                env.dispatchMessages(_cxt);
            }
        }
    }
}

FlasckServices.configure = function(env) {
    env.broker.register("Ajax", new LiveAjaxService());
}

