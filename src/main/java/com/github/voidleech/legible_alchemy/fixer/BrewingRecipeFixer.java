package com.github.voidleech.legible_alchemy.fixer;

import com.github.voidleech.legible_alchemy.LegibleAlchemy;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = LegibleAlchemy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrewingRecipeFixer {
    private static final Set<IBrewingRecipe> FAULTY_RECIPES = Collections.synchronizedSet(new HashSet<>());
    private static final List<Ingredient> POSSIBLE_INGREDIENTS = new ArrayList<>();

    public static void addRecipe(IBrewingRecipe recipe){
        FAULTY_RECIPES.add(recipe);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fixRecipes(FMLCommonSetupEvent event) throws InterruptedException {
        LegibleAlchemy.LOGGER.info("Taking a nap because EventPriority seemingly cannot be trusted");
        Thread.sleep(4000);
        LegibleAlchemy.LOGGER.info("Done sleeping");
        event.enqueueWork(() -> {
            if (FAULTY_RECIPES.isEmpty()) {
                LegibleAlchemy.LOGGER.debug("No brewing recipes to fix");
                return;
            }
            LegibleAlchemy.LOGGER.debug("Attempting to find recipes for {} faulty brewing recipes", FAULTY_RECIPES.size());
            LegibleAlchemy.LOGGER.debug("Item Registry contains {} items", ForgeRegistries.ITEMS.getValues().size());
            for (Item item : ForgeRegistries.ITEMS.getValues()){
                POSSIBLE_INGREDIENTS.add(Ingredient.of(item));
            }
            LegibleAlchemy.LOGGER.debug("Potion Registry contains {} potion", ForgeRegistries.POTIONS.getValues().size());
            for (Potion potion : ForgeRegistries.POTIONS.getValues()){
                POSSIBLE_INGREDIENTS.add(Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), potion)));
                POSSIBLE_INGREDIENTS.add(Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), potion)));
                POSSIBLE_INGREDIENTS.add(Ingredient.of(PotionUtils.setPotion(Items.LINGERING_POTION.getDefaultInstance(), potion)));
            }
            final Set<Tuple<IBrewingRecipe, Tuple<Ingredient, Ingredient>>> validIngredientPairings = new HashSet<>();
            for (IBrewingRecipe recipe : FAULTY_RECIPES){
                tryFindRecipe(validIngredientPairings, recipe);
            }
            for (Tuple<IBrewingRecipe, Tuple<Ingredient, Ingredient>> tuple : validIngredientPairings){
                Ingredient input = tuple.getB().getA();
                Ingredient ingredient = tuple.getB().getB();
                BrewingRecipe correctedRecipe = new BrewingRecipe(input, ingredient, tuple.getA().getOutput(input.getItems()[0], ingredient.getItems()[0]));
                BrewingRecipeRegistry.addRecipe(correctedRecipe);
            }
        });
    }

    private static void tryFindRecipe(Set<Tuple<IBrewingRecipe, Tuple<Ingredient, Ingredient>>> validIngredientPairings, IBrewingRecipe recipe) {
        int old = validIngredientPairings.size();
        for (Ingredient input : POSSIBLE_INGREDIENTS){
            ItemStack[] inputItems = input.getItems();
            if (inputItems.length == 0){
                LegibleAlchemy.LOGGER.warn("Input {} was empty", input);
                continue;
            }
            if (recipe.isInput(inputItems[0])){
                for (Ingredient ingredient : POSSIBLE_INGREDIENTS){
                    ItemStack[] ingredientItems = ingredient.getItems();
                    if (ingredientItems.length == 0){
                        LegibleAlchemy.LOGGER.warn("Ingredient {} was empty", input);
                        continue;
                    }
                    if (recipe.isIngredient(ingredientItems[0])){
                        validIngredientPairings.add(new Tuple<>(recipe, new Tuple<>(input, ingredient)));
                    }
                }
            }
        }
        if (validIngredientPairings.size() != old){
            LegibleAlchemy.LOGGER.info("Found {} brewing recipes", validIngredientPairings.size() - old);
        }
        else{
            LegibleAlchemy.LOGGER.info("Didn't find brewing recipe");
        }
    }
}
