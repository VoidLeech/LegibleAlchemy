package com.github.voidleech.legible_alchemy.fixer;

import com.github.voidleech.legible_alchemy.LegibleAlchemy;
import com.github.voidleech.legible_alchemy.LegibleAlchemyConfig;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.BrewingRecipe;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = LegibleAlchemy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BrewingRecipeFixer {
    // Adding recipes *should* only be done in a thread-safe, but you never know what someone else does
    private static final Set<IBrewingRecipe> FAULTY_RECIPES = Collections.synchronizedSet(new HashSet<>());
    private static final List<Ingredient> POSSIBLE_INGREDIENTS = new ArrayList<>();
    private static final List<Item> SKIPPED_ITEMS = new ArrayList<>();

    private static boolean DID_BRUTE_FORCING = false;

    static {
        SKIPPED_ITEMS.addAll(List.of(new Item[]{Items.AIR, Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION}));
    }

    public static boolean didBruteForcing(){
        return DID_BRUTE_FORCING;
    }

    public static void addRecipe(IBrewingRecipe recipe){
        FAULTY_RECIPES.add(recipe);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fixRecipes(FMLCommonSetupEvent event) throws InterruptedException {
        LegibleAlchemy.LOGGER.info("Taking a nap because EventPriority cannot be trusted");
        Thread.sleep(LegibleAlchemyConfig.timeToSleep);
        LegibleAlchemy.LOGGER.info("Done sleeping");
        //skipModItems();
        for (Item item : ForgeRegistries.ITEMS.getValues()){
            if (SKIPPED_ITEMS.contains(item)){ continue; }
            POSSIBLE_INGREDIENTS.add(Ingredient.of(item));
        }
        for (Potion potion : ForgeRegistries.POTIONS.getValues()){
            if (potion == Potions.EMPTY) { continue; }
            POSSIBLE_INGREDIENTS.add(Ingredient.of(PotionUtils.setPotion(Items.POTION.getDefaultInstance(), potion)));
            POSSIBLE_INGREDIENTS.add(Ingredient.of(PotionUtils.setPotion(Items.SPLASH_POTION.getDefaultInstance(), potion)));
            POSSIBLE_INGREDIENTS.add(Ingredient.of(PotionUtils.setPotion(Items.LINGERING_POTION.getDefaultInstance(), potion)));
        }
        event.enqueueWork(() -> {
            LegibleAlchemy.LOGGER.info("Attempting to find recipes for {} faulty brewing recipes", FAULTY_RECIPES.size());
            final Set<Tuple<IBrewingRecipe, Tuple<Ingredient, Ingredient>>> validIngredientPairings = new HashSet<>();
            for (IBrewingRecipe recipe : FAULTY_RECIPES){
                if (!tryFindRecipe(validIngredientPairings, recipe)){
                    // Couldn't brute force the recipe, let it through to not break anything
                    BrewingRecipeRegistry.addRecipe(recipe);
                }
            }
            DID_BRUTE_FORCING = true;
            for (Tuple<IBrewingRecipe, Tuple<Ingredient, Ingredient>> tuple : validIngredientPairings){
                Ingredient input = tuple.getB().getA();
                Ingredient ingredient = tuple.getB().getB();
                BrewingRecipe correctedRecipe = new BrewingRecipe(input, ingredient, tuple.getA().getOutput(input.getItems()[0], ingredient.getItems()[0]));
                BrewingRecipeRegistry.addRecipe(correctedRecipe);
            }
        });
    }

    /**
     * For adding `if isModInstalled then call compat method` (that must exist in a separate file)
     * to add more items to skip in brute-forcing (such as TiC potion buckets that make no sense to have without potion nbt)
     */
    private static void skipModItems() {
        List<Item> modItemsToSkip = new ArrayList<>();
        SKIPPED_ITEMS.addAll(modItemsToSkip);
    }

    /**
     * @param validIngredientPairings the set to insert brute-forced recipes into
     * @param recipe the recipe to brute force recipes for
     * @return whether any valid recipe(s) was/were found. It's possible not all recipes were brute-forced due to nonstandard ingredient types
     */
    private static boolean tryFindRecipe(Set<Tuple<IBrewingRecipe, Tuple<Ingredient, Ingredient>>> validIngredientPairings, IBrewingRecipe recipe) {
        int old = validIngredientPairings.size();
        LegibleAlchemy.LOGGER.info("Attempting to find recipe for {}", recipe.getClass().descriptorString());
        for (Ingredient input : POSSIBLE_INGREDIENTS){
            ItemStack[] inputItems = input.getItems();
            if (recipe.isInput(inputItems[0])){
                for (Ingredient ingredient : POSSIBLE_INGREDIENTS){
                    ItemStack[] ingredientItems = ingredient.getItems();
                    if (recipe.isIngredient(ingredientItems[0])){
                        validIngredientPairings.add(new Tuple<>(recipe, new Tuple<>(input, ingredient)));
                    }
                }
            }
        }
        if (validIngredientPairings.size() != old){
            LegibleAlchemy.LOGGER.info("Found {} brewing recipes", validIngredientPairings.size() - old);
            return true;
        }
        LegibleAlchemy.LOGGER.error("Couldn't find brewing recipe for {}. Report to Legible Alchemy", recipe.getClass().descriptorString());
        return false;
    }
}
