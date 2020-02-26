package net.kyori.text.adapter.bukkit;

import com.google.gson.JsonDeserializer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

final class CraftReflection extends AbstractCraftReflection {
  @SuppressWarnings("unchecked")
  static CraftReflection tryBind() throws ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
    final Class<?> server = Bukkit.getServer().getClass();
    if(!isCompatibleServer(server)) {
      throw new UnsupportedOperationException("Incompatible server version");
    }
    final String serverVersion = maybeVersion(server.getPackage().getName().substring("org.bukkit.craftbukkit".length()));
    final Class<?> craftPlayerClass = craftBukkitClass(serverVersion, "entity.CraftPlayer");
    final Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
    final Class<?> entityPlayerClass = getHandleMethod.getReturnType();
    final Field playerConnectionField = entityPlayerClass.getField("playerConnection");
    final Class<?> playerConnectionClass = playerConnectionField.getType();
    final Class<?> packetClass = minecraftClass(serverVersion, "Packet");
    final Method sendPacketMethod = playerConnectionClass.getMethod("sendPacket", packetClass);
    final Class<?> baseComponentClass = minecraftClass(serverVersion, "IChatBaseComponent");
    final Class<?> chatPacketClass = minecraftClass(serverVersion, "PacketPlayOutChat");
    final Constructor<?> chatPacketConstructor = chatPacketClass.getConstructor(baseComponentClass);
    final Class<?> titlePacketClass = optionalMinecraftClass(serverVersion, "PacketPlayOutTitle");
    final Class<? extends Enum<?>> titlePacketClassAction;
    final Constructor<?> titlePacketConstructor;
    final Constructor<?> titlePacketConstructorTimes;
    if(titlePacketClass != null) {
      titlePacketClassAction = (Class<? extends Enum<?>>) minecraftClass(serverVersion, "PacketPlayOutTitle$EnumTitleAction");
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
          return minecraftClass(serverVersion, "ChatSerializer");
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
    return new CraftReflection(
      getHandleMethod, playerConnectionField, sendPacketMethod,
      chatPacketConstructor, serializeMethod,
      titlePacketClassAction, titlePacketConstructor, titlePacketConstructorTimes
    );
  }

  final Method getHandleMethod;
  final Field playerConnectionField;
  final Method sendPacketMethod;

  final Constructor<?> chatPacketConstructor;
  final Method serializeMethod;

  @SuppressWarnings("rawtypes")
  final Class<? extends Enum> titlePacketClassAction;
  final Constructor<?> titlePacketConstructor;
  final Constructor<?> titlePacketConstructorTimes;

  CraftReflection(final Method getHandleMethod, final Field playerConnectionField, final Method sendPacketMethod, final Constructor<?> chatPacketConstructor, final Method serializeMethod, final Class<? extends Enum<?>> titlePacketClassAction, final Constructor<?> titlePacketConstructor, final Constructor<?> titlePacketConstructorTimes) {
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
