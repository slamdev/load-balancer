package com.github.slamdev.load.balancer;

public interface HostAvailabilityChecker {

    boolean isHostAvailable(String host);
}
