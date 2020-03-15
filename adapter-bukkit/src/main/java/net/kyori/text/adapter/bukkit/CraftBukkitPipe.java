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

import com.google.gson.JsonDeserializer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CraftBukkitPipe implements ReflectionPipe {
  private static final BiFunction<Reflection, Component, List<Object>> CREATE_MESSAGE_PACKETS = (reflection, type) -> {
    try {
      return Collections.singletonList(reflection.createMessagePacket(type));
    } catch(final Exception e) {
      throw new UnsupportedOperationException("An exception was encountered while creating a packet for a message", e);
    }
  };
  private static final BiFunction<Reflection, Component, List<Object>> CREATE_ACTION_BAR_PACKETS = (reflection, type) -> {
    try {
      return Collections.singletonList(reflection.createTitleActionbarPacket(type));
    } catch(final Exception e) {
      throw new UnsupportedOperationException("An exception was encountered while creating a packet for an action bar", e);
    }
  };
  private static final BiFunction<Reflection, Title, List<Object>> CREATE_TITLE_PACKETS = (reflection, type) -> {
    try {
      final List<Object> packets = new ArrayList<>();
      if(type.shouldClear()) {
        packets.add(reflection.createTitleClearPacket());
      }
      if(type.shouldReset()) {
        packets.add(reflection.createTitleResetPacket());
      }
      final Title.Times times = type.times();
      if(times != null) {
        packets.add(reflection.createTitleTimesPacket(times));
      }
      final Component subtitle = type.subtitle();
      if(subtitle != null) {
        packets.add(reflection.createTitleSubtitlePacket(subtitle));
      }
      final Component title = type.title();
      if(title != null) {
        packets.add(reflection.createTitleTitlePacket(title));
      }
      return packets;
    } catch(final Exception e) {
      throw new UnsupportedOperationException("An exception was encountered while creating a packet for a component", e);
    }
  };
  private final Reflection reflection;

  static @Nullable Pipe create() {
    try {
      final Reflection reflection = createReflection();
      return new CraftBukkitPipe(reflection);
    } catch(final Throwable e) {
      return null;
    }
  }

  static Reflection createReflection() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
    final Class<?> server = Bukkit.getServer().getClass();
    if(!Reflection.isCompatibleServer(server)) {
      throw new UnsupportedOperationException("Incompatible server version");
    }
    final String serverVersion = Reflection.maybeVersion(server.getPackage().getName().substring("org.bukkit.craftbukkit".length()));
    final Class<?> craftPlayerClass = Reflection.craftBukkitClass(serverVersion, "entity.CraftPlayer");
    final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
    final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
    final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
    final Class<?> playerConnectionClass = playerConnectionField.getType();
    final Class<?> packetClass = Reflection.minecraftClass(serverVersion, "Packet");
    final Method sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
    final Class<?> baseComponentClass = Reflection.minecraftClass(serverVersion, "IChatBaseComponent");
    final Class<?> chatPacketClass = Reflection.minecraftClass(serverVersion, "PacketPlayOutChat");
    final Constructor<?> chatPacketConstructor = chatPacketClass.getConstructor(baseComponentClass);
    final Class<?> titlePacketClass = Reflection.optionalMinecraftClass(serverVersion, "PacketPlayOutTitle");
    final Class<? extends Enum<?>> titlePacketClassAction;
    final Constructor<?> titlePacketConstructor;
    final Constructor<?> titlePacketConstructorTimes;
    if(titlePacketClass != null) {
      titlePacketClassAction = (Class<? extends Enum<?>>) Reflection.minecraftClass(serverVersion, "PacketPlayOutTitle$EnumTitleAction");
      titlePacketConstructor = titlePacketClass.getConstructor(titlePacketClassAction, baseComponentClass);
      titlePacketConstructorTimes = titlePacketClass.getConstructor(int.class, int.class, int.class);
    } else {
      titlePacketClassAction = null;
      titlePacketConstructor = null;
      titlePacketConstructorTimes = null;
    }
    final Class<?> chatSerializerClass = Arrays.stream(baseComponentClass.getClasses())
      .filter(JsonDeserializer.class::isAssignableFrom)
      .findAny()
      // fallback to the 1.7 class?
      .orElseGet(() -> {
        try {
          return Reflection.minecraftClass(serverVersion, "ChatSerializer");
        } catch(final ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      });
    final Method serializeMethod = Arrays.stream(chatSerializerClass.getMethods())
      .filter(m -> Modifier.isStatic(m.getModifiers()))
      .filter(m -> m.getReturnType().equals(baseComponentClass))
      .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(String.class))
      .min(Comparator.comparing(Method::getName)) // prefer the #a method
      .orElseThrow(() -> new RuntimeException("Unable to find serialize method"));
    return new Reflection(
      getHandleMethod, playerConnectionField, sendPacketMethod,
      chatPacketConstructor, serializeMethod,
      titlePacketClassAction, titlePacketConstructor, titlePacketConstructorTimes
    );
  }

  CraftBukkitPipe(final Reflection reflection) {
    this.reflection = reflection;
  }

  @Override
  public void message(final @NonNull Component type, final @NonNull List<? extends CommandSender> viewers) {
    this.send(type, viewers, CREATE_MESSAGE_PACKETS);
  }

  @Override
  public void actionBar(final @NonNull Component type, final @NonNull List<? extends CommandSender> viewers) {
    this.send(type, viewers, CREATE_ACTION_BAR_PACKETS);
  }

  @Override
  public void title(final @NonNull Title type, final @NonNull List<? extends CommandSender> viewers) {
    this.send(type, viewers, CREATE_TITLE_PACKETS);
  }

  private <T> void send(final T type, final List<? extends CommandSender> viewers, final BiFunction<Reflection, T, List<Object>> function) {
    this.send(viewers.iterator(), type, function);
  }

  private <T> void send(final Iterator<? extends CommandSender> viewers, final T type, final BiFunction<Reflection, T, List<Object>> function) {
    List<Object> packets = null;
    while(viewers.hasNext()) {
      final CommandSender viewer = viewers.next();
      if(viewer instanceof Player) {
        try {
          final Player player = (Player) viewer;
          if(packets == null) {
            packets = function.apply(this.reflection, type);
          }
          this.sendPackets(player, packets);
          viewers.remove();
        } catch(final Exception e) {
          e.printStackTrace(); // TODO
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
        if(packet != null) {
          this.reflection.sendPacket(connection, packet);
        }
      }
    } catch(final Exception e) {
      throw new UnsupportedOperationException("An exception was encountered while sending a packet for a component", e);
    }
  }

  static class Reflection {
    final Method getHandleMethod;
    final Field playerConnectionField;
    final Method sendPacketMethod;

    final Constructor<?> chatPacketConstructor;
    final Method serializeMethod;

    @SuppressWarnings("rawtypes")
    final Class<? extends Enum> titlePacketClassAction;
    final Constructor<?> titlePacketConstructor;
    final Constructor<?> titlePacketConstructorTimes;

    static boolean isCompatibleServer(final Class<?> serverClass) {
      return serverClass.getPackage().getName().startsWith("org.bukkit.craftbukkit")
        && serverClass.getSimpleName().equals("CraftServer");
    }

    static String maybeVersion(final String version) {
      if(version.isEmpty()) {
        return "";
      } else if(version.charAt(0) == '.') {
        return version.substring(1) + '.';
      }
      throw new IllegalArgumentException("Unknown version " + version);
    }

    static Class<?> craftBukkitClass(final String version, final String name) throws ClassNotFoundException {
      return Class.forName("org.bukkit.craftbukkit." + version + name);
    }

    static Class<?> minecraftClass(final String version, final String name) throws ClassNotFoundException {
      return Class.forName("net.minecraft.server." + version + name);
    }

    static Class<?> optionalMinecraftClass(final String version, final String name) {
      try {
        return minecraftClass(version, name);
      } catch(final ClassNotFoundException e) {
        return null;
      }
    }

    Reflection(final Method getHandleMethod, final Field playerConnectionField, final Method sendPacketMethod, final Constructor<?> chatPacketConstructor, final Method serializeMethod, final Class<? extends Enum<?>> titlePacketClassAction, final Constructor<?> titlePacketConstructor, final Constructor<?> titlePacketConstructorTimes) {
      this.getHandleMethod = getHandleMethod;
      this.playerConnectionField = playerConnectionField;
      this.sendPacketMethod = sendPacketMethod;
      this.chatPacketConstructor = chatPacketConstructor;
      this.serializeMethod = serializeMethod;
      this.titlePacketClassAction = titlePacketClassAction;
      this.titlePacketConstructor = titlePacketConstructor;
      this.titlePacketConstructorTimes = titlePacketConstructorTimes;
    }

    Object getConnection(final Player player) throws IllegalAccessException, InvocationTargetException {
      return this.playerConnectionField.get(this.getHandleMethod.invoke(player));
    }

    void sendPacket(final Object connection, final Object packet) throws IllegalAccessException, InvocationTargetException {
      this.sendPacketMethod.invoke(connection, packet);
    }

    // Messages

    Object createMessagePacket(final Component component) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      return this.chatPacketConstructor.newInstance(this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(component)));
    }

    // Titles

    @SuppressWarnings("rawtypes")
    @Nullable Object createTitleClearPacket() throws IllegalAccessException, InstantiationException, InvocationTargetException {
      final Enum type = this.titleType("CLEAR", 4);
      return type == null ? null : this.titlePacketConstructor.newInstance(type, null);
    }

    @SuppressWarnings("rawtypes")
    @Nullable Object createTitleResetPacket() throws IllegalAccessException, InstantiationException, InvocationTargetException {
      final Enum type = this.titleType("RESET", 5);
      return type == null ? null : this.titlePacketConstructor.newInstance(type, null);
    }

    @SuppressWarnings("rawtypes")
    @Nullable Object createTitleTitlePacket(final Component title) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      final Enum type = this.titleType("TITLE", 0);
      return type == null ? null : this.titlePacketConstructor.newInstance(type, this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(title)));
    }

    @SuppressWarnings("rawtypes")
    @Nullable Object createTitleSubtitlePacket(final Component title) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      final Enum type = this.titleType("SUBTITLE", 1);
      return type == null ? null : this.titlePacketConstructor.newInstance(type, this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(title)));
    }

    @SuppressWarnings("rawtypes")
    @Nullable Object createTitleActionbarPacket(final Component title) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      final Enum type = this.titleType("ACTIONBAR", 2);
      return type == null ? null : this.titlePacketConstructor.newInstance(type, this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(title)));
    }

    Object createTitleTimesPacket(final Title.Times times) throws IllegalAccessException, InstantiationException, InvocationTargetException {
      return this.titlePacketConstructorTimes.newInstance(times.fadeIn(), times.stay(), times.fadeOut());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Enum titleType(final String name, final int ordinal) {
      try {
        return Enum.valueOf(this.titlePacketClassAction, name);
      } catch(final IllegalArgumentException iae) {
        try {
          return this.titlePacketClassAction.getEnumConstants()[ordinal];
        } catch(final ArrayIndexOutOfBoundsException aioobe) {
          return null;
        }
      }
    }
  }
}
