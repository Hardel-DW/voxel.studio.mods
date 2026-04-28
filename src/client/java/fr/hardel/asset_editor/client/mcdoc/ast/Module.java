package fr.hardel.asset_editor.client.mcdoc.ast;

import java.util.List;
import java.util.Optional;

public record Module(Path path, List<TopLevelStatement> statements) {

    public Module {
        statements = List.copyOf(statements);
    }

    public sealed interface TopLevelStatement permits
        UseStatement,
        DispatchStatement,
        StructStatement,
        EnumStatement,
        TypeAliasStatement,
        InjectStatement {}

    public record UseStatement(Path path, Optional<String> alias) implements TopLevelStatement {}

    public record DispatchStatement(
        String registry,
        List<McdocType.Index> parallelIndices,
        List<McdocType.TypeParam> typeParams,
        McdocType target,
        Attributes attributes
    ) implements TopLevelStatement {
        public DispatchStatement {
            parallelIndices = List.copyOf(parallelIndices);
            typeParams = List.copyOf(typeParams);
        }
    }

    public record StructStatement(
        Optional<String> name,
        McdocType.StructType type,
        Optional<String> doc,
        Attributes attributes
    ) implements TopLevelStatement {}

    public record EnumStatement(
        String name,
        McdocType.EnumType type,
        Optional<String> doc,
        Attributes attributes
    ) implements TopLevelStatement {}

    public record TypeAliasStatement(
        String name,
        List<McdocType.TypeParam> typeParams,
        McdocType target,
        Optional<String> doc,
        Attributes attributes
    ) implements TopLevelStatement {
        public TypeAliasStatement { typeParams = List.copyOf(typeParams); }
    }

    public sealed interface InjectTarget permits StructInjectTarget, EnumInjectTarget {}

    public record StructInjectTarget(Path path, List<McdocType.StructField> fields) implements InjectTarget {
        public StructInjectTarget { fields = List.copyOf(fields); }
    }

    public record EnumInjectTarget(McdocType.EnumKind kind, Path path, List<McdocType.EnumField> fields) implements InjectTarget {
        public EnumInjectTarget { fields = List.copyOf(fields); }
    }

    public record InjectStatement(InjectTarget target) implements TopLevelStatement {}
}
