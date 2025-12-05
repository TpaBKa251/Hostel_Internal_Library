package ru.tpu.hostel.internal.config.amqp.tracing;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class TracedConsumer implements Consumer {

    private final Consumer delegate;

    private final Tracer tracer;

    private final String queue;

    private final OpenTelemetry openTelemetry;

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

        Context context = extractTraceContext(properties);

        Span span = tracer.spanBuilder("rabbitmq.consume")
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(context)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.destination.name", queue)
                .setAttribute("messaging.rabbitmq.destination.routing_key", envelope.getRoutingKey())
                .setAttribute("messaging.message.payload_size_bytes", body != null ? body.length : 0)
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.handleDelivery(consumerTag, envelope, properties, body);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    private Context extractTraceContext(AMQP.BasicProperties properties) {
        Map<String, Object> headers = properties.getHeaders();
        if (headers == null) {
            return Context.current();
        }

        TextMapGetter<Map<String, Object>> getter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Map<String, Object> carrier) {
                return carrier.keySet();
            }

            @Override
            public String get(Map<String, Object> carrier, String key) {
                Object value = null;
                if (carrier != null) {
                    value = carrier.get(key);
                }
                return value != null ? value.toString() : null;
            }
        };

        return openTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), headers, getter);
    }

    @Override
    public void handleConsumeOk(String consumerTag) {
        delegate.handleConsumeOk(consumerTag);
    }

    @Override
    public void handleCancelOk(String consumerTag) {
        delegate.handleCancelOk(consumerTag);
    }

    @Override
    public void handleCancel(String consumerTag) throws IOException {
        delegate.handleCancel(consumerTag);
    }

    @Override
    public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {
        Span span = tracer.spanBuilder("rabbitmq.consumer.shutdown")
                .setSpanKind(SpanKind.CONSUMER)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.rabbitmq.consumer_tag", consumerTag)
                .setAttribute("shutdown.reason", sig.getMessage())
                .setAttribute("shutdown.hard", sig.isHardError())
                .setAttribute("shutdown.initiated_by_application", sig.isInitiatedByApplication())
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            span.recordException(sig);
            span.setStatus(StatusCode.ERROR, "Consumer shutdown");

            delegate.handleShutdownSignal(consumerTag, sig);
        } finally {
            span.end();
        }
    }

    @Override
    public void handleRecoverOk(String consumerTag) {
        delegate.handleRecoverOk(consumerTag);
    }
}
