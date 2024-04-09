// src/main/javascript/live/jsenv.js
import { CommonEnv } from "/js/flasjs.js";
import { SimpleBroker } from "/js/ziwsh.js";
var JSEnv = function(broker) {
  if (broker == null)
    broker = new SimpleBroker(console, this, {});
  var logger = {
    log: console.log,
    debugmsg: console.log
  };
  CommonEnv.call(this, logger, broker);
  if (typeof FlasckServices !== "undefined") {
    FlasckServices.configure(this);
  }
};
JSEnv.prototype = new CommonEnv();
JSEnv.prototype.constructor = JSEnv;
JSEnv.prototype.addHistory = function(state, title, url) {
  history.pushState(state, title, url);
};

// src/main/javascript/live/services.js
import { FLURI } from "/js/flasjs.js";
var groundUri = function(uri) {
  try {
    if (uri instanceof FLURI) {
      uri = uri.resolve(window.location);
    } else if (typeof uri === "string") {
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
};
var FlasckServices2 = function() {
};
var LiveAjaxService = function() {
};
LiveAjaxService.prototype.subscribe = function(_cxt2, uri, options, handler) {
  uri = groundUri(uri);
  if (uri) {
    console.log("want to subscribe to", uri);
    this.ajax(_cxt2, uri, this.feedback(_cxt2.env, uri, options, handler));
  }
};
LiveAjaxService.prototype.ajax = function(_cxt2, uri, handler) {
  var verb = "GET";
  var xhr = new XMLHttpRequest();
  xhr.onreadystatechange = handler;
  xhr.open(verb, uri, true);
  xhr.send();
};
LiveAjaxService.prototype.feedback = function(env2, uri, options, handler) {
  var self = this;
  var fb = function() {
    if (this.readyState == 4) {
      _cxt = env2.newContext();
      if (Math.floor(this.status / 100) != 2) {
        console.log("error from ajax:", this.status);
      } else {
        var msg = new AjaxMessage(_cxt);
        msg.state.set("headers", []);
        msg.state.set("body", this.responseText);
        env2.queueMessages(_cxt, Send.eval(_cxt, handler, "message", [msg], null));
        env2.dispatchMessages(_cxt);
      }
      var ms = options.state.get("subscribeRepeat").asJs();
      setTimeout(() => {
        self.ajax(_cxt, uri, fb);
      }, ms);
    }
  };
  return fb;
};
var LiveNavigationService = function() {
};
LiveNavigationService.prototype.redirect = function(_cxt2, uri) {
  uri = groundUri(uri);
  if (uri) {
    _cxt2.log("redirecting to", uri);
    if (uri.toString().startsWith(window.appl.baseUri())) {
      window.history.pushState({}, "", uri);
      window.appl.gotoRoute(_cxt2, uri);
    } else {
      window.location = uri;
    }
  }
};
window.addEventListener("popstate", function(ev) {
  console.log("location: " + document.location + ", state: " + JSON.stringify(ev.state));
  ev.preventDefault();
  window.appl.gotoRoute(env.newContext(), document.location);
});
FlasckServices2.configure = function(env2) {
  env2.broker.register("Ajax", new LiveAjaxService());
  env2.broker.register("Navigation", new LiveNavigationService());
};
export {
  FlasckServices2 as FlasckServices,
  JSEnv
};
