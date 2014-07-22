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
        proxyClasses[classId] = description;
    }
    function createProxy(classId, proxyId) {
        var object = null;
        object.__TeaVM_proxyId__ = proxyId;
        var description = proxyClasses[classId];
        for (var property in description.properties) {
            var propertyData = description.properties[property];
            var propertyDesc = {};
            if (propertyData.readable) {
                propertyDesc.get = (function(propertyName) {
                    return function() {
                        return sendMessage({ type : "get-property", propertyName : propertyName,
                                instance : object });
                    };
                })(property);
            }
            if (propertyData.writable) {
                propertyDesc.writable = true;
                propertyDesc.set = (function(propertyName) {
                    return function(value) {
                        sendMessage({ type : "set-property", propertyName : propertyName,
                                instance : object, value : value });
                    };
                })(property);
            }
            Object.defineProperty(object, property, propertyDesc);
        }
        for (var i = 0; i < description.methods.length; ++i) {
            var method = description.methods[i];
            object.method = (function(methodName) {
                return function() {
                    var params = [];
                    for (var i = 0; i < arguments.length; ++i) {
                        params.push(arguments[i]);
                    }
                    return sendMessage({ type : "invoke-method", methodName : methodName,
                                instance : object, params : params });
                };
            })(method);
        }
        proxies[propxyId] = object;
        return object;
    }
    function sendMessage(message) {
        sentMessage = message;
        debugger;
        sentMessage = null;
        return receivedMessage;
    }
    return { declareProxyClass : declareProxyClass, createProxy : createProxy,
            callMethod : callMethod, getProperty : getProperty, setProperty : setProperty };
})();
