/*
 * This file is part of HexNicks, licensed under the MIT License.
 *
 * Copyright (c) 2020-2021 Majekdor
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.majek.hexnicks.command;

import dev.majek.hexnicks.Nicks;
import dev.majek.hexnicks.api.SetNickEvent;
import dev.majek.hexnicks.config.NicksMessages;
import dev.majek.hexnicks.util.MiniMessageWrapper;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles <code>/nick</code> command execution and tab completion.
 */
public class CommandNick implements TabExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {
    // Console cannot have a nickname
    if (!(sender instanceof Player)) {
      NicksMessages.INVALID_SENDER.send(sender);
      return true;
    }
    Player player = (Player) sender;

    if (args.length == 0) {
      return false;
    }

    String nickInput = String.join(" ", args);

    Component nickname = MiniMessageWrapper.builder()
        .gradients(player.hasPermission("hexnicks.color.gradient"))
        .hexColors(player.hasPermission("hexnicks.color.hex"))
        .standardColors(true)
        .legacyColors(Nicks.config().LEGACY_COLORS)
        .removeTextDecorations(Nicks.config().DISABLED_DECORATIONS.toArray(new TextDecoration[0]))
        .removeColors(Nicks.utils().blockedColors(player).toArray(new NamedTextColor[0]))
        .build().mmParse(nickInput);

    // Make sure the nickname is alphanumeric if that's enabled
    String plainTextNick = PlainTextComponentSerializer.plainText().serialize(nickname);
    if (Nicks.config().REQUIRE_ALPHANUMERIC) {
      if (!plainTextNick.matches("[a-zA-Z0-9]+")) {
        NicksMessages.NON_ALPHANUMERIC.send(player);
        return true;
      }
    }

    // Set the nickname to the default color if there's no color specified
    nickname = nickname.colorIfAbsent(Nicks.config().DEFAULT_NICK_COLOR);

    // Make sure the nickname isn't too short
    int minLength = Nicks.config().MIN_LENGTH;
    if (plainTextNick.length() < minLength) {
      NicksMessages.TOO_SHORT.send(player, minLength);
      return true;
    }

    // Make sure the nickname isn't too long
    int maxLength = Nicks.config().MAX_LENGTH;
    if (plainTextNick.length() > maxLength) {
      NicksMessages.TOO_LONG.send(player, maxLength);
      return true;
    }

    // Make sure the nickname isn't taken
    if (Nicks.utils().preventDuplicates(nickname, player)) {
      return true;
    }

    // Call event
    SetNickEvent nickEvent = new SetNickEvent(player, nickname,
        Nicks.core().getDisplayName(player));
    Nicks.api().callEvent(nickEvent);
    if (nickEvent.isCancelled()) {
      return true;
    }

    // Set nick
    Nicks.core().setNick(player, nickEvent.newNick());
    NicksMessages.NICKNAME_SET.send(player, nickEvent.newNick());

    return true;
  }

  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String label, @NotNull String[] args) {
    return Collections.emptyList();
  }
}
