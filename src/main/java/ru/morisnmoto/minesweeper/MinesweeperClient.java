package ru.morisnmoto.minesweeper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MinesweeperClient implements ClientModInitializer {
    public static final String MOD_ID = "minesweeper_client";

    private static KeyBinding openMinesweeperKey;

    @Override
    public void onInitializeClient() {
        openMinesweeperKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON, // Physical key: ; on US layout, Ж on Russian layout.
                "key.category." + MOD_ID
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMinesweeperKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MinesweeperScreen());
                }
            }
        });
    }
}
