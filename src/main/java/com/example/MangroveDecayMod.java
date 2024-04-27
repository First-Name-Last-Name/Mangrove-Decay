package com.example;


import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MangroveDecayMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mangrove-decay");

	public static final int DISTANCE = 41;

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
	}
}