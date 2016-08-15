# Load balancer [![Build Status](https://travis-ci.org/slamdev/load-balancer.svg?branch=master)](https://travis-ci.org/slamdev/load-balancer) [![Download](https://api.bintray.com/packages/slamdev/maven/load-balancer/images/download.svg)](https://bintray.com/slamdev/maven/load-balancer/_latestVersion)
Library balancing requests between hosts. Weight based algorithm is used to determinate host to which request should be forwarded. Weight is calculated according to the duration of the previously executed request with the same URI and method. So it is useful only for applications sending bunch of requests of the same type.
## Library usage
1. Add it to the project dependencies. Latest version can be found at Bintray (https://bintray.com/slamdev/maven/load-balancer) and added via Maven or Gradle
2. Create instance of the ```LoadBalancer``` class with list of hosts (can be added later via ```LoadBalancer#addHosts```), instance of ```HostAvailabilityChecker``` (see below) and availability check period
3. Call ```LoadBalancer#executeRequest``` method with ```LoadBalancedRequest``` (see below) when the execution is required. You should pass absolute ```String uri``` without host:port to the method, eg. ```loadBalancer.executeRequest("/api/user/1", "GET", executor);```
### ```HostAvailabilityChecker```
If load balancer receive ```IOException``` during request execution it places the failed host to the black list and will not execute any requests on it. ```HostAvailabilityChecker#isHostAvailable``` will be called periodically (```hostAvailabilityCheckDuration``` param) for such hosts. If method returns true, the host will be available for future requests. In implementation you can just call ping like action for the passed host and return true if all is good.
### ```LoadBalancedRequest```
You need to implement the sending request mechanism here. The ```String uri``` param contains fully qualified URL with the most free server.
## Existing integrations
There is a Spring Boot Starter project with the library auto configuration at: https://github.com/slamdev/load-balancer-spring-boot
Also you can find example of library used at: https://github.com/slamdev/catalog/tree/master/client
