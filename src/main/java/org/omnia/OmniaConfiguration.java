package org.omnia;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpointMetricReader;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OmniaConfiguration {

	private static final Log logger = LogFactory.getLog(OmniaConfiguration.class);

	@Value("${omnia.collector.host:collector}")
	private String host;

	@Value("${omnia.collector.port:8125}")
	private int port;

	@Value("${spring.application.name:service}")
	private String service;

	@Value("${random.uuid:0000}")
	private String serviceiid;

	@Bean
	public MetricsEndpointMetricReader metricsEndpointMetricReader(final MetricsEndpoint metricsEndpoint) {
		return new MetricsEndpointMetricReader(metricsEndpoint);
	}

	@Bean
	@ExportMetricWriter
	MetricWriter metricWriter() {
		Map<String, String> tags = new HashMap<String, String>();
		tags.put("service", service);
		tags.put("serviceiid", serviceiid);
		logger.info("Configuring StatsdTaggedMetricWriter with host: " + host + ", port: " + port + " and tags: "
				+ tags.toString());
		return new StatsdTaggedMetricWriter(host, port, tags);
	}

}
