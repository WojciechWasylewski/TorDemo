package org.example.tordemo.exitnode.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.validation.BindValidationException
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.NestedExceptionUtils
import org.springframework.validation.FieldError
import spock.lang.Specification

import java.time.Duration

import static org.assertj.core.api.Assertions.assertThat

class TorConfigurationSpec extends Specification {

    private static final Map<String, String> VALID_PROPERTIES = [
            'tor.cache.ttl'             : '30m',
            'tor.client.base-url'       : 'https://check.torproject.org',
            'tor.client.connect-timeout': '5s',
            'tor.client.read-timeout'   : '10s'
    ].asImmutable()

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertyBindingConfiguration)

    def "binds valid durations and an absolute #scheme URL"() {
        given:
        Map<String, String> properties = new LinkedHashMap<>(VALID_PROPERTIES)
        properties['tor.client.base-url'] = "${scheme}://example.com:8080"

        expect:
        withProperties(properties).run { context ->
            assertThat(context).hasNotFailed()
            TorCacheProperties cache = context.getBean(TorCacheProperties)
            TorClientProperties client = context.getBean(TorClientProperties)
            assert cache.ttl() == Duration.ofMinutes(30)
            assert client.baseUrl() == URI.create("${scheme}://example.com:8080")
            assert client.connectTimeout() == Duration.ofSeconds(5)
            assert client.readTimeout() == Duration.ofSeconds(10)
        }

        where:
        scheme << ['http', 'https']
    }

    def "fails startup when required property #property is missing"() {
        given:
        Map<String, String> properties = new LinkedHashMap<>(VALID_PROPERTIES)
        properties.remove(property)

        expect:
        withProperties(properties).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).hasRootCauseInstanceOf(BindValidationException)
            BindValidationException validationFailure =
                    (BindValidationException) NestedExceptionUtils.getMostSpecificCause(context.startupFailure)
            assert validationFailure.validationErrors.allErrors
                    .findAll { it instanceof FieldError }
                    .any { FieldError error -> error.field == field && error.codes.contains('NotNull') }
        }

        where:
        property                     | field
        'tor.cache.ttl'              | 'ttl'
        'tor.client.base-url'        | 'baseUrl'
        'tor.client.connect-timeout' | 'connectTimeout'
        'tor.client.read-timeout'    | 'readTimeout'
    }

    def "fails startup when #property is #value"() {
        given:
        Map<String, String> properties = new LinkedHashMap<>(VALID_PROPERTIES)
        properties[property] = value

        expect:
        withProperties(properties).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure).hasStackTraceContaining("${property} must be positive")
        }

        where:
        property                     | value
        'tor.cache.ttl'              | '0s'
        'tor.cache.ttl'              | '-1s'
        'tor.client.connect-timeout' | '0s'
        'tor.client.connect-timeout' | '-1s'
        'tor.client.read-timeout'    | '0s'
        'tor.client.read-timeout'    | '-1s'
    }

    def "fails startup for an invalid base URL: #baseUrl"() {
        given:
        Map<String, String> properties = new LinkedHashMap<>(VALID_PROPERTIES)
        properties['tor.client.base-url'] = baseUrl

        expect:
        withProperties(properties).run { context ->
            assertThat(context).hasFailed()
            assertThat(context.startupFailure)
                    .hasStackTraceContaining('tor.client.base-url must be an absolute HTTP(S) URL')
        }

        where:
        baseUrl << ['/relative', 'ftp://example.com', 'https:/missing-host']
    }

    private ApplicationContextRunner withProperties(Map<String, String> properties) {
        String[] values = properties.collect { String key, String value -> "${key}=${value}" } as String[]
        contextRunner.withPropertyValues(values)
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties([TorCacheProperties, TorClientProperties])
    static class PropertyBindingConfiguration {
    }
}
