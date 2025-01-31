/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.client.ml;

import org.elasticsearch.client.ml.datafeed.DatafeedStats;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.ConstructingObjectParser;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.elasticsearch.xcontent.ConstructingObjectParser.constructorArg;

/**
 * Contains a {@link List} of the found {@link DatafeedStats} objects and the total count found
 */
public class GetDatafeedStatsResponse extends AbstractResultResponse<DatafeedStats> {

    public static final ParseField RESULTS_FIELD = new ParseField("datafeeds");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<GetDatafeedStatsResponse, Void> PARSER =
        new ConstructingObjectParser<>("get_datafeed_stats_response",
            true,
            a -> new GetDatafeedStatsResponse((List<DatafeedStats>) a[0], (long) a[1]));

    static {
        PARSER.declareObjectArray(constructorArg(), DatafeedStats.PARSER, RESULTS_FIELD);
        PARSER.declareLong(constructorArg(), COUNT);
    }

    GetDatafeedStatsResponse(List<DatafeedStats> results, long count) {
        super(RESULTS_FIELD, results, count);
    }

    /**
     * The collection of {@link DatafeedStats} objects found in the query
     */
    public List<DatafeedStats> datafeedStats() {
        return results;
    }

    public static GetDatafeedStatsResponse fromXContent(XContentParser parser) throws IOException {
        return PARSER.parse(parser, null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(results, count);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        GetDatafeedStatsResponse other = (GetDatafeedStatsResponse) obj;
        return Objects.equals(results, other.results) && count == other.count;
    }

    @Override
    public final String toString() {
        return Strings.toString(this);
    }
}
