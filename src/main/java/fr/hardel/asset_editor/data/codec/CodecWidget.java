package fr.hardel.asset_editor.data.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public sealed interface CodecWidget {

    Codec<CodecWidget> CODEC = Codec.lazyInitialized(
        () -> Type.CODEC.dispatch("type", CodecWidget::type, Type::codec));

    Type type();

    enum Type implements StringRepresentable {
        INTEGER("integer"),
        FLOAT("float"),
        BOOLEAN("boolean"),
        UNIT("unit"),
        STRING("string"),
        IDENTIFIER("identifier"),
        TEXT_COMPONENT("text_component"),
        ENUM("enum"),
        HOLDER("holder"),
        HOLDER_SET("holder_set"),
        TAG("tag"),
        OBJECT("object"),
        LIST("list"),
        MAP("map"),
        DISPATCHED("dispatched"),
        EITHER("either"),
        RAW_JSON("raw_json"),
        REFERENCE("reference");

        public static final Codec<Type> CODEC = StringRepresentable.fromValues(Type::values);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        MapCodec<? extends CodecWidget> codec() {
            return switch (this) {
                case INTEGER -> IntegerWidget.MAP_CODEC;
                case FLOAT -> FloatWidget.MAP_CODEC;
                case BOOLEAN -> BooleanWidget.MAP_CODEC;
                case UNIT -> UnitWidget.MAP_CODEC;
                case STRING -> StringWidget.MAP_CODEC;
                case IDENTIFIER -> IdentifierWidget.MAP_CODEC;
                case TEXT_COMPONENT -> TextCodecWidget.MAP_CODEC;
                case ENUM -> EnumWidget.MAP_CODEC;
                case HOLDER -> HolderWidget.MAP_CODEC;
                case HOLDER_SET -> HolderSetWidget.MAP_CODEC;
                case TAG -> TagWidget.MAP_CODEC;
                case OBJECT -> ObjectWidget.MAP_CODEC;
                case LIST -> ListWidget.MAP_CODEC;
                case MAP -> MapWidget.MAP_CODEC;
                case DISPATCHED -> DispatchedWidget.MAP_CODEC;
                case EITHER -> EitherWidget.MAP_CODEC;
                case RAW_JSON -> RawJsonWidget.MAP_CODEC;
                case REFERENCE -> ReferenceWidget.MAP_CODEC;
            };
        }
    }

    record IntegerWidget(Optional<Integer> min, Optional<Integer> max, Optional<Integer> defaultValue) implements CodecWidget {
        public static final MapCodec<IntegerWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("min").forGetter(IntegerWidget::min),
            Codec.INT.optionalFieldOf("max").forGetter(IntegerWidget::max),
            Codec.INT.optionalFieldOf("default").forGetter(IntegerWidget::defaultValue)
        ).apply(i, IntegerWidget::new));

        @Override
        public Type type() {
            return Type.INTEGER;
        }
    }

    record FloatWidget(Optional<Float> min, Optional<Float> max, Optional<Float> defaultValue) implements CodecWidget {
        public static final MapCodec<FloatWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("min").forGetter(FloatWidget::min),
            Codec.FLOAT.optionalFieldOf("max").forGetter(FloatWidget::max),
            Codec.FLOAT.optionalFieldOf("default").forGetter(FloatWidget::defaultValue)
        ).apply(i, FloatWidget::new));

        @Override
        public Type type() {
            return Type.FLOAT;
        }
    }

    record BooleanWidget(Optional<Boolean> defaultValue) implements CodecWidget {
        public static final MapCodec<BooleanWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("default").forGetter(BooleanWidget::defaultValue)
        ).apply(i, BooleanWidget::new));

        @Override
        public Type type() {
            return Type.BOOLEAN;
        }
    }

    record UnitWidget() implements CodecWidget {
        public static final UnitWidget INSTANCE = new UnitWidget();
        public static final MapCodec<UnitWidget> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public Type type() {
            return Type.UNIT;
        }
    }

    record StringWidget(Optional<Integer> maxLength, Optional<String> defaultValue) implements CodecWidget {
        public static final MapCodec<StringWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("max_length").forGetter(StringWidget::maxLength),
            Codec.STRING.optionalFieldOf("default").forGetter(StringWidget::defaultValue)
        ).apply(i, StringWidget::new));

        @Override
        public Type type() {
            return Type.STRING;
        }
    }

    record IdentifierWidget(Optional<Identifier> defaultValue) implements CodecWidget {
        public static final MapCodec<IdentifierWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.optionalFieldOf("default").forGetter(IdentifierWidget::defaultValue)
        ).apply(i, IdentifierWidget::new));

        @Override
        public Type type() {
            return Type.IDENTIFIER;
        }
    }

    record TextCodecWidget() implements CodecWidget {
        public static final TextCodecWidget INSTANCE = new TextCodecWidget();
        public static final MapCodec<TextCodecWidget> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public Type type() {
            return Type.TEXT_COMPONENT;
        }
    }

    record EnumWidget(List<String> values, Optional<String> defaultValue) implements CodecWidget {
        public static final MapCodec<EnumWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.listOf().fieldOf("values").forGetter(EnumWidget::values),
            Codec.STRING.optionalFieldOf("default").forGetter(EnumWidget::defaultValue)
        ).apply(i, EnumWidget::new));

        @Override
        public Type type() {
            return Type.ENUM;
        }
    }

    record HolderWidget(Identifier registry) implements CodecWidget {
        public static final MapCodec<HolderWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("registry").forGetter(HolderWidget::registry)
        ).apply(i, HolderWidget::new));

        @Override
        public Type type() {
            return Type.HOLDER;
        }
    }

    record HolderSetWidget(Identifier registry) implements CodecWidget {
        public static final MapCodec<HolderSetWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("registry").forGetter(HolderSetWidget::registry)
        ).apply(i, HolderSetWidget::new));

        @Override
        public Type type() {
            return Type.HOLDER_SET;
        }
    }

    record TagWidget(Identifier registry) implements CodecWidget {
        public static final MapCodec<TagWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("registry").forGetter(TagWidget::registry)
        ).apply(i, TagWidget::new));

        @Override
        public Type type() {
            return Type.TAG;
        }
    }

    record Field(String key, CodecWidget widget, boolean optional) {
        public static final Codec<Field> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(Field::key),
            CodecWidget.CODEC.fieldOf("widget").forGetter(Field::widget),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(Field::optional)
        ).apply(i, Field::new));
    }

    record ObjectWidget(List<Field> fields) implements CodecWidget {
        public static final MapCodec<ObjectWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Field.CODEC.listOf().fieldOf("fields").forGetter(ObjectWidget::fields)
        ).apply(i, ObjectWidget::new));

        @Override
        public Type type() {
            return Type.OBJECT;
        }
    }

    record ListWidget(CodecWidget item, Optional<Integer> maxSize) implements CodecWidget {
        public static final MapCodec<ListWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CodecWidget.CODEC.fieldOf("item").forGetter(ListWidget::item),
            Codec.INT.optionalFieldOf("max_size").forGetter(ListWidget::maxSize)
        ).apply(i, ListWidget::new));

        @Override
        public Type type() {
            return Type.LIST;
        }
    }

    record MapWidget(CodecWidget key, CodecWidget value) implements CodecWidget {
        public static final MapCodec<MapWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CodecWidget.CODEC.fieldOf("key").forGetter(MapWidget::key),
            CodecWidget.CODEC.fieldOf("value").forGetter(MapWidget::value)
        ).apply(i, MapWidget::new));

        @Override
        public Type type() {
            return Type.MAP;
        }
    }

    record DispatchedWidget(String discriminator, Map<String, CodecWidget> cases) implements CodecWidget {
        public static final MapCodec<DispatchedWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("discriminator").forGetter(DispatchedWidget::discriminator),
            Codec.unboundedMap(Codec.STRING, CodecWidget.CODEC).fieldOf("cases").forGetter(DispatchedWidget::cases)
        ).apply(i, DispatchedWidget::new));

        @Override
        public Type type() {
            return Type.DISPATCHED;
        }
    }

    record EitherWidget(CodecWidget left, CodecWidget right) implements CodecWidget {
        public static final MapCodec<EitherWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CodecWidget.CODEC.fieldOf("left").forGetter(EitherWidget::left),
            CodecWidget.CODEC.fieldOf("right").forGetter(EitherWidget::right)
        ).apply(i, EitherWidget::new));

        @Override
        public Type type() {
            return Type.EITHER;
        }
    }

    record RawJsonWidget() implements CodecWidget {
        public static final RawJsonWidget INSTANCE = new RawJsonWidget();
        public static final MapCodec<RawJsonWidget> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public Type type() {
            return Type.RAW_JSON;
        }
    }

    record ReferenceWidget(Identifier id) implements CodecWidget {
        public static final MapCodec<ReferenceWidget> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("id").forGetter(ReferenceWidget::id)
        ).apply(i, ReferenceWidget::new));

        @Override
        public Type type() {
            return Type.REFERENCE;
        }
    }
}
