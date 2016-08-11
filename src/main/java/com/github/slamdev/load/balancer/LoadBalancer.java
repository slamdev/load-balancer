package com.github.slamdev.load.balancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.time.Duration.ZERO;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public class LoadBalancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancer.class);

    private final List<Server> liveServers;

    private final Set<Server> failedServers = new CopyOnWriteArraySet<>();

    private final Map<Operation, Duration> operationTime = new ConcurrentHashMap<>();

    private final HostAvailabilityChecker hostAvailabilityChecker;

    public LoadBalancer(List<String> hosts, HostAvailabilityChecker hostAvailabilityChecker, Duration hostAvailabilityCheckDuration) {
        if (hosts.isEmpty()) {
            throw new IllegalArgumentException("At least one host should be provided");
        }
        liveServers = hosts.stream().map(Server::new).collect(toCollection(CopyOnWriteArrayList::new));
        this.hostAvailabilityChecker = hostAvailabilityChecker;
        newScheduledThreadPool(1).scheduleAtFixedRate(
                this::checkFailedServers,
                hostAvailabilityCheckDuration.toNanos(),
                hostAvailabilityCheckDuration.toNanos(),
                NANOSECONDS
        );
    }

    private void checkFailedServers() {
        List<Server> revivedServers = failedServers.stream()
                .filter(server -> hostAvailabilityChecker.isHostAvailable(server.getHost()))
                .collect(toList());
        if (!revivedServers.isEmpty()) {
            LOGGER.debug("Found revived servers: {}", revivedServers);
        }
        liveServers.addAll(revivedServers);
        failedServers.removeAll(revivedServers);
    }

    public <T> T executeRequest(String uri, String method, LoadBalancedRequest<T> request) throws IOException {
        Server server = liveServers.stream().sorted().findFirst().orElse(null);
        LOGGER.debug("For [{}] uri and [{}] method trying to use [{}] server. Operations duration cache is: {}",
                uri, method, server, operationTime);
        if (server == null) {
            throw new IOException("All servers are not available.");
        }
        Operation operation = new Operation(uri, method);
        Duration duration = operationTime.computeIfAbsent(operation, k -> ZERO);
        server.setDuration(server.getDuration().plus(duration));
        try {
            Instant clock = now();
            LOGGER.info("Start operation {} {} on server {}", uri, method, server);
            T response = request.execute(server.getHost() + uri, method);
            server.setDuration(server.getDuration().minus(duration));
            duration = between(clock, now());
            LOGGER.info("End operation {} {} on server {} for {}", uri, method, server, duration);
            operationTime.put(operation, duration);
            return response;
        } catch (IOException e) {
            liveServers.remove(server);
            server.setDuration(ZERO);
            failedServers.add(server);
            LOGGER.debug("Exception occurred for ["
                    + server + "] server. Adding server to failed list\nFailed servers: "
                    + failedServers, e);
            return executeRequest(uri, method, request);
        }
    }
}
