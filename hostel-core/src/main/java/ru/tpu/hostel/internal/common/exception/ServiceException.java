package ru.tpu.hostel.internal.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Общее исключение сервиса. Имеет вложенные классы для всех 4хх и 5хх ошибок,
 * названия соответствуют ошибкам.
 * <p>💡При создании кастомных исключений, из-за которых может быть падение (т.е. при выбросе клиенту возвращается
 * ошибка), наследоваться от него или вложенного наследника.
 * <p>💡При написании исключений, которые не вызывают падение запроса, необязательно наследоваться от этого исключения
 * или его наследников.
 *
 * <p>Пример кастомного исключения:
 * <pre><code>
 *     public class Some400Exception extends ServiceException.BadRequest {
 *
 *         public Some400Exception(String message) {
 *             super(message);
 *         }
 *
 *         public Some400Exception(String message, Throwable cause) {
 *             super(message, cause);
 *         }
 *
 *         public Some400Exception() {
 *             // 1) пустой
 *             // 2) super();
 *             // 3) this("Что-то пошло не так, виноват ты");
 *             // 4) super("Что-то пошло не так, виноват ты");
 *         }
 *     }
 * </code></pre>
 *
 * @author Лапшин Илья
 * @version 1.0.3
 * @since 1.0.0
 */
