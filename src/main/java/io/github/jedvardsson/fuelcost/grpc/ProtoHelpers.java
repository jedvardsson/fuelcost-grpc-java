package io.github.jedvardsson.fuelcost.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import com.google.type.Date;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Pattern;


public class ProtoHelpers {
    private ProtoHelpers() {
    }

    public static LocalDate javaLocalDateOf(Date x) {
        return x == null ? null : LocalDate.of(x.getYear(), x.getMonth() == 0 ? 1 : x.getMonth(), x.getDay() == 0 ? 1 : x.getDay());
    }

    public static Date protoDateOf(LocalDate x) {
        return x == null ? null : Date.newBuilder()
                .setYear(x.getYear())
                .setMonth(x.getMonthValue())
                .setDay(x.getDayOfMonth())
                .build();
    }

    public static Date protoDateOf(String x) {
        if (x == null) {
            return null;
        }
        LocalDate date = LocalDate.parse(x);
        return Date.newBuilder()
                .setYear(date.getYear())
                .setMonth(date.getMonthValue())
                .setDay(date.getDayOfMonth())
                .build();
    }


    public static Instant javaInstanceOf(Timestamp x) {
        return x == null ? null : Instant.ofEpochSecond(x.getSeconds(), x.getNanos());
    }

    public static Timestamp protoTimestampOf(Instant x) {
        return x == null ? null : Timestamp.newBuilder()
                .setSeconds(x.getEpochSecond())
                .setNanos(x.getNano())
                .build();
    }

    public static Timestamp protoTimestampOf(String x) {
        try {
            return Timestamps.parse(x);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Message unpack(Any any) {
        String typeName = getTypeName(any.getTypeUrl());
        Descriptors.Descriptor descriptor = JSON_TYPE_REGISTRY.find(typeName);
        if (descriptor == null) {
            throw new IllegalArgumentException("type not found: " + typeName);
        }
        //noinspection unchecked
        return unpack(any, (Class<? extends Message>) findJavaClass(descriptor.getFile(), typeName));
    }

    public static <T extends Message> T unpack(Any any, Class<T> type) {
        try {
            return any.unpack(type);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Class<?> findJavaClass(Descriptors.FileDescriptor file, String fullTypeName) {
        String protobufPackage = file.getPackage();
        String javaSimpleTypeName = fullTypeName.replaceFirst("^" + Pattern.quote(protobufPackage) + "\\.", "").replace('.', '$');

        DescriptorProtos.FileOptions options = file.getOptions();
        String javaPackage = options.getJavaPackage();
        String javaClassName = options.getJavaMultipleFiles() ? javaPackage + "." + javaSimpleTypeName : options.getJavaOuterClassname() + "$" + javaSimpleTypeName;
        try {
            return Class.forName(javaClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private static String getTypeName(String typeUrl) {
        String[] parts = typeUrl.split("/");
        if (parts.length == 1) {
            throw new IllegalArgumentException("Invalid type url found: " + typeUrl);
        }
        return parts[parts.length - 1];
    }

    private static final JsonFormat.TypeRegistry JSON_TYPE_REGISTRY = JsonFormat.TypeRegistry.newBuilder()
            .build();

    private static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer()
            .usingTypeRegistry(JSON_TYPE_REGISTRY)
            .preservingProtoFieldNames();
    private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser()
            .usingTypeRegistry(JSON_TYPE_REGISTRY)
            .ignoringUnknownFields();

    public static String printJson(Message message) {
        try {
            return JSON_PRINTER.print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T extends Message.Builder> T parseJson(String json, T builder) {
        try {
            JSON_PARSER.merge(json, builder);
            return builder;
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setOrClearFieldIfExists(Message.Builder builder, String field, Object value) {
        Descriptors.FieldDescriptor fd = builder.getDescriptorForType().findFieldByName(field);
        if (fd == null) {
            return;
        }
        if (value != null) {
            builder.setField(fd, value);
        } else {
            builder.clearField(fd);
        }
    }
}
