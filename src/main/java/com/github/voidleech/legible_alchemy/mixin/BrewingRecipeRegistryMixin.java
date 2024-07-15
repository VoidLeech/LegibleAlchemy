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

import java.util.HashSet;
import java.util.Set;

@Mixin(BrewingRecipeRegistry.class)
public class BrewingRecipeRegistryMixin {
    private static Set<String> knownBadRecipeClasses = new HashSet<>();
    @WrapMethod(method = "addRecipe(Lnet/minecraftforge/common/brewing/IBrewingRecipe;)Z", remap = false)
    private static boolean la$spotFaultyRecipes(IBrewingRecipe recipe, Operation<Boolean> original){
        if (!BrewingRecipeFixer.didBruteForcing()){ // Don't intercept recipes after we've done brute-forcing, because they'll never get re-added in that case
            if (!(recipe instanceof BrewingRecipe) && !(recipe instanceof VanillaBrewingRecipe)) {
                String badClass = recipe.getClass().descriptorString();
                if (knownBadRecipeClasses.contains(badClass)) {
                    knownBadRecipeClasses.add(badClass);
                    LegibleAlchemy.LOGGER.debug("Lossy brewing recipe in {}", badClass);
                }
                BrewingRecipeFixer.addRecipe(recipe);
                return false;
            }
        }
        return original.call(recipe);
    }

}
