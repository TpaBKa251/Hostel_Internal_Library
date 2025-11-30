package ru.tpu.hostel.internal.config.amqp;

import com.rabbitmq.client.BlockedListener;
import com.rabbitmq.client.Channel;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.Connection;

public class TracedConnection implements Connection {

    private final Connection delegate;

    private final Tracer tracer;

    private final OpenTelemetry openTelemetry;

    public TracedConnection(Connection delegate, Tracer tracer, OpenTelemetry openTelemetry) {
        this.delegate = delegate;
        this.tracer = tracer;
        this.openTelemetry = openTelemetry;
    }

    @Override
    public @NotNull Channel createChannel(boolean transactional) {
        Span span = tracer.spanBuilder("rabbitmq.channel.create")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("channel.transactional", transactional)
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            Channel channel = delegate.createChannel(transactional);
            span.setStatus(StatusCode.OK);
            return new TracedChannel(channel, tracer, openTelemetry);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void close() throws AmqpException {
        delegate.close();
//        Span span = tracer.spanBuilder("rabbitmq.connection.close")
//                .setSpanKind(SpanKind.CLIENT)
//                .setAttribute("messaging.system", "rabbitmq")
//                .setParent(Context.current())
//                .startSpan();
//
//        try (Scope ignored = span.makeCurrent()) {
//            delegate.close();
//            span.setStatus(StatusCode.OK);
//        } catch (Exception e) {
//            span.recordException(e);
//            span.setStatus(StatusCode.ERROR);
//            throw e;
//        } finally {
//            span.end();
//        }
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public void addBlockedListener(@NotNull BlockedListener listener) {
        delegate.addBlockedListener(listener);
    }

    @Override
    public boolean removeBlockedListener(@NotNull BlockedListener listener) {
        return delegate.removeBlockedListener(listener);
    }

    @Override
    public com.rabbitmq.client.Connection getDelegate() {
        return delegate.getDelegate();
    }

    @Override
    public void closeThreadChannel() {
        delegate.closeThreadChannel();
    }

}
