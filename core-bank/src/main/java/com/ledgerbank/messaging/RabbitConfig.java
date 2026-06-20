package com.ledgerbank.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ wiring: a durable topic exchange for domain events and a JSON message
 * converter (so events serialize as JSON, readable across services/languages). The
 * converter only trusts our own event package for type-header deserialization.
 */
@Configuration
class RabbitConfig {

	@Bean
	TopicExchange eventsExchange() {
		return new TopicExchange(EventRouting.EXCHANGE, true, false);
	}

	@Bean
	MessageConverter jsonMessageConverter() {
		JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
		DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
		typeMapper.setTrustedPackages("com.ledgerbank.shared.events");
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}
}
