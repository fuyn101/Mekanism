package mekanism.additions.common.recipe;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import mekanism.additions.common.AdditionsTags;
import mekanism.additions.common.MekanismAdditions;
import mekanism.additions.common.block.BlockGlowPanel;
import mekanism.additions.common.item.ItemBalloon;
import mekanism.additions.common.registries.AdditionsBlocks;
import mekanism.additions.common.registries.AdditionsItems;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.datagen.recipe.builder.ItemStackChemicalToItemStackRecipeBuilder;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.api.text.EnumColor;
import mekanism.common.block.interfaces.IColoredBlock;
import mekanism.common.recipe.BaseRecipeProvider;
import mekanism.common.recipe.ISubRecipeProvider;
import mekanism.common.recipe.builder.ExtendedShapedRecipeBuilder;
import mekanism.common.recipe.builder.ExtendedShapelessRecipeBuilder;
import mekanism.common.recipe.impl.PigmentExtractingRecipeProvider;
import mekanism.common.recipe.pattern.Pattern;
import mekanism.common.recipe.pattern.RecipePattern;
import mekanism.common.recipe.pattern.RecipePattern.TripleLine;
import mekanism.common.registration.impl.BlockRegistryObject;
import mekanism.common.registration.impl.ItemRegistryObject;
import mekanism.common.registries.MekanismChemicals;
import mekanism.common.registries.MekanismItems;
import mekanism.common.resource.PrimaryResource;
import mekanism.common.resource.ResourceType;
import mekanism.common.tags.MekanismTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

@NothingNullByDefault
public class AdditionsRecipeProvider extends BaseRecipeProvider {

    static final char TNT_CHAR = 'T';
    static final char OBSIDIAN_CHAR = 'O';
    static final char GLASS_PANES_CHAR = 'P';
    static final char PLASTIC_SHEET_CHAR = 'H';
    static final char PLASTIC_ROD_CHAR = 'R';
    static final char SAND_CHAR = 'S';
    static final char SLIME_CHAR = 'S';


    private static final RecipePattern GLOW_PANEL = RecipePattern.createPattern(
          TripleLine.of(GLASS_PANES_CHAR, PLASTIC_SHEET_CHAR, GLASS_PANES_CHAR),
          TripleLine.of(PLASTIC_SHEET_CHAR, Pattern.DYE, PLASTIC_SHEET_CHAR),
          TripleLine.of(Pattern.GLOWSTONE, PLASTIC_SHEET_CHAR, Pattern.GLOWSTONE));