@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus status;

    public ServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ServiceException(HttpStatus status) {
        this(status.getReasonPhrase(), status);
    }

    public ServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public ServiceException(String message, ServiceException cause) {
        this(message, cause.status, cause);
    }

    // 4xx Client Error
    public static class BadRequest extends ServiceException {
        public BadRequest(String message) {
            super(message, HttpStatus.BAD_REQUEST);
        }

        public BadRequest(String message, Throwable cause) {
            super(message, HttpStatus.BAD_REQUEST, cause);
        }

        public BadRequest() {
            this("Bad Request");
        }
    }

    public static class Unauthorized extends ServiceException {
        public Unauthorized(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }

        public Unauthorized(String message, Throwable cause) {
            super(message, HttpStatus.UNAUTHORIZED, cause);
        }

        public Unauthorized() {
            this("Unauthorized");
        }
    }

    public static class PaymentRequired extends ServiceException {
        public PaymentRequired(String message) {
            super(message, HttpStatus.PAYMENT_REQUIRED);
        }

        public PaymentRequired(String message, Throwable cause) {
            super(message, HttpStatus.PAYMENT_REQUIRED, cause);
        }

        public PaymentRequired() {
            this("Payment Required");
        }
    }

    public static class Forbidden extends ServiceException {
        public Forbidden(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }

        public Forbidden(String message, Throwable cause) {
            super(message, HttpStatus.FORBIDDEN, cause);
        }

        public Forbidden() {
            this("Forbidden");
        }
    }

    public static class NotFound extends ServiceException {
        public NotFound(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }

        public NotFound(String message, Throwable cause) {
            super(message, HttpStatus.NOT_FOUND, cause);
        }

        public NotFound() {
            this("Not Found");
        }
    }

    public static class MethodNotAllowed extends ServiceException {
        public MethodNotAllowed(String message) {
            super(message, HttpStatus.METHOD_NOT_ALLOWED);
        }

        public MethodNotAllowed(String message, Throwable cause) {
            super(message, HttpStatus.METHOD_NOT_ALLOWED, cause);
        }

        public MethodNotAllowed() {
            this("Method Not Allowed");
        }
    }

    public static class NotAcceptable extends ServiceException {
        public NotAcceptable(String message) {
            super(message, HttpStatus.NOT_ACCEPTABLE);
        }

        public NotAcceptable(String message, Throwable cause) {
            super(message, HttpStatus.NOT_ACCEPTABLE, cause);
        }

        public NotAcceptable() {
            this("Not Acceptable");
        }
    }

    public static class ProxyAuthenticationRequired extends ServiceException {
        public ProxyAuthenticationRequired(String message) {
            super(message, HttpStatus.PROXY_AUTHENTICATION_REQUIRED);
        }

        public ProxyAuthenticationRequired(String message, Throwable cause) {
            super(message, HttpStatus.PROXY_AUTHENTICATION_REQUIRED, cause);
        }

        public ProxyAuthenticationRequired() {
            this("Proxy Authentication Required");
        }
    }

    public static class RequestTimeout extends ServiceException {
        public RequestTimeout(String message) {
            super(message, HttpStatus.REQUEST_TIMEOUT);
        }

        public RequestTimeout(String message, Throwable cause) {
            super(message, HttpStatus.REQUEST_TIMEOUT, cause);
        }

        public RequestTimeout() {
            this("Request Timeout");
        }
    }

    public static class Conflict extends ServiceException {
        public Conflict(String message) {
            super(message, HttpStatus.CONFLICT);
        }

        public Conflict(String message, Throwable cause) {
            super(message, HttpStatus.CONFLICT, cause);
        }

        public Conflict() {
            this("Conflict");
        }
    }

    public static class Gone extends ServiceException {
        public Gone(String message) {
            super(message, HttpStatus.GONE);
        }

        public Gone(String message, Throwable cause) {
            super(message, HttpStatus.GONE, cause);
        }

        public Gone() {
            this("Gone");
        }
    }

    public static class LengthRequired extends ServiceException {
        public LengthRequired(String message) {
            super(message, HttpStatus.LENGTH_REQUIRED);
        }

        public LengthRequired(String message, Throwable cause) {
            super(message, HttpStatus.LENGTH_REQUIRED, cause);
        }

        public LengthRequired() {
            this("Length Required");
        }
    }

    public static class PreconditionFailed extends ServiceException {
        public PreconditionFailed(String message) {
            super(message, HttpStatus.PRECONDITION_FAILED);
        }

        public PreconditionFailed(String message, Throwable cause) {
            super(message, HttpStatus.PRECONDITION_FAILED, cause);
        }

        public PreconditionFailed() {
            this("Precondition Failed");
        }
    }

    public static class PayloadTooLarge extends ServiceException {
        public PayloadTooLarge(String message) {
            super(message, HttpStatus.PAYLOAD_TOO_LARGE);
        }

        public PayloadTooLarge(String message, Throwable cause) {
            super(message, HttpStatus.PAYLOAD_TOO_LARGE, cause);
        }

        public PayloadTooLarge() {
            this("Payload Too Large");
        }
    }

    public static class UriTooLong extends ServiceException {
        public UriTooLong(String message) {
            super(message, HttpStatus.URI_TOO_LONG);
        }

        public UriTooLong(String message, Throwable cause) {
            super(message, HttpStatus.URI_TOO_LONG, cause);
        }

        public UriTooLong() {
            this("URI Too Long");
        }
    }

    public static class UnsupportedMediaType extends ServiceException {
        public UnsupportedMediaType(String message) {
            super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        public UnsupportedMediaType(String message, Throwable cause) {
            super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, cause);
        }

        public UnsupportedMediaType() {
            this("Unsupported Media Type");
        }
    }

    public static class RequestedRangeNotSatisfiable extends ServiceException {
        public RequestedRangeNotSatisfiable(String message) {
            super(message, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
        }

        public RequestedRangeNotSatisfiable(String message, Throwable cause) {
            super(message, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, cause);
        }

        public RequestedRangeNotSatisfiable() {
            this("Requested Range Not Satisfiable");
        }
    }

    public static class ExpectationFailed extends ServiceException {
        public ExpectationFailed(String message) {
            super(message, HttpStatus.EXPECTATION_FAILED);
        }

        public ExpectationFailed(String message, Throwable cause) {
            super(message, HttpStatus.EXPECTATION_FAILED, cause);
        }

        public ExpectationFailed() {
            this("Expectation Failed");
        }
    }

    public static class Teapot extends ServiceException {
        public Teapot(String message) {
            super(message, HttpStatus.I_AM_A_TEAPOT);
        }

        public Teapot(String message, Throwable cause) {
            super(message, HttpStatus.I_AM_A_TEAPOT, cause);
        }

        public Teapot() {
            this("I Am A Teapot");
        }
    }

    public static class UnprocessableEntity extends ServiceException {
        public UnprocessableEntity(String message) {
            super(message, HttpStatus.UNPROCESSABLE_ENTITY);
        }

        public UnprocessableEntity(String message, Throwable cause) {
            super(message, HttpStatus.UNPROCESSABLE_ENTITY, cause);
        }

        public UnprocessableEntity() {
            this("Unprocessable Entity");
        }
    }

    public static class Locked extends ServiceException {
        public Locked(String message) {
            super(message, HttpStatus.LOCKED);
        }

        public Locked(String message, Throwable cause) {
            super(message, HttpStatus.LOCKED, cause);
        }

        public Locked() {
            this("Locked");
        }
    }

    public static class FailedDependency extends ServiceException {
        public FailedDependency(String message) {
            super(message, HttpStatus.FAILED_DEPENDENCY);
        }

        public FailedDependency(String message, Throwable cause) {
            super(message, HttpStatus.FAILED_DEPENDENCY, cause);
        }

        public FailedDependency() {
            this("Failed Dependency");
        }
    }

    public static class TooEarly extends ServiceException {
        public TooEarly(String message) {
            super(message, HttpStatus.TOO_EARLY);
        }

        public TooEarly(String message, Throwable cause) {
            super(message, HttpStatus.TOO_EARLY, cause);
        }

        public TooEarly() {
            this("Too Early");
        }
    }

    public static class UpgradeRequired extends ServiceException {
        public UpgradeRequired(String message) {
            super(message, HttpStatus.UPGRADE_REQUIRED);
        }

        public UpgradeRequired(String message, Throwable cause) {
            super(message, HttpStatus.UPGRADE_REQUIRED, cause);
        }

        public UpgradeRequired() {
            this("Upgrade Required");
        }
    }

    public static class PreconditionRequired extends ServiceException {
        public PreconditionRequired(String message) {
            super(message, HttpStatus.PRECONDITION_REQUIRED);
        }

        public PreconditionRequired(String message, Throwable cause) {
            super(message, HttpStatus.PRECONDITION_REQUIRED, cause);
        }

        public PreconditionRequired() {
            this("Precondition Required");
        }
    }

    public static class TooManyRequests extends ServiceException {
        public TooManyRequests(String message) {
            super(message, HttpStatus.TOO_MANY_REQUESTS);
        }

        public TooManyRequests(String message, Throwable cause) {
            super(message, HttpStatus.TOO_MANY_REQUESTS, cause);
        }

        public TooManyRequests() {
            this("Too Many Requests");
        }
    }

    public static class RequestHeaderFieldsTooLarge extends ServiceException {
        public RequestHeaderFieldsTooLarge(String message) {
            super(message, HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
        }

        public RequestHeaderFieldsTooLarge(String message, Throwable cause) {
            super(message, HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, cause);
        }

        public RequestHeaderFieldsTooLarge() {
            this("Request Header Fields Too Large");
        }
    }

    public static class UnavailableForLegalReasons extends ServiceException {
        public UnavailableForLegalReasons(String message) {
            super(message, HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
        }

        public UnavailableForLegalReasons(String message, Throwable cause) {
            super(message, HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, cause);
        }

        public UnavailableForLegalReasons() {
            this("Unavailable For Legal Reasons");
        }
    }

    // 5xx Server Error
    public static class InternalServerError extends ServiceException {
        public InternalServerError(String message) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        public InternalServerError(String message, Throwable cause) {
            super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
        }

        public InternalServerError() {
            this("Internal Server Error");
        }
    }

    public static class NotImplemented extends ServiceException {
        public NotImplemented(String message) {
            super(message, HttpStatus.NOT_IMPLEMENTED);
        }

        public NotImplemented(String message, Throwable cause) {
            super(message, HttpStatus.NOT_IMPLEMENTED, cause);
        }

        public NotImplemented() {
            this("Not Implemented");
        }
    }

    public static class BadGateway extends ServiceException {
        public BadGateway(String message) {
            super(message, HttpStatus.BAD_GATEWAY);
        }

        public BadGateway(String message, Throwable cause) {
            super(message, HttpStatus.BAD_GATEWAY, cause);
        }

        public BadGateway() {
            this("Bad Gateway");
        }
    }

    public static class ServiceUnavailable extends ServiceException {
        public ServiceUnavailable(String message) {
            super(message, HttpStatus.SERVICE_UNAVAILABLE);
        }

        public ServiceUnavailable(String message, Throwable cause) {
            super(message, HttpStatus.SERVICE_UNAVAILABLE, cause);
        }

        public ServiceUnavailable() {
            this("Service Unavailable");
        }
    }

    public static class GatewayTimeout extends ServiceException {
        public GatewayTimeout(String message) {
            super(message, HttpStatus.GATEWAY_TIMEOUT);
        }

        public GatewayTimeout(String message, Throwable cause) {
            super(message, HttpStatus.GATEWAY_TIMEOUT, cause);
        }

        public GatewayTimeout() {
            this("Gateway Timeout");
        }
    }

    public static class HttpVersionNotSupported extends ServiceException {
        public HttpVersionNotSupported(String message) {
            super(message, HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
        }

        public HttpVersionNotSupported(String message, Throwable cause) {
            super(message, HttpStatus.HTTP_VERSION_NOT_SUPPORTED, cause);
        }

        public HttpVersionNotSupported() {
            this("HTTP Version Not Supported");
        }
    }

    public static class VariantAlsoNegotiates extends ServiceException {
        public VariantAlsoNegotiates(String message) {
            super(message, HttpStatus.VARIANT_ALSO_NEGOTIATES);
        }

        public VariantAlsoNegotiates(String message, Throwable cause) {
            super(message, HttpStatus.VARIANT_ALSO_NEGOTIATES, cause);
        }

        public VariantAlsoNegotiates() {
            this("Variant Also Negotiates");
        }
    }

    public static class InsufficientStorage extends ServiceException {
        public InsufficientStorage(String message) {
            super(message, HttpStatus.INSUFFICIENT_STORAGE);
        }

        public InsufficientStorage(String message, Throwable cause) {
            super(message, HttpStatus.INSUFFICIENT_STORAGE, cause);
        }

        public InsufficientStorage() {
            this("Insufficient Storage");
        }
    }

    public static class LoopDetected extends ServiceException {
        public LoopDetected(String message) {
            super(message, HttpStatus.LOOP_DETECTED);
        }

        public LoopDetected(String message, Throwable cause) {
            super(message, HttpStatus.LOOP_DETECTED, cause);
        }

        public LoopDetected() {
            this("Loop Detected");
        }
    }

    public static class BandwidthLimitExceeded extends ServiceException {
        public BandwidthLimitExceeded(String message) {
            super(message, HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
        }

        public BandwidthLimitExceeded(String message, Throwable cause) {
            super(message, HttpStatus.BANDWIDTH_LIMIT_EXCEEDED, cause);
        }

        public BandwidthLimitExceeded() {
            this("Bandwidth Limit Exceeded");
        }
    }

    public static class NotExtended extends ServiceException {
        public NotExtended(String message) {
            super(message, HttpStatus.NOT_EXTENDED);
        }

        public NotExtended(String message, Throwable cause) {
            super(message, HttpStatus.NOT_EXTENDED, cause);
        }

        public NotExtended() {
            this("Not Extended");
        }
    }

    public static class NetworkAuthenticationRequired extends ServiceException {
        public NetworkAuthenticationRequired(String message) {
            super(message, HttpStatus.NETWORK_AUTHENTICATION_REQUIRED);
        }

        public NetworkAuthenticationRequired(String message, Throwable cause) {
            super(message, HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, cause);
        }

        public NetworkAuthenticationRequired() {
            this("Network Authentication Required");
        }
    }

}
