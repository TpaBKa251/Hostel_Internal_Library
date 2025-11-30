package ru.tpu.hostel.internal.config.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.ConfirmCallback;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ConsumerShutdownSignalCallback;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Method;
import com.rabbitmq.client.ReturnCallback;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
public class TracedChannel implements Channel {

    private final Channel delegate;

    private final Tracer tracer;

    private final OpenTelemetry openTelemetry;

    @Override
    public void basicQos(int i, int i1, boolean b) throws IOException {
        delegate.basicQos(i, i1, b);
    }

    @Override
    public void basicQos(int i, boolean b) throws IOException {
        delegate.basicQos(i, b);
    }

    @Override
    public void basicQos(int i) throws IOException {
        delegate.basicQos(i);
    }

    @Override
    public void basicPublish(String s, String s1, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        Span span = createPublishSpan(s, s1, bytes);

        AMQP.BasicProperties tracedProps = injectTraceContext(basicProperties, span);

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicPublish(s, s1, tracedProps, bytes);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void basicPublish(String s, String s1, boolean b, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        Span span = createPublishSpan(s, s1, bytes);

        AMQP.BasicProperties tracedProps = injectTraceContext(basicProperties, span);

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicPublish(s, s1, b, tracedProps, bytes);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void basicPublish(String exchange, String routingKey,
                             boolean mandatory, boolean immediate,
                             AMQP.BasicProperties props, byte[] body) throws IOException {
        Span span = createPublishSpan(exchange, routingKey, body);

        AMQP.BasicProperties tracedProps = injectTraceContext(props, span);

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicPublish(exchange, routingKey, mandatory, immediate, tracedProps, body);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    private Span createPublishSpan(String exchange, String routingKey, byte[] body) {
        return tracer.spanBuilder("rabbitmq.publish")
                .setSpanKind(SpanKind.PRODUCER)
                .setParent(Context.current())
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.destination.name", exchange)
                .setAttribute("messaging.rabbitmq.destination.routing_key", routingKey)
                .setAttribute("messaging.message.payload_size_bytes", body != null ? body.length : 0)
                .startSpan();
    }

    private void handleException(Exception e, Span span) {
        span.recordException(e);
        span.setStatus(StatusCode.ERROR);
    }

    private AMQP.BasicProperties injectTraceContext(AMQP.BasicProperties props, Span span) {
        Map<String, Object> headers = props.getHeaders() != null
                ? new HashMap<>(props.getHeaders())
                : new HashMap<>();

        TextMapSetter<Map<String, Object>> setter = (carrier, key, value) -> {
            if (carrier != null) {
                carrier.put(key, value);
            }
        };

        Context context = Context.current().with(span);
        openTelemetry.getPropagators().getTextMapPropagator()
                .inject(context, headers, setter);

        return props.builder().headers(headers).build();
    }

    @Override
    public GetResponse basicGet(String s, boolean b) throws IOException {
        return delegate.basicGet(s, b);
    }

    @Override
    public void basicAck(long l, boolean b) throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.ack")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.rabbitmq.delivery_tag", l)
                .setAttribute("messaging.rabbitmq.ack.multiple", b)
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicAck(l, b);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void basicNack(long l, boolean b, boolean b1) throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.nack")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.rabbitmq.delivery_tag", l)
                .setAttribute("messaging.rabbitmq.nack.multiple", b)
                .setAttribute("messaging.rabbitmq.nack.requeue", b1)
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicNack(l, b, b1);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void basicReject(long l, boolean b) throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.reject")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.rabbitmq.delivery_tag", l)
                .setAttribute("messaging.rabbitmq.reject.requeue", b)
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicReject(l, b);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public String basicConsume(String s, Consumer consumer) throws IOException {
        TracedConsumer tracedConsumer = new TracedConsumer(consumer, tracer, s, openTelemetry);

        return delegate.basicConsume(s, tracedConsumer);
    }

    @Override
    public String basicConsume(String s, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return delegate.basicConsume(s, deliverCallback, cancelCallback);
    }

    @Override
    public String basicConsume(String s, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, deliverCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, deliverCallback, cancelCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, Consumer consumer) throws IOException {
        TracedConsumer tracedConsumer = new TracedConsumer(consumer, tracer, s, openTelemetry);

        return delegate.basicConsume(s, b, tracedConsumer);
    }

