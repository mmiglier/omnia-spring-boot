package org.omnia;

import java.io.Closeable;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.util.Assert;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import com.timgroup.statsd.StatsDClientErrorHandler;

/**
 * A {@link MetricWriter} that pushes data to a host in statsd format, with
 * support to tags according to the <a href=
 * "https://www.influxdata.com/getting-started-with-sending-statsd-metrics-to-telegraf-influxdb/">Influx
 * line protocol</a>). Statsd has the concept of counters and gauges, but only
 * supports gauges with data type Long, so values will be truncated towards
 * zero. Metrics whose name contains "timer." (but not "gauge." or "counter.")
 * will be treated as execution times (in statsd terms). Anything incremented is
 * treated as a counter, and anything with a snapshot value in
 * {@link #set(Metric)} is treated as a gauge.
 *
 * @author Dave Syer, Marco Miglierina
 * @since 1.3.0
 */
public class StatsdTaggedMetricWriter implements MetricWriter, Closeable {

	private static final Log logger = LogFactory.getLog(StatsdTaggedMetricWriter.class);

	private final StatsDClient client;
	private final String globalTagsString;

	/**
	 * Create a new writer with the given client.
	 * 
	 * @param client
	 *            StatsD client to write metrics with
	 */
	public StatsdTaggedMetricWriter(StatsDClient client, Map<String, String> globalTags) {
		Assert.notNull(client);
		if (globalTags == null)
			globalTagsString = "";
		else {
			globalTagsString = globalTags.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
					.collect(Collectors.joining(","));
		}
		this.client = client;
	}

	public StatsdTaggedMetricWriter(String host, int port, Map<String, String> globalTags) {
		this(new NonBlockingStatsDClient(null, host, port, new LoggingStatsdErrorHandler()), globalTags);
	}

	public StatsdTaggedMetricWriter(String host, int port) {
		this(host, port, null);
	}

	public void increment(Delta<?> delta) {
		this.client.count(delta.getName(), delta.getValue().longValue());
	}

	public void set(Metric<?> value) {
		// Convert tags from Netflix to Influx format + sanitize
		String name = value.getName().replaceAll("\\((.+(,.+)*)\\)", ",$1").replace(':', '.')
				+ (globalTagsString != "" ? "," + globalTagsString : "");
		if (name.contains("timer.") && !name.contains("gauge.") && !name.contains("counter.")) {
			this.client.recordExecutionTime(name, value.getValue().longValue());
		} else {
			if (name.contains("counter.")) {
				this.client.count(name, value.getValue().longValue());
			} else {
				this.client.gauge(name, value.getValue().doubleValue());
			}
		}
	}

	public void reset(String name) {
		// Not implemented
	}

	public void close() {
		this.client.stop();
	}

	private static final class LoggingStatsdErrorHandler implements StatsDClientErrorHandler {

		public void handle(Exception e) {
			logger.debug("Failed to write metric. Exception: " + e.getClass() + ", message: " + e.getMessage());
		}

	}

}
