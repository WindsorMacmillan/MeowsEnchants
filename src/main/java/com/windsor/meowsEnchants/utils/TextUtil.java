package com.windsor.meowsEnchants.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * 附魔配置文本解析工具。
 * <p>
 * 同一份配置里既可能使用 Bukkit 传统的 &amp; 颜色代码（含 &amp;#RRGGBB 与 &amp;x 重复字符十六进制格式），
 * 也可能使用 MiniMessage 标签，这里根据内容自动选择解析方式。
 */
public final class TextUtil {

    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)[&\u00A7][0-9a-fk-orx]");

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {
    }

    /**
     * 解析一行配置文本，自动识别 Bukkit 颜色代码与 MiniMessage 标签。
     *
     * @param raw 原始配置文本，允许为 null
     * @return 解析后的组件，解析失败时退回为纯文本组件
     */
    public static @NotNull Component parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String text = raw.indexOf('\u00A7') >= 0 ? raw.replace('\u00A7', '&') : raw;
        try {
            if (LEGACY_CODE.matcher(text).find()) {
                return LEGACY.deserialize(text);
            }
            return MINI_MESSAGE.deserialize(text);
        } catch (RuntimeException e) {
            return Component.text(raw);
        }
    }

    /**
     * 解析文本并取消斜体，用于物品名称与 lore。
     */
    public static @NotNull Component parseFlat(@Nullable String raw) {
        return parse(raw).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * 去掉所有样式后的纯文本，用于排序与比较。
     */
    public static @NotNull String plain(@Nullable String raw) {
        return PlainTextComponentSerializer.plainText().serialize(parse(raw));
    }
}
