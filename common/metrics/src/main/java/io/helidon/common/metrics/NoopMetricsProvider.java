/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.common.metrics;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.helidon.common.metrics.spi.MetricsProvider;
import io.helidon.config.Config;

import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricFilter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.Timer;

class NoopMetricsProvider implements MetricsProvider {
    private static final MetricRegistry NOOP_REGISTRY = new NoopRegistry();
    private static final Counter COUNTER = new NoopCounter();
    private static final ConcurrentGauge GAUGE = new NoopGauge();
    private static final Histogram HISTOGRAM = new NoopHistogram();
    private static final Snapshot SNAPSHOT = new NoopSnapshot();
    private static final Meter METER = new NoopMeter();
    private static final Timer TIMER = new NoopTimer();
    private static final Timer.Context CONTEXT = new NoopContext();
    private static final SortedSet<?> EMPTY_SORTED_SET = Collections.unmodifiableSortedSet(new TreeSet<>());
    private static final SortedMap<?, ?> EMPTY_SORTED_MAP = Collections.unmodifiableSortedMap(new TreeMap<>());

    @Override
    public MetricRegistry createRegistry(Config config, MetricRegistry.Type registryType) {
        return NOOP_REGISTRY;
    }

    private static class NoopRegistry extends MetricRegistry {

        @Override
        public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
            return metric;
        }

        @Override
        public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
            return metric;
        }

        @Override
        public <T extends Metric> T register(Metadata metadata, T metric, Tag... tags) throws IllegalArgumentException {
            return metric;
        }

        @Override
        public Counter counter(String name) {
            return COUNTER;
        }

        @Override
        public Counter counter(String name, Tag... tags) {
            return COUNTER;
        }

        @Override
        public Counter counter(Metadata metadata) {
            return COUNTER;
        }

        @Override
        public Counter counter(Metadata metadata, Tag... tags) {
            return COUNTER;
        }

        @Override
        public ConcurrentGauge concurrentGauge(String name) {
            return GAUGE;
        }

        @Override
        public ConcurrentGauge concurrentGauge(String name, Tag... tags) {
            return GAUGE;
        }

        @Override
        public ConcurrentGauge concurrentGauge(Metadata metadata) {
            return GAUGE;
        }

        @Override
        public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
            return GAUGE;
        }

        @Override
        public Histogram histogram(String name) {
            return HISTOGRAM;
        }

        @Override
        public Histogram histogram(String name, Tag... tags) {
            return HISTOGRAM;
        }

        @Override
        public Histogram histogram(Metadata metadata) {
            return HISTOGRAM;
        }

        @Override
        public Histogram histogram(Metadata metadata, Tag... tags) {
            return HISTOGRAM;
        }

        @Override
        public Meter meter(String name) {
            return METER;
        }

        @Override
        public Meter meter(String name, Tag... tags) {
            return METER;
        }

        @Override
        public Meter meter(Metadata metadata) {
            return METER;
        }

        @Override
        public Meter meter(Metadata metadata, Tag... tags) {
            return METER;
        }

        @Override
        public Timer timer(String name) {
            return TIMER;
        }

        @Override
        public Timer timer(String name, Tag... tags) {
            return TIMER;
        }

        @Override
        public Timer timer(Metadata metadata) {
            return TIMER;
        }

        @Override
        public Timer timer(Metadata metadata, Tag... tags) {
            return TIMER;
        }

        @Override
        public boolean remove(String name) {
            return false;
        }

        @Override
        public boolean remove(MetricID metricID) {
            return false;
        }

        @Override
        public void removeMatching(MetricFilter filter) {

        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedSet<String> getNames() {
            return (SortedSet<String>) EMPTY_SORTED_SET;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedSet<MetricID> getMetricIDs() {
            return (SortedSet<MetricID>) EMPTY_SORTED_SET;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Gauge> getGauges() {
            return (SortedMap<MetricID, Gauge>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Gauge> getGauges(MetricFilter filter) {
            return (SortedMap<MetricID, Gauge>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Counter> getCounters() {
            return (SortedMap<MetricID, Counter>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Counter> getCounters(MetricFilter filter) {
            return (SortedMap<MetricID, Counter>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
            return (SortedMap<MetricID, ConcurrentGauge>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter filter) {
            return (SortedMap<MetricID, ConcurrentGauge>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Histogram> getHistograms() {
            return (SortedMap<MetricID, Histogram>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Histogram> getHistograms(MetricFilter filter) {
            return (SortedMap<MetricID, Histogram>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Meter> getMeters() {
            return (SortedMap<MetricID, Meter>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Meter> getMeters(MetricFilter filter) {
            return (SortedMap<MetricID, Meter>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Timer> getTimers() {
            return (SortedMap<MetricID, Timer>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SortedMap<MetricID, Timer> getTimers(MetricFilter filter) {
            return (SortedMap<MetricID, Timer>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<MetricID, Metric> getMetrics() {
            return (Map<MetricID, Metric>) EMPTY_SORTED_MAP;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map<String, Metadata> getMetadata() {
            return (Map<String, Metadata>) EMPTY_SORTED_MAP;
        }
    }

    private static class NoopCounter implements Counter {
        @Override
        public void inc() {
        }

        @Override
        public void inc(long n) {
        }

        @Override
        public long getCount() {
            return 0;
        }
    }

    private static class NoopGauge implements ConcurrentGauge {
        @Override
        public long getCount() {
            return 0;
        }

        @Override
        public long getMax() {
            return 0;
        }

        @Override
        public long getMin() {
            return 0;
        }

        @Override
        public void inc() {

        }

        @Override
        public void dec() {

        }
    }

    private static class NoopHistogram implements Histogram {
        @Override
        public void update(int value) {

        }

        @Override
        public void update(long value) {

        }

        @Override
        public long getCount() {
            return 0;
        }

        @Override
        public Snapshot getSnapshot() {
            return SNAPSHOT;
        }
    }

    private static class NoopSnapshot extends Snapshot {
        @Override
        public double getValue(double quantile) {
            return 0;
        }

        @Override
        public long[] getValues() {
            return new long[0];
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public long getMax() {
            return 0;
        }

        @Override
        public double getMean() {
            return 0;
        }

        @Override
        public long getMin() {
            return 0;
        }

        @Override
        public double getStdDev() {
            return 0;
        }

        @Override
        public void dump(OutputStream output) {

        }
    }

    private static class NoopMeter extends NoopCounter implements Meter {
        @Override
        public void mark() {

        }

        @Override
        public void mark(long n) {

        }

        @Override
        public double getFifteenMinuteRate() {
            return 0;
        }

        @Override
        public double getFiveMinuteRate() {
            return 0;
        }

        @Override
        public double getMeanRate() {
            return 0;
        }

        @Override
        public double getOneMinuteRate() {
            return 0;
        }
    }

    private static class NoopTimer extends NoopMeter implements Timer {

        @Override
        public void update(long duration, TimeUnit unit) {

        }

        @Override
        public <T> T time(Callable<T> event) throws Exception {
            return event.call();
        }

        @Override
        public void time(Runnable event) {
            event.run();
        }

        @Override
        public Context time() {
            return CONTEXT;
        }

        @Override
        public Snapshot getSnapshot() {
            return SNAPSHOT;
        }
    }

    private static class NoopContext implements Timer.Context {
        @Override
        public long stop() {
            return 0;
        }

        @Override
        public void close() {
        }
    }
}
