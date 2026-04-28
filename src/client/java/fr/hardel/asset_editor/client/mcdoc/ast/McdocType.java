package fr.hardel.asset_editor.client.mcdoc.ast;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public sealed interface McdocType permits
    McdocType.AnyType,
    McdocType.BooleanType,
    McdocType.UnsafeType,
    McdocType.NumericType,
    McdocType.PrimitiveArrayType,
    McdocType.StringType,
    McdocType.LiteralType,
    McdocType.ListType,
    McdocType.TupleType,
    McdocType.StructType,
    McdocType.EnumType,
    McdocType.UnionType,
    McdocType.ReferenceType,
    McdocType.DispatcherType,
    McdocType.IndexedType,
    McdocType.ConcreteType,
    McdocType.TemplateType,
    McdocType.MappedType {

    Attributes attributes();

    McdocType withAttributes(Attributes attributes);

    record AnyType(Attributes attributes) implements McdocType {
        public AnyType() { this(Attributes.EMPTY); }
        @Override public AnyType withAttributes(Attributes attributes) { return new AnyType(attributes); }
    }

    record BooleanType(Attributes attributes) implements McdocType {
        public BooleanType() { this(Attributes.EMPTY); }
        @Override public BooleanType withAttributes(Attributes attributes) { return new BooleanType(attributes); }
    }

    record UnsafeType(Attributes attributes) implements McdocType {
        public UnsafeType() { this(Attributes.EMPTY); }
        @Override public UnsafeType withAttributes(Attributes attributes) { return new UnsafeType(attributes); }
    }

    enum NumericKind { BYTE, SHORT, INT, LONG, FLOAT, DOUBLE;
        public boolean isInteger() { return this == BYTE || this == SHORT || this == INT || this == LONG; }
    }

    record NumericType(NumericKind kind, Optional<NumericRange> valueRange, Attributes attributes) implements McdocType {
        @Override public NumericType withAttributes(Attributes attributes) {
            return new NumericType(kind, valueRange, attributes);
        }
    }

    enum PrimitiveArrayKind { BYTE_ARRAY, INT_ARRAY, LONG_ARRAY;
        public NumericKind elementKind() {
            return switch (this) {
                case BYTE_ARRAY -> NumericKind.BYTE;
                case INT_ARRAY -> NumericKind.INT;
                case LONG_ARRAY -> NumericKind.LONG;
            };
        }
    }

    record PrimitiveArrayType(
        PrimitiveArrayKind kind,
        Optional<NumericRange> valueRange,
        Optional<NumericRange> lengthRange,
        Attributes attributes
    ) implements McdocType {
        @Override public PrimitiveArrayType withAttributes(Attributes attributes) {
            return new PrimitiveArrayType(kind, valueRange, lengthRange, attributes);
        }
    }

    record StringType(Optional<NumericRange> lengthRange, Attributes attributes) implements McdocType {
        @Override public StringType withAttributes(Attributes attributes) {
            return new StringType(lengthRange, attributes);
        }
    }

    sealed interface LiteralValue permits StringLiteral, BooleanLiteral, NumericLiteral {}
    record StringLiteral(String value) implements LiteralValue {}
    record BooleanLiteral(boolean value) implements LiteralValue {}
    record NumericLiteral(NumericKind kind, double value) implements LiteralValue {}

    record LiteralType(LiteralValue value, Attributes attributes) implements McdocType {
        @Override public LiteralType withAttributes(Attributes attributes) {
            return new LiteralType(value, attributes);
        }
    }

    record ListType(McdocType item, Optional<NumericRange> lengthRange, Attributes attributes) implements McdocType {
        @Override public ListType withAttributes(Attributes attributes) {
            return new ListType(item, lengthRange, attributes);
        }
    }

    record TupleType(List<McdocType> items, Attributes attributes) implements McdocType {
        public TupleType { items = List.copyOf(items); }
        @Override public TupleType withAttributes(Attributes attributes) {
            return new TupleType(items, attributes);
        }
    }

    sealed interface StructField permits StructPairField, StructSpreadField {
        Attributes attributes();
    }

    sealed interface StructKey permits StringKey, ComputedKey {}
    record StringKey(String name) implements StructKey {}
    record ComputedKey(McdocType type) implements StructKey {}

    record StructPairField(
        StructKey key,
        McdocType type,
        boolean optional,
        boolean deprecated,
        Optional<String> doc,
        Attributes attributes
    ) implements StructField {}

    record StructSpreadField(McdocType type, Attributes attributes) implements StructField {}

    record StructType(List<StructField> fields, Attributes attributes) implements McdocType {
        public StructType { fields = List.copyOf(fields); }
        @Override public StructType withAttributes(Attributes attributes) {
            return new StructType(fields, attributes);
        }
    }

    enum EnumKind { BYTE, SHORT, INT, LONG, STRING, FLOAT, DOUBLE }

    sealed interface EnumValue permits StringEnumValue, NumericEnumValue {}
    record StringEnumValue(String value) implements EnumValue {}
    record NumericEnumValue(double value) implements EnumValue {}

    record EnumField(
        String identifier,
        EnumValue value,
        Optional<String> doc,
        Attributes attributes
    ) {}

    record EnumType(EnumKind kind, List<EnumField> values, Attributes attributes) implements McdocType {
        public EnumType { values = List.copyOf(values); }
        @Override public EnumType withAttributes(Attributes attributes) {
            return new EnumType(kind, values, attributes);
        }
    }

    record UnionType(List<McdocType> members, Attributes attributes) implements McdocType {
        public UnionType { members = List.copyOf(members); }
        @Override public UnionType withAttributes(Attributes attributes) {
            return new UnionType(members, attributes);
        }
    }

    record ReferenceType(Path path, Attributes attributes) implements McdocType {
        @Override public ReferenceType withAttributes(Attributes attributes) {
            return new ReferenceType(path, attributes);
        }
    }

    sealed interface Index permits StaticIndex, DynamicIndex {}
    record StaticIndex(String value) implements Index {}
    record DynamicIndex(List<DynamicAccessor> accessors) implements Index {
        public DynamicIndex { accessors = List.copyOf(accessors); }
    }

    sealed interface DynamicAccessor permits FieldAccessor, KeywordAccessor {}
    record FieldAccessor(String name) implements DynamicAccessor {}
    record KeywordAccessor(String keyword) implements DynamicAccessor {}

    record DispatcherType(
        String registry,
        List<Index> parallelIndices,
        Attributes attributes
    ) implements McdocType {
        public DispatcherType { parallelIndices = List.copyOf(parallelIndices); }
        @Override public DispatcherType withAttributes(Attributes attributes) {
            return new DispatcherType(registry, parallelIndices, attributes);
        }
    }

    record IndexedType(McdocType child, List<Index> parallelIndices, Attributes attributes) implements McdocType {
        public IndexedType { parallelIndices = List.copyOf(parallelIndices); }
        @Override public IndexedType withAttributes(Attributes attributes) {
            return new IndexedType(child, parallelIndices, attributes);
        }
    }

    record TypeParam(String name) {}

    record TemplateType(McdocType child, List<TypeParam> typeParams, Attributes attributes) implements McdocType {
        public TemplateType { typeParams = List.copyOf(typeParams); }
        @Override public TemplateType withAttributes(Attributes attributes) {
            return new TemplateType(child, typeParams, attributes);
        }
    }

    record ConcreteType(McdocType child, List<McdocType> typeArgs, Attributes attributes) implements McdocType {
        public ConcreteType { typeArgs = List.copyOf(typeArgs); }
        @Override public ConcreteType withAttributes(Attributes attributes) {
            return new ConcreteType(child, typeArgs, attributes);
        }
    }

    record MappedType(McdocType child, Map<String, McdocType> mapping, Attributes attributes) implements McdocType {
        public MappedType { mapping = Map.copyOf(mapping); }
        @Override public MappedType withAttributes(Attributes attributes) {
            return new MappedType(child, mapping, attributes);
        }
    }
}
