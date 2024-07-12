package com.github.voidleech.legible_alchemy.mixin;

import com.github.voidleech.legible_alchemy.LegibleAlchemy;
import com.github.voidleech.legible_alchemy.fixer.BrewingRecipeFixer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.common.brewing.VanillaBrewingRecipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BrewingRecipeRegistry.class)
public class BrewingRecipeRegistryMixin {
    @WrapMethod(method = "addRecipe(Lnet/minecraftforge/common/brewing/IBrewingRecipe;)Z", remap = false)
    private static boolean la$spotFaultyRecipes(IBrewingRecipe recipe, Operation<Boolean> original){
        if (!(recipe instanceof BrewingRecipe) && !(recipe instanceof VanillaBrewingRecipe)) {
            // Loop over items/potions to find recipes
            LegibleAlchemy.LOGGER.debug("Lossy brewing recipe in {}", recipe.getClass());
            BrewingRecipeFixer.addRecipe(recipe);
            return false;
        }
        return original.call(recipe);
    }

}
