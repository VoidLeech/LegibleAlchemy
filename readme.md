# Legible Alchemy
A mod that tries to fix certain potion recipes not showing up in recipe viewers.

---

Ever noticed how some mods add potions with *only seemingly* no way to brew them when you try to check for a brewing recipe?  
How Create's automated brewing doesn't pick up the input potion or ingredient even after you've figured out the recipe?

Potion recipes (sadly) aren't datapackable. That also means there's no complete standard for what such a recipe looks like.  
In practice in Forge there is one: the BrewingRecipe class, which provides methods for looking up ingredients, but importantly, the only thing that's required to add a recipe is an IBrewingRecipe, which doesn't provide those methods. (There is sense to that, as it allows adding potion-independent recipes (such as normal to splash) without making each one individually.)
And that means that mods that want to use a brewing recipe really want a BrewingRecipe, but sometimes only get an IBrewingRecipe, which is unusable for their purposes.

In comes Legible Alchemy.

### Implementation Details
We use MixinExtras's WrapMethod to intercept any brewing recipes getting added via BrewingRecipeRegistry.addRecipe(IBrewingRecipe). If the IBrewingRecipe is a BrewingRecipe, we let the call go through as normal, otherwise we cache the recipe to deal with later.

In the Common startup events, we want to make sure we enquere our work last, because other mods very well could (should) add their brewing recipes in that queue, and if we do our work first, we don't catch all recipes. This is why we add a configurable delay to startup as we pause our thread.

Finally, we go over all "faulty" recipes, brute-forcing the input and ingredient via the few methods IBrewingRecipes *does* thankfully implement.  
These found input-ingredient pairings then get added as proper BrewingRecipes.  
If we can't find any pairings, that means there's some complex ingredient we don't cover yet and we just add the recipe as-is to not break anything. We log an error in the hope someone will report it.

### What mods does this effect?
There's one main contributor to the proliferation of these recipes: MCreator. Someone else has made a [plugin](https://mcreator.net/plugin/103347/just-enough-recipes) that fixes this on the dev end of an MCreator mod, but that of course doesn't catch mods not using the plugin.  
There's also some non-MCreator mods that have such recipes: Ecologics, Naturalist (probably not an exhaustive list)

### F.A.Q.
**Q: My potion only sometimes has a shown brewing recipe.**  
A: Increase time_to_sleep in our config `legible_alchemy-common.toml`. We've intercepted the recipe but did our brute-forcing before that. You'll need to restart your game.

**Q: My potion never has a recipe, even with time_to_sleep obscenely high.**  
A: Please make an issue for the mod. We can look at the recipe it adds in a dev environment to see what's required and adjust our brute-forcing to catch that too. Some mods also legitimately don't have recipes for some of their potions.

**Q: My potion has a recipe that is brewable without Legible Alchemy but isn't without.**  
A: Please make an issue for the affected mod. We can look at the recipe it adds in a dev environment to see what's required and adjust our brute-forcing to catch that too. Their recipe class adds multiple recipes, at least one of which we catch and another one we miss.

**Q: Will you be porting to Loader/X.x.x?**  
A: Yes to 1.20.4 and 1.21 (and future later releases), provided the issue is present on the loader. Past 1.20.1, I'll support Neoforged instead of Forge. I will check whether this is still a possible issue on Neoforged, as those are based on Forge (and the former currently has MCreator support). I'm not too familiar with Fabric, but I don't think it supports item inputs, only potions, and so this issue shouldn't exist on Fabric to begin with.

**Q: Can this be used in my modpack?**
A: Yes.