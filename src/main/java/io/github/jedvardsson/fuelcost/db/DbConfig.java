package io.github.jedvardsson.fuelcost.db;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.protobuf.Any;
import io.github.jedvardsson.fuelcost.common.VersionEtag;
import io.github.jedvardsson.fuelcost.grpc.ProtoHelpers;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.codec.CodecFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.jackson2.Jackson2Config;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

@Configuration
public class DbConfig {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .addModule(new ParameterNamesModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        final Jdbi jdbi = Jdbi.create(new TransactionAwareDataSourceProxy(dataSource));
        jdbi.installPlugin(new PostgresPlugin());
        jdbi.installPlugin(new Jackson2Plugin());
        jdbi.getConfig(Jackson2Config.class).setMapper(JSON_MAPPER);

        jdbi.registerRowMapper((RowMapperFactory) (t, r) ->
                t instanceof Class<?> c && c.isRecord() ? Optional.of(ConstructorMapper.of(c)) : Optional.empty());

        jdbi.registerColumnMapper(VersionEtag.class, (ColumnMapper<VersionEtag>) (r, columnNumber, ctx) -> {
            long version = r.getLong(columnNumber);
            return r.wasNull() ? null : VersionEtag.of(version);
        });

        jdbi.registerCodecFactory(CodecFactory.builder()
                .addCodec(com.google.type.Date.class, new ProtoDateJdbiCodec())
                .addCodec(com.google.protobuf.Timestamp.class, new ProtoTimestampJdbiCodec())
                .addCodec(Any.class, new ProtoAnyCodec())
                .build());
        return jdbi;
    }

    public static class ProtoDateJdbiCodec implements Codec<com.google.type.Date> {
        @Override
        public ColumnMapper<com.google.type.Date> getColumnMapper() {
            return (r, idx, ctx) -> {
                LocalDate v = r.getObject(idx, LocalDate.class);
                return v == null ? null : ProtoHelpers.protoDateOf(v);
            };
        }

        @Override
        public Function<com.google.type.Date, Argument> getArgumentFunction() {
            return v -> (idx, stmt, ctx) -> stmt.setObject(idx, v == null ? null : ProtoHelpers.javaLocalDateOf(v), Types.DATE);
        }
    }

    public static class ProtoTimestampJdbiCodec implements Codec<com.google.protobuf.Timestamp> {
        @Override
        public ColumnMapper<com.google.protobuf.Timestamp> getColumnMapper() {
            return (r, idx, ctx) -> {
                java.sql.Timestamp v = r.getTimestamp(idx);
                return v == null ? null : ProtoHelpers.protoTimestampOf(v.toInstant());
            };
        }

        @Override
        public Function<com.google.protobuf.Timestamp, Argument> getArgumentFunction() {
            return v -> (idx, stmt, ctx) -> stmt.setTimestamp(idx, v == null ? null : java.sql.Timestamp.from(ProtoHelpers.javaInstanceOf(v)));
        }
    }

    public static class ProtoAnyCodec implements Codec<Any> {
        @Override
        public ColumnMapper<Any> getColumnMapper() {
            return (r, idx, ctx) -> {
                String json = r.getString(idx);
                return json == null ? null : ProtoHelpers.parseJson(json, Any.newBuilder()).build();
            };
        }

        @Override
        public Function<Any, Argument> getArgumentFunction() {
            return v -> (idx, stmt, ctx) -> stmt.setObject(idx, v == null ? null : ProtoHelpers.printJson(v), Types.OTHER);
        }
    }
}
