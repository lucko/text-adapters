/*
 * This file is part of text-extras, licensed under the MIT License.
 *
 * Copyright (c) 2018 KyoriPowered
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
package net.kyori.text.adapter.bukkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.kyori.text.Component;
import net.kyori.text.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CraftBukkitAdapter implements Adapter {
  static @Nullable Adapter load() {
    try {
      final CraftReflection reflection = CraftReflection.tryBind();
      return new CraftBukkitAdapter(reflection);
    } catch(final Throwable e) {
      return null;
    }
  }

  private final CraftReflection reflection;
  private final boolean canMakeTitle;

  CraftBukkitAdapter(final CraftReflection reflection) {
    this.reflection = reflection;
    this.canMakeTitle = this.reflection.titlePacketClassAction != null && this.reflection.titlePacketConstructor != null;
  }

  @Override
  public void sendMessage(final List<? extends CommandSender> viewers, final Component component) {
    this.send(viewers, component, this::createMessagePackets);
  }

  List<Object> createMessagePackets(final Component component) {
    try {
      return Collections.singletonList(this.reflection.createMessagePacket(component));
    } catch(final Exception e) {
      throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
    }
  }

  @Override
  public void sendTitle(final List<? extends CommandSender> viewers, final Title title) {
    this.send(viewers, title, this::createTitlePackets);
  }

  List<Object> createTitlePackets(final Title msg) {
    if(this.canMakeTitle) {
      try {
        final List<Object> packets = new ArrayList<>();
        if(msg.shouldClear()) {
          packets.add(this.reflection.createTitleClearPacket());
        }
        if(msg.shouldReset()) {
          packets.add(this.reflection.createTitleResetPacket());
        }
        final Component actionbar = msg.actionbar();
        if(actionbar != null) {
          packets.add(this.reflection.createTitleActionbarPacket(actionbar));
        }
        final Title.Times times = msg.times();
        if(times != null) {
          packets.add(this.reflection.createTitleTimesPacket(times));
        }
        final Component subtitle = msg.subtitle();
        if(subtitle != null) {
          packets.add(this.reflection.createTitleSubtitlePacket(subtitle));
        }
        final Component title = msg.title();
        if(title != null) {
          packets.add(this.reflection.createTitleTitlePacket(title));
        }
        return packets;
      } catch(final Exception e) {
        throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public void sendActionBar(final List<? extends CommandSender> viewers, final Component component) {
    this.send(viewers, component, this::createActionBarPackets);
  }

  List<Object> createActionBarPackets(final Component component) {
    if(this.canMakeTitle) {
      try {
        return Collections.singletonList(this.reflection.createTitleActionbarPacket(component));
      } catch(final Exception e) {
        throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
      }
    } else {
      return this.createMessagePackets(component);
    }
  }

  private <T> void send(final List<? extends CommandSender> viewers, final T component, final Function<T, List<Object>> function) {
    List<Object> packets = null;
    for(final Iterator<? extends CommandSender> iterator = viewers.iterator(); iterator.hasNext(); ) {
      final CommandSender sender = iterator.next();
      if(sender instanceof Player) {
        try {
          final Player player = (Player) sender;
          if(packets == null) {
            packets = function.apply(component);
          }
          this.sendPackets(player, packets);
          iterator.remove();
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  void sendPackets(final Player player, final List<Object> packets) {
    if(packets.isEmpty()) {
      return;
    }
    try {
      final Object connection = this.reflection.getConnection(player);
      for(int i = 0, size = packets.size(); i < size; i++) {
        final Object packet = packets.get(i);
        this.reflection.sendPacket(connection, packet);
      }
    } catch(final Exception e) {
      throw new UnsupportedOperationException("An exception was encountered while sending a packet for a component", e);
    }
  }
}
