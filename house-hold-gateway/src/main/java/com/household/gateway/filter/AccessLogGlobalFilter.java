package com.household.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Gateway 访问日志 Filter：记录每个经过网关的请求的路径、路由、状态码和耗时。
 */
@Slf4j
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "gatewayRequestStartTime";
    private static final String TRACE_ID_ATTR = "gatewayTraceId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());
        exchange.getAttributes().put(TRACE_ID_ATTR, traceId);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            if (startTime == null) return;

            long duration = System.currentTimeMillis() - startTime;
            ServerHttpRequest request = exchange.getRequest();
            String method = request.getMethod().name();
            String path = request.getURI().getPath();
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;

            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "unknown";

            log.info("[{}] {} {} → route={} status={} ({}ms)",
                    traceId, method, path, routeId, status, duration);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