    public AdditionsRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper existingFileHelper) {
        super(output, provider, existingFileHelper);
    }

    @Override
    protected void addRecipes(RecipeOutput consumer, HolderLookup.Provider registries) {
        ExtendedShapedRecipeBuilder.shapedRecipe(AdditionsItems.WALKIE_TALKIE)
              .pattern(RecipePattern.createPattern(
                    TripleLine.of(Pattern.EMPTY, Pattern.EMPTY, Pattern.OSMIUM),
                    TripleLine.of(Pattern.STEEL, Pattern.CIRCUIT, Pattern.STEEL),
                    TripleLine.of(Pattern.EMPTY, Pattern.STEEL, Pattern.EMPTY))
              ).key(Pattern.OSMIUM, MekanismTags.Items.PROCESSED_RESOURCES.get(ResourceType.INGOT, PrimaryResource.OSMIUM))
              .key(Pattern.CIRCUIT, MekanismTags.Items.CIRCUITS_BASIC)
              .key(Pattern.STEEL, MekanismTags.Items.INGOTS_STEEL)
              .build(consumer);
        ExtendedShapedRecipeBuilder.shapedRecipe(AdditionsBlocks.OBSIDIAN_TNT)
              .pattern(RecipePattern.createPattern(
                    TripleLine.of(OBSIDIAN_CHAR, OBSIDIAN_CHAR, OBSIDIAN_CHAR),
                    TripleLine.of(TNT_CHAR, TNT_CHAR, TNT_CHAR),
                    TripleLine.of(OBSIDIAN_CHAR, OBSIDIAN_CHAR, OBSIDIAN_CHAR))
              ).key(OBSIDIAN_CHAR, Tags.Items.OBSIDIANS)
              .key(TNT_CHAR, Blocks.TNT)
              .category(RecipeCategory.REDSTONE)
              .build(consumer);
        registerBalloons(consumer);
        registerGlowPanels(consumer);
    }

    @Override
    protected List<ISubRecipeProvider> getSubRecipeProviders() {
        return List.of(
              new PigmentExtractingPlasticRecipeProvider(),
              new PlasticBlockRecipeProvider(),
              new PlasticFencesRecipeProvider(),
              new PlasticSlabsRecipeProvider(),
              new PlasticStairsRecipeProvider()
        );
    }

    private void registerBalloons(RecipeOutput consumer) {
        for (ItemRegistryObject<ItemBalloon> balloon : AdditionsItems.BALLOONS.values()) {
            registerBalloon(consumer, balloon, "balloon/");
        }
    }

    private void registerBalloon(RecipeOutput consumer, ItemRegistryObject<ItemBalloon> result, String basePath) {
        EnumColor color = result.asItem().getColor();
        String colorString = color.getRegistryPrefix();
        Ingredient recolorInput = difference(AdditionsTags.Items.BALLOONS, result);
        DyeColor dye = color.getDyeColor();
        if (dye != null) {
            ExtendedShapelessRecipeBuilder.shapelessRecipe(result, 2)
                  .addIngredient(Tags.Items.LEATHERS)
                  .addIngredient(Tags.Items.STRINGS)
                  .addIngredient(dye.getTag())
                  .category(RecipeCategory.DECORATIONS)
                  .build(consumer, MekanismAdditions.rl(basePath + colorString));
            ExtendedShapelessRecipeBuilder.shapelessRecipe(result)
                  .addIngredient(recolorInput)
                  .addIngredient(dye.getTag())
                  .category(RecipeCategory.DECORATIONS)
                  .build(consumer, MekanismAdditions.rl(basePath + "recolor/" + colorString));
        }
        ItemStackChemicalToItemStackRecipeBuilder.painting(
              IngredientCreatorAccess.item().from(recolorInput),
              IngredientCreatorAccess.chemicalStack().from(MekanismChemicals.PIGMENT_COLOR_LOOKUP.get(color), PigmentExtractingRecipeProvider.DYE_RATE),
              result.getItemStack(),
              false
        ).build(consumer, MekanismAdditions.rl(basePath + "recolor/painting/" + colorString));
    }

    private void registerGlowPanels(RecipeOutput consumer) {
        for (BlockRegistryObject<BlockGlowPanel, ?> glowPanel : AdditionsBlocks.GLOW_PANELS.values()) {
            registerGlowPanel(consumer, glowPanel, "glow_panel/");
        }
    }

    private void registerGlowPanel(RecipeOutput consumer, BlockRegistryObject<? extends IColoredBlock, ?> result, String basePath) {
        EnumColor color = result.getBlock().getColor();
        DyeColor dye = color.getDyeColor();
        if (dye != null) {
            ExtendedShapedRecipeBuilder.shapedRecipe(result, 2)
                  .pattern(GLOW_PANEL)
                  .key(PLASTIC_SHEET_CHAR, MekanismItems.HDPE_SHEET)
                  .key(GLASS_PANES_CHAR, Tags.Items.GLASS_PANES)
                  .key(Pattern.GLOWSTONE, Tags.Items.DUSTS_GLOWSTONE)
                  .key(Pattern.DYE, dye.getTag())
                  .category(RecipeCategory.BUILDING_BLOCKS)
                  .build(consumer, MekanismAdditions.rl(basePath + color.getRegistryPrefix()));
        }
        PlasticBlockRecipeProvider.registerRecolor(consumer, result, AdditionsTags.Items.GLOW_PANELS, color, basePath);
    }
}