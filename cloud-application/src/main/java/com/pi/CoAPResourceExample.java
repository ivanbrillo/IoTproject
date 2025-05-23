package com.pi;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * A simple observable CoAP resource example.
 */
class CoAPResourceExample extends CoapResource {

    /**
     * Constructor for the CoAP resource.
     *
     * @param name the name of the resource
     */
    CoAPResourceExample(final String name) {
        super(name);
        setObservable(true);
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
        exchange.respond("hello world");
    }

    @Override
    public void handlePOST(final CoapExchange exchange) {
        // your logic here
        exchange.respond(ResponseCode.CREATED);
    }
}
