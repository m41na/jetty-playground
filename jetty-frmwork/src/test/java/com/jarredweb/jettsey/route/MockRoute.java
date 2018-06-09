package com.jarredweb.jettsey.route;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpInput;
import org.eclipse.jetty.server.Request;

public class MockRoute extends Request{
    
    private final AppRoute request;
    
    public MockRoute(HttpChannel channel, HttpInput input, AppRoute route) {
        super(channel, input);
        this.request = route;
    }

    @Override
    public String getRequestURI() {
        return request.path;
    }

    @Override
    public String getMethod() {
        return request.method;
    }

    @Override
    public String getHeader(String name) {
        if("Accepts".equals(name)){
            return request.accepts;
        }
        if("Content-Type".equals(name)){
            return request.contentType;
        }
        return request.headers.get(name);
    }    
}
