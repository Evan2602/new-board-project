package com.dong.board.tracing;

import brave.Tracing;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.sampler.Sampler;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

@Configuration
public class TracingConfig {

    @Value("${management.zipkin.tracing.endpoint:http://localhost:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Value("${management.tracing.sampling.probability:1.0}")
    private float samplingProbability;

    @Bean
    @ConditionalOnMissingBean
    public Tracing braveTracing() {
        URLConnectionSender sender = URLConnectionSender.create(zipkinEndpoint);
        AsyncZipkinSpanHandler spanHandler = AsyncZipkinSpanHandler.create(sender);

        return Tracing.newBuilder()
                .localServiceName("board")
                .sampler(Sampler.create(samplingProbability))
                .currentTraceContext(ThreadLocalCurrentTraceContext.newBuilder().build())
                .addSpanHandler(spanHandler)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracer tracer(Tracing braveTracing) {
        brave.propagation.CurrentTraceContext braveCtx = braveTracing.currentTraceContext();
        return new BraveTracer(
                braveTracing.tracer(),
                new BraveCurrentTraceContext(braveCtx),
                new BraveBaggageManager()
        );
    }
}
