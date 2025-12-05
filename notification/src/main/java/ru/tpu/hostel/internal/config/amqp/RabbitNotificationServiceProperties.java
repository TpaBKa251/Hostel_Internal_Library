//package ru.tpu.hostel.internal.config.amqp;
//
//import jakarta.validation.constraints.NotEmpty;
//import jakarta.validation.constraints.NotNull;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.boot.convert.DurationUnit;
//import org.springframework.validation.annotation.Validated;
//
//import java.time.Duration;
//import java.time.temporal.ChronoUnit;
//
//@Validated
//@ConfigurationProperties(prefix = "rabbitmq.notification-service")
//@Data
//public class RabbitNotificationServiceProperties {
//
//    @NotEmpty
//    private String username = "notification";
//
//    @NotEmpty
//    private String password = "notification";
//
//    @NotEmpty
//    private String virtualHost = "notification-service";
//
//    @NotEmpty
//    private String addresses;
//
//    @NotNull
//    @DurationUnit(ChronoUnit.MILLIS)
//    private Duration connectionTimeout = Duration.of(6, ChronoUnit.SECONDS);
//
//}
