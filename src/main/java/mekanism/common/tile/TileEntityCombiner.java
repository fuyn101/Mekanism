package mekanism.common.tile;

import java.util.Map;
import mekanism.common.MekanismBlock;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.recipe.inputs.DoubleMachineInput;
import mekanism.common.recipe.machines.CombinerRecipe;
import mekanism.common.tile.prefab.TileEntityDoubleElectricMachine;

public class TileEntityCombiner extends TileEntityDoubleElectricMachine<CombinerRecipe> {

    public TileEntityCombiner() {
        super("combiner", MekanismBlock.COMBINER, 200);
    }

    @Override
    public Map<DoubleMachineInput, CombinerRecipe> getRecipes() {
        return Recipe.COMBINER.get();
    }
}