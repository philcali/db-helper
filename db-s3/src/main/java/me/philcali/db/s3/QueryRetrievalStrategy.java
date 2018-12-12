package me.philcali.db.s3;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import me.philcali.db.api.IPageKey;
import me.philcali.db.api.PageKey;
import me.philcali.db.api.QueryParams;
import me.philcali.db.api.QueryResult;

public class QueryRetrievalStrategy implements BiFunction<QueryParams, AmazonS3, QueryResult<S3ObjectSummary>> {
    private static final String NEXT_TOKEN = "nextToken";

    public static final class Builder {
        private String bucketName;
        private String prefixField;
        private String delimiter;

        public Builder withBucketName(final String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder withPrefixField(final String prefixField) {
            this.prefixField = prefixField;
            return this;
        }

        public Builder withDelimiter(final String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public QueryRetrievalStrategy build() {
            Objects.requireNonNull(bucketName);
            return new QueryRetrievalStrategy(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final String bucketName;
    private final String prefixField;
    private final String delimiter;

    public QueryRetrievalStrategy(final Builder builder) {
        this.bucketName = builder.bucketName;
        this.prefixField = builder.prefixField;
        this.delimiter = builder.delimiter;
    }

    @Override
    public QueryResult<S3ObjectSummary> apply(final QueryParams params, final AmazonS3 s3) {
        final ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withMaxKeys(params.getMaxSize());
        Optional.ofNullable(params.getToken()).ifPresent(token -> {
            request.withContinuationToken(token.getKey().get(NEXT_TOKEN).toString());
        });
        Optional.ofNullable(params.getConditions().get(prefixField)).ifPresent(condition -> {
            request.withPrefix(condition.getValue().toString());
        });
        Optional.ofNullable(delimiter).ifPresent(request::withDelimiter);
        final ListObjectsV2Result result = s3.listObjectsV2(request);
        final Optional<IPageKey> token = Optional.ofNullable(result.getContinuationToken())
                .map(continuation -> new PageKey().addKey(NEXT_TOKEN, continuation.toString()));
        return new QueryResult<>(token.orElse(null), result.getObjectSummaries(), result.isTruncated());
    }
}
