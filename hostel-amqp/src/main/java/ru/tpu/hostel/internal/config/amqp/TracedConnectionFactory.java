package ru.tpu.hostel.internal.config.amqp;

import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;

@RequiredArgsConstructor
public class TracedConnectionFactory implements ConnectionFactory, InitializingBean, ShutdownListener, SmartLifecycle {

    private final CachingConnectionFactory delegate;

    private final OpenTelemetry openTelemetry;

    @Override
    public @NotNull Connection createConnection() throws AmqpException {
        Tracer tracer = openTelemetry.getTracer("ru.tpu.hostel.internal.amqp");
        Span span = tracer.spanBuilder("rabbitmq.connection.create")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("server.host", delegate.getHost())
                .setAttribute("server.port", delegate.getPort())
                .setAttribute("server.virtualHost", delegate.getVirtualHost())
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            Connection connection = delegate.createConnection();
            span.setStatus(StatusCode.OK);
            return new TracedConnection(connection, tracer, openTelemetry);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public String getHost() {
        return delegate.getHost();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public @NotNull String getVirtualHost() {
        return delegate.getVirtualHost();
    }

    @Override
    public @NotNull String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public void addConnectionListener(@NotNull ConnectionListener listener) {
        delegate.addConnectionListener(listener);
    }

    @Override
    public boolean removeConnectionListener(@NotNull ConnectionListener listener) {
        return delegate.removeConnectionListener(listener);
    }

    @Override
    public void clearConnectionListeners() {
        delegate.clearConnectionListeners();
    }

    @Override
    public ConnectionFactory getPublisherConnectionFactory() {
        return delegate.getPublisherConnectionFactory();
    }

    @Override
    public boolean isSimplePublisherConfirms() {
        return delegate.isSimplePublisherConfirms();
    }

    @Override
    public boolean isPublisherConfirms() {
        return delegate.isPublisherConfirms();
    }

    @Override
    public boolean isPublisherReturns() {
        return delegate.isPublisherReturns();
    }

    @Override
    public void resetConnection() {
        Span span = openTelemetry.getTracer("ru.tpu.hostel.internal.amqp").spanBuilder("rabbitmq.connection.reset")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.resetConnection();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void shutdownCompleted(ShutdownSignalException e) {
        delegate.shutdownCompleted(e);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        delegate.afterPropertiesSet();
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public boolean isAutoStartup() {
        return delegate.isAutoStartup();
    }

    @Override
    public void stop(@NotNull Runnable callback) {
        delegate.stop(callback);
    }

    @Override
    public int getPhase() {
        return delegate.getPhase();
    }
}