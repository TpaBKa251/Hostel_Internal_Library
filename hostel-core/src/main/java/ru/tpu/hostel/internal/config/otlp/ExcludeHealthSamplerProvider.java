package ru.tpu.hostel.internal.config.otlp;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.springframework.stereotype.Component;

@Component
public class ExcludeHealthSamplerProvider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
        autoConfigurationCustomizer.addSamplerCustomizer((fallbackSampler, _) ->
                RuleBasedRoutingSampler.builder(SpanKind.SERVER, fallbackSampler)
                        .drop(AttributeKey.stringKey("http.url"), "^/(actuator|health|metrics).*")
                        .drop(AttributeKey.stringKey("url.path"), "^/(actuator|health|metrics).*")
                        .build()
        );
    }

}
