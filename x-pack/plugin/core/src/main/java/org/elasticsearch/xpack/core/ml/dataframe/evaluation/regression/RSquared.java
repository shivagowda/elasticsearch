/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.core.ml.dataframe.evaluation.regression;

import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.core.Tuple;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.PipelineAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.ExtendedStats;
import org.elasticsearch.search.aggregations.metrics.ExtendedStatsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.NumericMetricsAggregation;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.EvaluationFields;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.EvaluationMetric;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.EvaluationMetricResult;
import org.elasticsearch.xpack.core.ml.dataframe.evaluation.EvaluationParameters;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.elasticsearch.xpack.core.ml.dataframe.evaluation.MlEvaluationNamedXContentProvider.registeredMetricName;

/**
 * Calculates R-Squared between two known numerical fields.
 *
 * equation: R-Squared = 1 - SSres/SStot
 * such that,
 * SSres = Σ(y - y´)^2, The residual sum of squares
 * SStot =  Σ(y - y_mean)^2, The total sum of squares
 */
public class RSquared implements EvaluationMetric {

    public static final ParseField NAME = new ParseField("r_squared");

    private static final String PAINLESS_TEMPLATE =
        "def diff = doc[''{0}''].value - doc[''{1}''].value;" +
        "return diff * diff;";
    private static final String SS_RES = "residual_sum_of_squares";

    private static String buildScript(Object...args) {
        return new MessageFormat(PAINLESS_TEMPLATE, Locale.ROOT).format(args);
    }

    private static final ObjectParser<RSquared, Void> PARSER =
        new ObjectParser<>(NAME.getPreferredName(), true, RSquared::new);

    public static RSquared fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    private EvaluationMetricResult result;

    public RSquared(StreamInput in) {}

    public RSquared() {}

    @Override
    public String getName() {
        return NAME.getPreferredName();
    }

    @Override
    public Set<String> getRequiredFields() {
        return Sets.newHashSet(EvaluationFields.ACTUAL_FIELD.getPreferredName(), EvaluationFields.PREDICTED_FIELD.getPreferredName());
    }

    @Override
    public Tuple<List<AggregationBuilder>, List<PipelineAggregationBuilder>> aggs(EvaluationParameters parameters,
                                                                                  EvaluationFields fields) {
        if (result != null) {
            return Tuple.tuple(Collections.emptyList(), Collections.emptyList());
        }
        String actualField = fields.getActualField();
        String predictedField = fields.getPredictedField();
        return Tuple.tuple(
            Arrays.asList(
                AggregationBuilders.sum(SS_RES).script(new Script(buildScript(actualField, predictedField))),
                AggregationBuilders.extendedStats(ExtendedStatsAggregationBuilder.NAME + "_actual").field(actualField)),
            Collections.emptyList());
    }

    @Override
    public void process(Aggregations aggs) {
        NumericMetricsAggregation.SingleValue residualSumOfSquares = aggs.get(SS_RES);
        ExtendedStats extendedStats = aggs.get(ExtendedStatsAggregationBuilder.NAME + "_actual");
        // extendedStats.getVariance() is the statistical sumOfSquares divided by count
        final boolean validResult = residualSumOfSquares == null
            || extendedStats == null
            || extendedStats.getCount() == 0
            || extendedStats.getVariance() == 0;
        result = validResult ?
            new Result(0.0) :
            new Result(1 - (residualSumOfSquares.value() / (extendedStats.getVariance() * extendedStats.getCount())));
    }

    @Override
    public Optional<EvaluationMetricResult> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public String getWriteableName() {
        return registeredMetricName(Regression.NAME, NAME);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        // create static hash code from name as there are currently no unique fields per class instance
        return Objects.hashCode(NAME.getPreferredName());
    }

    public static class Result implements EvaluationMetricResult {

        private static final String VALUE = "value";
        private final double value;

        public Result(double value) {
            this.value = value;
        }

        public Result(StreamInput in) throws IOException {
            this.value = in.readDouble();
        }

        @Override
        public String getWriteableName() {
            return registeredMetricName(Regression.NAME, NAME);
        }

        @Override
        public String getMetricName() {
            return NAME.getPreferredName();
        }

        public double getValue() {
            return value;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            out.writeDouble(value);
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(VALUE, value);
            builder.endObject();
            return builder;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Result other = (Result)o;
            return value == other.value;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(value);
        }
    }
}
