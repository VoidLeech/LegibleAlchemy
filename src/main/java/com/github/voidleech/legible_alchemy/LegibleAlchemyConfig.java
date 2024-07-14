package com.github.voidleech.legible_alchemy;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = LegibleAlchemy.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class LegibleAlchemyConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<Integer> TIME_TO_SLEEP = BUILDER
            .comment("Time (ms) spent sleeping during startup to guarantee LA's event is executed last")
            .comment("If a normally working brewing recipe fails to work when LA is added, try increasing this value first before you make a report")
            .defineInRange("time_to_sleep", 1000, 500, 60000);

    public static int timeToSleep;

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        timeToSleep = TIME_TO_SLEEP.get();
    }
}
