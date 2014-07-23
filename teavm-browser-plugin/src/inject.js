__TeaVM_remote__ = (function() {
    var proxyClasses = {};
    var proxies = {};
    var sentMessage = null;
    var receivedMessage = null;

    function callMethod() {
        var instance = arguments[0];
        var method = arguments[1];
        var params = [];
        for (var i = 2; i < arguments.length; ++i) {
            params.push(arguments[i]);
        }
        if (!instance[method]) {
            throw "Method not found: " + method;
        }
        return instance[method].apply(instance, params);
    }
    function getProperty(instance, property) {
        return instance[property];
    }
    function setProperty(instance, property, value) {
        instance[property] = value;
    }
    function declareProxyClass(classId, description) {
        description = JSON.parse(description);
        var proxyClass = function() {
        };
        if (!description.functor) {
            proxyClass.prototype = new Object();
        } else {
            var method = description.methods[0];
            proxyClass.prototype = new Function("return this." + method + ".apply(this, arguments);");
        }

        for (var property in description.properties) {
            var propertyData = description.properties[property];
            var propertyDesc = {};
            if (propertyData.readable) {
                propertyDesc.get = (function(propertyName) {
                    return function() {
                        return sendMessage({
                            type : "get-property",
                            propertyName : propertyName,
                            instance : this
                        });
                    };
                })(property);
            }
            if (propertyData.writable) {
                propertyDesc.writable = true;
                propertyDesc.set = (function(propertyName) {
                    return function(value) {
                        sendMessage({
                            type : "set-property",
                            propertyName : propertyName,
                            instance : this,
                            value : value
                        });
                    };
                })(property);
            }
            Object.defineProperty(proxyClass.prototype, property, propertyDesc);
        }
        for (var i = 0; i < description.methods.length; ++i) {
            var method = description.methods[i];
            proxyClass.prototype[method] = (function(methodName) {
                return function() {
                    var params = [];
                    for (var i = 0; i < arguments.length; ++i) {
                        params.push(arguments[i]);
                    }
                    return sendMessage({
                        type : "invoke-method",
                        methodName : methodName,
                        instance : this,
                        args : params
                    });
                };
            })(method);
        }

        console.log("%O", proxyClass);
        proxyClasses[classId] = proxyClass;
    }
    function createProxy(classId, proxyId) {
        var object = new proxyClasses[classId]();
        proxies[proxyId] = object;
        return object;
    }
    function sendMessage(message) {
        sentMessage = message;
        while (sentMessage != null) {
            debugger;
        }
        return receivedMessage;
    }
    function receiveMessage() {
        var result = sentMessage;
        sentMessage = null;
        return result;
    }
    function sendResponse(response) {
        receivedMessage = response;
    }
    return {
        declareProxyClass : declareProxyClass,
        createProxy : createProxy,
        receiveMessage : receiveMessage,
        callMethod : callMethod,
        getProperty : getProperty,
        setProperty : setProperty,
        sendResponse : sendResponse
    };
})();