    @Override
    public String basicConsume(String s, boolean b, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return delegate.basicConsume(s, b, deliverCallback, cancelCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, deliverCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, deliverCallback, cancelCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, Map<String, Object> map, Consumer consumer) throws IOException {
        TracedConsumer tracedConsumer = new TracedConsumer(consumer, tracer, s, openTelemetry);

        return delegate.basicConsume(s, b, map, tracedConsumer);
    }

    @Override
    public String basicConsume(String s, boolean b, Map<String, Object> map, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return delegate.basicConsume(s, b, map, deliverCallback, cancelCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, Map<String, Object> map, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, map, deliverCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, Map<String, Object> map, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, map, deliverCallback, cancelCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, Consumer consumer) throws IOException {
        TracedConsumer tracedConsumer = new TracedConsumer(consumer, tracer, s, openTelemetry);

        return delegate.basicConsume(s, b, s1, tracedConsumer);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return delegate.basicConsume(s, b, s1, deliverCallback, cancelCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, s1, deliverCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, s1, deliverCallback, cancelCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String queue, boolean autoAck,
                               String consumerTag, boolean noLocal,
                               boolean exclusive, Map<String, Object> arguments,
                               Consumer callback) throws IOException {

        // Обертываем callback для трассировки получения
        TracedConsumer tracedConsumer = new TracedConsumer(callback, tracer, queue, openTelemetry);

        return delegate.basicConsume(queue, autoAck, consumerTag, noLocal,
                exclusive, arguments, tracedConsumer);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, boolean b1, boolean b2, Map<String, Object> map, DeliverCallback deliverCallback, CancelCallback cancelCallback) throws IOException {
        return delegate.basicConsume(s, b, s1, b1, b2, map, deliverCallback, cancelCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, boolean b1, boolean b2, Map<String, Object> map, DeliverCallback deliverCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, s1, b1, b2, map, deliverCallback, consumerShutdownSignalCallback);
    }

    @Override
    public String basicConsume(String s, boolean b, String s1, boolean b1, boolean b2, Map<String, Object> map, DeliverCallback deliverCallback, CancelCallback cancelCallback, ConsumerShutdownSignalCallback consumerShutdownSignalCallback) throws IOException {
        return delegate.basicConsume(s, b, s1, b1, b2, map, deliverCallback, cancelCallback, consumerShutdownSignalCallback);
    }

    @Override
    public void basicCancel(String s) throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.consumer.cancel")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setAttribute("messaging.rabbitmq.consumer_tag", s)
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.basicCancel(s);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public AMQP.Basic.RecoverOk basicRecover() throws IOException {
        return delegate.basicRecover();
    }

    @Override
    public AMQP.Basic.RecoverOk basicRecover(boolean b) throws IOException {
        return delegate.basicRecover(b);
    }

    @Override
    public AMQP.Tx.SelectOk txSelect() throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.tx.select")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            AMQP.Tx.SelectOk res = delegate.txSelect();
            span.setStatus(StatusCode.OK);
            return res;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public AMQP.Tx.CommitOk txCommit() throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.tx.commit")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            AMQP.Tx.CommitOk res = delegate.txCommit();
            span.setStatus(StatusCode.OK);
            return res;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public AMQP.Tx.RollbackOk txRollback() throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.tx.rollback")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            AMQP.Tx.RollbackOk res = delegate.txRollback();
            span.setStatus(StatusCode.OK);
            return res;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public AMQP.Confirm.SelectOk confirmSelect() throws IOException {
        return delegate.confirmSelect();
    }

    @Override
    public void asyncRpc(Method method) throws IOException {
        delegate.asyncRpc(method);
    }

    @Override
    public Command rpc(Method method) throws IOException {
        return delegate.rpc(method);
    }

    @Override
    public CompletableFuture<Command> asyncCompletableRpc(Method method) throws IOException {
        return delegate.asyncCompletableRpc(method);
    }


    @Override
    public long messageCount(String s) throws IOException {
        return delegate.messageCount(s);
    }

    @Override
    public long consumerCount(String s) throws IOException {
        return delegate.consumerCount(s);
    }

    @Override
    public int getChannelNumber() {
        return delegate.getChannelNumber();
    }

    @Override
    public Connection getConnection() {
        return delegate.getConnection();
    }

    @Override
    public void close() throws IOException, TimeoutException {
        Span span = tracer.spanBuilder("rabbitmq.channel.close")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.close();
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void close(int i, String s) throws IOException, TimeoutException {
        Span span = tracer.spanBuilder("rabbitmq.channel.close")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.close(i, s);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            handleException(e, span);
            throw e;
        } finally {
            span.end();
        }
    }

    @Override
    public void abort() throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.channel.abort")
                .setSpanKind(SpanKind.CLIENT)
                .setParent(Context.current())
                .setAttribute("messaging.system", "rabbitmq")
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.abort();
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
    public void abort(int i, String s) throws IOException {
        Span span = tracer.spanBuilder("rabbitmq.channel.abort")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("messaging.system", "rabbitmq")
                .setParent(Context.current())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            delegate.abort(i, s);
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
    public void addReturnListener(ReturnListener returnListener) {
        delegate.addReturnListener(returnListener);
    }

    @Override
    public ReturnListener addReturnListener(ReturnCallback returnCallback) {
        return delegate.addReturnListener(returnCallback);
    }

    @Override
    public boolean removeReturnListener(ReturnListener returnListener) {
        return delegate.removeReturnListener(returnListener);
    }

    @Override
    public void clearReturnListeners() {
        delegate.clearReturnListeners();
    }

    @Override
    public void addConfirmListener(ConfirmListener confirmListener) {
        delegate.addConfirmListener(confirmListener);
    }

    @Override
    public ConfirmListener addConfirmListener(ConfirmCallback confirmCallback, ConfirmCallback confirmCallback1) {
        return delegate.addConfirmListener(confirmCallback, confirmCallback1);
    }

    @Override
    public boolean removeConfirmListener(ConfirmListener confirmListener) {
        return delegate.removeConfirmListener(confirmListener);
    }

    @Override
    public void clearConfirmListeners() {
        delegate.clearConfirmListeners();
    }

    @Override
    public Consumer getDefaultConsumer() {
        return delegate.getDefaultConsumer();
    }

    @Override
    public void setDefaultConsumer(Consumer consumer) {
        delegate.setDefaultConsumer(consumer);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s1) throws IOException {
        return delegate.exchangeDeclare(s, s1);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, BuiltinExchangeType builtinExchangeType) throws IOException {
        return delegate.exchangeDeclare(s, builtinExchangeType);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s1, boolean b) throws IOException {
        return delegate.exchangeDeclare(s, s1, b);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, BuiltinExchangeType builtinExchangeType, boolean b) throws IOException {
        return delegate.exchangeDeclare(s, builtinExchangeType, b);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s1, boolean b, boolean b1, Map<String, Object> map) throws IOException {
        return delegate.exchangeDeclare(s, s1, b, b1, map);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, BuiltinExchangeType builtinExchangeType, boolean b, boolean b1, Map<String, Object> map) throws IOException {
        return null;
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, String s1, boolean b, boolean b1, boolean b2, Map<String, Object> map) throws IOException {
        return delegate.exchangeDeclare(s, s1, b, b2, map);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclare(String s, BuiltinExchangeType builtinExchangeType, boolean b, boolean b1, boolean b2, Map<String, Object> map) throws IOException {
        return delegate.exchangeDeclare(s, builtinExchangeType, b, b1, map);
    }

    @Override
    public void exchangeDeclareNoWait(String s, String s1, boolean b, boolean b1, boolean b2, Map<String, Object> map) throws IOException {
        delegate.exchangeDeclareNoWait(s, s1, b, b1, b2, map);
    }

    @Override
    public void exchangeDeclareNoWait(String s, BuiltinExchangeType builtinExchangeType, boolean b, boolean b1, boolean b2, Map<String, Object> map) throws IOException {
        delegate.exchangeDeclare(s, builtinExchangeType, b, b1, b2, map);
    }

    @Override
    public AMQP.Exchange.DeclareOk exchangeDeclarePassive(String s) throws IOException {
        return delegate.exchangeDeclarePassive(s);
    }

    @Override
    public AMQP.Exchange.DeleteOk exchangeDelete(String s, boolean b) throws IOException {
        return delegate.exchangeDelete(s, b);
    }

    @Override
    public void exchangeDeleteNoWait(String s, boolean b) throws IOException {
        delegate.exchangeDeleteNoWait(s, b);
    }

    @Override
    public AMQP.Exchange.DeleteOk exchangeDelete(String s) throws IOException {
        return delegate.exchangeDelete(s);
    }

    @Override
    public AMQP.Exchange.BindOk exchangeBind(String s, String s1, String s2) throws IOException {
        return delegate.exchangeBind(s, s1, s2);
    }

    @Override
    public AMQP.Exchange.BindOk exchangeBind(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        return delegate.exchangeBind(s, s1, s2, map);
    }

    @Override
    public void exchangeBindNoWait(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        delegate.exchangeBindNoWait(s, s1, s2, map);
    }

    @Override
    public AMQP.Exchange.UnbindOk exchangeUnbind(String s, String s1, String s2) throws IOException {
        return delegate.exchangeUnbind(s, s1, s2);
    }

    @Override
    public AMQP.Exchange.UnbindOk exchangeUnbind(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        return delegate.exchangeUnbind(s, s1, s2, map);
    }

    @Override
    public void exchangeUnbindNoWait(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        delegate.exchangeUnbindNoWait(s, s1, s2, map);
    }

    @Override
    public AMQP.Queue.DeclareOk queueDeclare() throws IOException {
        return delegate.queueDeclare();
    }

    @Override
    public AMQP.Queue.DeclareOk queueDeclare(String s, boolean b, boolean b1, boolean b2, Map<String, Object> map) throws IOException {
        return delegate.queueDeclare(s, b, b1, b2, map);
    }

    @Override
    public void queueDeclareNoWait(String s, boolean b, boolean b1, boolean b2, Map<String, Object> map) throws IOException {
        delegate.queueDeclareNoWait(s, b, b1, b2, map);
    }

    @Override
    public AMQP.Queue.DeclareOk queueDeclarePassive(String s) throws IOException {
        return delegate.queueDeclarePassive(s);
    }

    @Override
    public AMQP.Queue.DeleteOk queueDelete(String s) throws IOException {
        return delegate.queueDelete(s);
    }

    @Override
    public AMQP.Queue.DeleteOk queueDelete(String s, boolean b, boolean b1) throws IOException {
        return delegate.queueDelete(s, b, b1);
    }

    @Override
    public void queueDeleteNoWait(String s, boolean b, boolean b1) throws IOException {
        delegate.queueDeleteNoWait(s, b, b1);
    }

    @Override
    public AMQP.Queue.BindOk queueBind(String s, String s1, String s2) throws IOException {
        return delegate.queueBind(s, s1, s2);
    }

    @Override
    public AMQP.Queue.BindOk queueBind(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        return delegate.queueBind(s, s1, s2, map);
    }

    @Override
    public void queueBindNoWait(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        delegate.queueBindNoWait(s, s1, s2, map);
    }

    @Override
    public AMQP.Queue.UnbindOk queueUnbind(String s, String s1, String s2) throws IOException {
        return delegate.queueUnbind(s, s1, s2);
    }

    @Override
    public AMQP.Queue.UnbindOk queueUnbind(String s, String s1, String s2, Map<String, Object> map) throws IOException {
        return delegate.queueUnbind(s, s1, s2, map);
    }

    @Override
    public AMQP.Queue.PurgeOk queuePurge(String s) throws IOException {
        return delegate.queuePurge(s);
    }

    @Override
    public long getNextPublishSeqNo() {
        return delegate.getNextPublishSeqNo();
    }

    @Override
    public boolean waitForConfirms() throws InterruptedException {
        return delegate.waitForConfirms();
    }

    @Override
    public boolean waitForConfirms(long l) throws InterruptedException, TimeoutException {
        return delegate.waitForConfirms(l);
    }

    @Override
    public void waitForConfirmsOrDie() throws IOException, InterruptedException {
        delegate.waitForConfirms();
    }

    @Override
    public void waitForConfirmsOrDie(long l) throws IOException, InterruptedException, TimeoutException {
        delegate.waitForConfirms();
    }

    @Override
    public void addShutdownListener(ShutdownListener shutdownListener) {
        delegate.addShutdownListener(shutdownListener);
    }

    @Override
    public void removeShutdownListener(ShutdownListener shutdownListener) {
        delegate.removeShutdownListener(shutdownListener);
    }

    @Override
    public ShutdownSignalException getCloseReason() {
        return delegate.getCloseReason();
    }

    @Override
    public void notifyListeners() {
        delegate.notifyListeners();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }
}