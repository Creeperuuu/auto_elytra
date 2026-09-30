package com.av.autoelytra;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoElytraClient implements ClientModInitializer {
    private static final String MOD_ID = "autoelytra";
    private static final KeyMapping.Category KEY_CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
    private static final KeyMapping SWAP_KEY = KeyMappingHelper.registerKeyMapping(
        new KeyMapping("key.autoelytra.swap", InputConstants.Type.KEYSYM, InputConstants.KEY_R, KEY_CATEGORY)
    );

    private boolean wasJumpDown;
    private int jumpPressCooldown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;
        Player player = client.player;

        if (jumpPressCooldown > 0) jumpPressCooldown--;

        if (player.isFallFlying() && client.options.keyJump.isDown()) {
            swapElytraForChestplate(client, player);
            wasJumpDown = true;
            return;
        }

        while (SWAP_KEY.consumeClick()) {
            swapChestAndElytra(client, player);
        }

        boolean jumpDown = client.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !wasJumpDown;

        if (jumpPressed && !player.onGround() && !player.isFallFlying()
                && jumpPressCooldown == 0
                && isChestplate(player.getItemBySlot(EquipmentSlot.CHEST))) {
            equipBestElytra(client, player);
            jumpPressCooldown = 8;
        }

        wasJumpDown = jumpDown;
    }

    private void swapChestAndElytra(Minecraft client, Player player) {
        if (isElytra(player.getItemBySlot(EquipmentSlot.CHEST))) {
            equipBestChestplate(client, player);
        } else {
            equipBestElytra(client, player);
        }
    }

    private void swapElytraForChestplate(Minecraft client, Player player) {
        if (isElytra(player.getItemBySlot(EquipmentSlot.CHEST))) {
            equipBestChestplate(client, player);
        }
    }

    private void equipBestElytra(Minecraft client, Player player) {
        int slot = findInventorySlot(player, this::isElytra);
        if (slot >= 0) swapInventorySlotWithChest(client, player, slot);
    }

    private void equipBestChestplate(Minecraft client, Player player) {
        int slot = findBestChestplateSlot(player);
        if (slot >= 0) swapInventorySlotWithChest(client, player, slot);
    }

    private int findBestChestplateSlot(Player player) {
        int bestSlot = -1;
        int bestDefense = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isChestplate(stack)) continue;
            int defense = chestplateDefense(stack);
            if (defense > bestDefense) {
                bestDefense = defense;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int findInventorySlot(Player player, java.util.function.Predicate<ItemStack> predicate) {
        for (int i = 0; i < 36; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    private boolean isElytra(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.ELYTRA);
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty() || isElytra(stack)) return false;
        return chestplateDefense(stack) >= 0;
    }

    private int chestplateDefense(ItemStack stack) {
        if (stack.is(Items.LEATHER_CHESTPLATE)) return 3;
        if (stack.is(Items.GOLDEN_CHESTPLATE)) return 5;
        if (stack.is(Items.CHAINMAIL_CHESTPLATE)) return 6;
        if (stack.is(Items.IRON_CHESTPLATE)) return 6;
        if (stack.is(Items.DIAMOND_CHESTPLATE)) return 8;
        if (stack.is(Items.NETHERITE_CHESTPLATE)) return 8;
        return -1;
    }

    private void swapInventorySlotWithChest(Minecraft client, Player player, int inventoryIndex) {
        if (client.gameMode == null) return;

        int sourceMenuSlot = inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
        int chestMenuSlot = 6;
        int syncId = player.containerMenu.containerId;

        client.gameMode.handleContainerInput(syncId, sourceMenuSlot, 0,
            ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(syncId, chestMenuSlot, 0,
            ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(syncId, sourceMenuSlot, 0,
            ContainerInput.PICKUP, player);
    }
}
