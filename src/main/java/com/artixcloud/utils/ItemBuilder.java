package com.artixcloud.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder fluente para criação de ItemStacks customizados.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacySection()
                    .deserialize(MessageUtil.color(name)));
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            for (String line : lines) {
                lore.add(LegacyComponentSerializer.legacySection()
                        .deserialize(MessageUtil.color(line)));
            }
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        return lore(lines.toArray(new String[0]));
    }

    public ItemBuilder addLore(String... lines) {
        if (meta != null) {
            List<Component> current = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            for (String line : lines) {
                current.add(LegacyComponentSerializer.legacySection()
                        .deserialize(MessageUtil.color(line)));
            }
            meta.lore(current);
        }
        return this;
    }

    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        if (meta != null) meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideAll() {
        return flags(ItemFlag.values());
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }

    // ── Itens estáticos utilitários ───────────────────────────────────────────

    /** Item de espaçador transparente (vidro cinza sem nome). */
    public static ItemStack filler() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .name("&r")
                .hideAll()
                .build();
    }

    /** Item de espaçador preto. */
    public static ItemStack fillerBlack() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .name("&r")
                .hideAll()
                .build();
    }

    /** Botão de voltar. */
    public static ItemStack backButton() {
        return new ItemBuilder(Material.ARROW)
                .name("&c&lVoltar")
                .lore("&7Clique para voltar ao menu anterior")
                .build();
    }

    /** Botão de fechar. */
    public static ItemStack closeButton() {
        return new ItemBuilder(Material.BARRIER)
                .name("&c&lFechar")
                .lore("&7Clique para fechar o menu")
                .build();
    }

    /** Botão de confirmar. */
    public static ItemStack confirmButton(String label) {
        return new ItemBuilder(Material.LIME_CONCRETE)
                .name("&a&l" + label)
                .glow()
                .build();
    }

    /** Botão de cancelar. */
    public static ItemStack cancelButton() {
        return new ItemBuilder(Material.RED_CONCRETE)
                .name("&c&lCancelar")
                .build();
    }
}
