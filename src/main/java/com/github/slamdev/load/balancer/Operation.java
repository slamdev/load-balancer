package com.github.slamdev.load.balancer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
class Operation {
    private String uri;
    private String method;
}
