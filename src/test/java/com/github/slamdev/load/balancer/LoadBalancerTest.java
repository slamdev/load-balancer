package com.github.slamdev.load.balancer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.concurrent.*;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofNanos;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoadBalancerTest {

    private static final String HOST_1 = "http://fake-host1.com";

    private static final String HOST_2 = "http://fake-host2.com";

    @Rule
    public ExpectedException expectedException = none();

    private LoadBalancer balancer;

    private LoadBalancedRequest<String> request;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        balancer = new LoadBalancer(asList(HOST_1, HOST_2), (host) -> true, ofMinutes(1));
        request = mock(LoadBalancedRequest.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_exception_when_no_hosts_provided() {
        new LoadBalancer(emptyList(), (host) -> true, ofMinutes(1));
    }

    @Test
    public void should_execute_provided_request() throws IOException {
        when(request.execute(anyString(), anyString())).thenReturn("some-response");
        String response = balancer.executeRequest("", "", request);
        assertThat(response, is("some-response"));
    }

    @Test
    public void should_append_host_to_request_uri() throws IOException {
        when(request.execute(eq(HOST_1 + "/uri"), anyString())).thenReturn("some-response");
        String response = balancer.executeRequest("/uri", "", request);
        assertThat(response, is("some-response"));
    }

    @Test
    public void should_use_next_host_when_request_throws_io() throws IOException {
        when(request.execute(eq(HOST_1 + "/uri"), anyString())).thenThrow(new IOException());
        when(request.execute(eq(HOST_2 + "/uri"), anyString())).thenReturn("some-response");
        String response = balancer.executeRequest("/uri", "", request);
        assertThat(response, is("some-response"));
    }

    @Test(expected = IOException.class)
    public void should_throw_io_when_request_to_all_hosts_throws_io() throws IOException {
        when(request.execute(eq(HOST_1 + "/uri"), anyString())).thenThrow(new IOException());
        when(request.execute(eq(HOST_2 + "/uri"), anyString())).thenThrow(new IOException());
        balancer.executeRequest("/uri", "", request);
    }

    @Test
    public void should_use_failed_server_if_it_becomes_available() throws IOException, InterruptedException {
        balancer = new LoadBalancer(singletonList(HOST_1), (host) -> true, ofNanos(1));
        when(request.execute(eq(HOST_1 + "/uri"), anyString())).thenThrow(new IOException());
        try {
            balancer.executeRequest("/uri", "", request);
        } catch (IOException ignore) {
        }
        when(request.execute(eq(HOST_1 + "/uri"), anyString())).thenReturn("some-response");
        MILLISECONDS.sleep(120);
        String response = balancer.executeRequest("/uri", "", request);
        assertThat(response, is("some-response"));
    }

    @Test
    public void should_use_host_that_has_less_sum_of_operations_duration() throws InterruptedException, ExecutionException, IOException {
        // Execute both long and short tasks to determinate their duration
        LoadBalancedRequest<String> request1 = (uri, method) -> {
            safeSleep(NANOSECONDS, 1);
            return "";
        };
        LoadBalancedRequest<String> request2 = (uri, method) -> {
            safeSleep(MILLISECONDS, 120);
            return "";
        };
        balancer.executeRequest("/short", "", request1);
        balancer.executeRequest("/long", "", request2);
        // Execute one long and 3 short tasks; Long should be execute on one server and 3 shorts on another
        ExecutorService executor = Executors.newFixedThreadPool(4);
        LoadBalancedRequest<String> request3 = (uri, method) -> {
            safeSleep(MILLISECONDS, 120);
            return (HOST_1 + "/long").equals(uri) ? "long" : "";
        };
        LoadBalancedRequest<String> request4 = (uri, method) -> {
            safeSleep(NANOSECONDS, 1);
            return (HOST_2 + "/short").equals(uri) ? "short" : "";
        };
        Future<String> response1 = executor.submit(() -> balancer.executeRequest("/long", "", request3));
        safeSleep(MILLISECONDS, 10);
        Future<String> response2 = executor.submit(() -> balancer.executeRequest("/short", "", request4));
        Future<String> response3 = executor.submit(() -> balancer.executeRequest("/short", "", request4));
        Future<String> response4 = executor.submit(() -> balancer.executeRequest("/short", "", request4));
        assertThat(response1.get(), is("long"));
        assertThat(response2.get(), is("short"));
        assertThat(response3.get(), is("short"));
        assertThat(response4.get(), is("short"));
    }

    private void safeSleep(TimeUnit unit, long value) {
        try {
            unit.sleep(value);
        } catch (InterruptedException ignore) {
        }
    }
}
