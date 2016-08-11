package com.github.slamdev.load.balancer;

import java.io.IOException;

public interface LoadBalancedRequest<T> {

    T execute(String uri, String method) throws IOException;
}
