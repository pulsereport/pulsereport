package io.github.pulsereport.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a test metric collected during test execution.
 * Can be used for API response times, page load times, network latency, etc.
 * Immutable value object using Builder pattern.
 * 
 * @author Pulse Report Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Metric {
    
    private final String name;
    private final double value;
    private final String unit;
    private final Instant timestamp;

    @JsonCreator
    private Metric(
            @JsonProperty("name") String name,
            @JsonProperty("value") double value,
            @JsonProperty("unit") String unit,
            @JsonProperty("timestamp") Instant timestamp) {
        this.name = name;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }

    /**
     * Gets the metric name.
     * @return the metric name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the metric value.
     * @return the metric value
     */
    public double getValue() {
        return value;
    }

    /**
     * Gets the unit of measurement.
     * @return the unit (e.g., "ms", "requests/sec", "bytes")
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Gets the timestamp when the metric was recorded.
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Creates a new builder for Metric.
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Compares this Metric to another object for equality.
     * @param o the object to compare with
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metric metric = (Metric) o;
        return Double.compare(metric.value, value) == 0 &&
                Objects.equals(name, metric.name) &&
                Objects.equals(unit, metric.unit) &&
                Objects.equals(timestamp, metric.timestamp);
    }

    /**
     * Returns a hash code value for this Metric.
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, value, unit, timestamp);
    }

    /**
     * Returns a string representation of this Metric.
     * @return a string representation
     */
    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Builder for Metric instances.
     */
    public static final class Builder {
        private String name;
        private double value;
        private String unit;
        private Instant timestamp;

        private Builder() {
        }

        /**
         * Sets the metric name.
         * @param name the metric name (required)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the metric value.
         * @param value the metric value (required)
         * @return this builder
         */
        public Builder value(double value) {
            this.value = value;
            return this;
        }

        /**
         * Sets the unit of measurement.
         * @param unit the unit (required)
         * @return this builder
         */
        public Builder unit(String unit) {
            this.unit = unit;
            return this;
        }

        /**
         * Sets the timestamp.
         * @param timestamp the timestamp (required)
         * @return this builder
         */
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Builds the Metric instance.
         * @return a new Metric
         * @throws IllegalArgumentException if required fields are missing
         */
        public Metric build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("name is required");
            }
            if (unit == null || unit.trim().isEmpty()) {
                throw new IllegalArgumentException("unit is required");
            }
            if (timestamp == null) {
                throw new IllegalArgumentException("timestamp is required");
            }
            return new Metric(name, value, unit, timestamp);
        }
    }
}
