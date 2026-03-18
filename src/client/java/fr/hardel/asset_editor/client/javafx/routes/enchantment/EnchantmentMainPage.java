package fr.hardel.asset_editor.client.javafx.routes.enchantment;

import fr.hardel.asset_editor.client.javafx.components.layout.SupportCard;
import fr.hardel.asset_editor.client.javafx.components.ui.Counter;
import fr.hardel.asset_editor.client.javafx.components.ui.ResponsiveGrid;
import fr.hardel.asset_editor.client.javafx.components.ui.Section;
import fr.hardel.asset_editor.client.javafx.components.ui.Selector;
import fr.hardel.asset_editor.client.javafx.components.ui.TemplateCard;
import fr.hardel.asset_editor.client.javafx.lib.RegistryPage;
import fr.hardel.asset_editor.client.javafx.lib.StudioContext;
import fr.hardel.asset_editor.client.javafx.lib.action.EnchantmentActions;
import fr.hardel.asset_editor.client.javafx.lib.data.StudioBreakpoint;
import fr.hardel.asset_editor.store.ElementEntry;
import fr.hardel.asset_editor.network.workspace.EditorAction;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class EnchantmentMainPage extends RegistryPage<Enchantment> {

    private static final Identifier MAX_LEVEL_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/max_level.svg");
    private static final Identifier WEIGHT_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/weight.svg");
    private static final Identifier ANVIL_COST_ICON = Identifier.fromNamespaceAndPath("asset_editor", "icons/tools/anvil_cost.svg");

    public EnchantmentMainPage(StudioContext context) {
        super(context, Registries.ENCHANTMENT, "editor-main-scroll", 32, new Insets(16, 32, 28, 32));
        ((ScrollPane) getChildren().getFirst()).viewportBoundsProperty()
                .addListener((obs, o, bounds) -> content().setMinHeight(Math.max(0, bounds.getHeight())));
    }

    @Override
    protected void buildContent() {
        Section section = new Section(I18n.get("enchantment:section.global.description"));
        ResponsiveGrid grid = new ResponsiveGrid(ResponsiveGrid.autoFit(256))
                .atMost(StudioBreakpoint.XL, ResponsiveGrid.fixed(1));

        buildCounter(grid, MAX_LEVEL_ICON,
                "enchantment:global.maxLevel.title", "enchantment:global.explanation.list.1",
                1, 127, 1,
                entry -> entry.data().getMaxLevel(),
                EnchantmentActions::maxLevel,
                v -> new EditorAction.SetIntField("max_level", v));

        buildCounter(grid, WEIGHT_ICON,
                "enchantment:global.weight.title", "enchantment:global.explanation.list.2",
                1, 1024, 1,
                entry -> entry.data().getWeight(),
                EnchantmentActions::weight,
                v -> new EditorAction.SetIntField("weight", v));

        buildCounter(grid, ANVIL_COST_ICON,
                "enchantment:global.anvilCost.title", "enchantment:global.explanation.list.3",
                0, 255, 1,
                entry -> entry.data().getAnvilCost(),
                EnchantmentActions::anvilCost,
                v -> new EditorAction.SetIntField("anvil_cost", v));

        section.addContent(grid, buildModeSelector());

        section.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (hasWritablePack()) return;
            e.consume();
            showPackGuard();
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content().getChildren().setAll(section, spacer, new SupportCard());
    }

    private void buildCounter(ResponsiveGrid grid, Identifier icon,
                               String titleKey, String descKey,
                               int min, int max, int step,
                               Function<ElementEntry<Enchantment>, Integer> selectorFn,
                               java.util.function.IntFunction<UnaryOperator<Enchantment>> mutation,
                               java.util.function.IntFunction<EditorAction> actionFactory) {
        Counter counter = new Counter(min, max, step, 0);
        bindInt(counter.valueProperty(), selectorFn, mutation, actionFactory);
        grid.addItem(new TemplateCard(icon, I18n.get(titleKey), I18n.get(descKey), counter));
    }

    private Selector buildModeSelector() {
        LinkedHashMap<String, String> modeOptions = new LinkedHashMap<>();
        modeOptions.put(EnchantmentActions.MODE_NORMAL, I18n.get("enchantment:global.mode.enum.normal"));
        modeOptions.put(EnchantmentActions.MODE_SOFT_DELETE, I18n.get("enchantment:global.mode.enum.soft_delete"));
        modeOptions.put(EnchantmentActions.MODE_ONLY_CREATIVE, I18n.get("enchantment:global.mode.enum.only_creative"));

        Selector selector = new Selector(
                I18n.get("enchantment:global.mode.title"),
                I18n.get("enchantment:global.mode.description"),
                modeOptions,
                EnchantmentActions.MODE_NORMAL,
                null);

        bindString(selector.valueProperty(), EnchantmentActions::mode,
                v -> applyCustomAction(EnchantmentActions.mode(v), new EditorAction.SetMode(v)));

        return selector;
    }
}
