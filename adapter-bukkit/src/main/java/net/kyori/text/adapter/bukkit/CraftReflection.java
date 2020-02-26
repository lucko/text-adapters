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

final class CraftReflection extends AbstractCraftReflection {
  CraftReflection(final Method getHandleMethod, final Field playerConnectionField, final Method sendPacketMethod, final Constructor<?> chatPacketConstructor, final Class<? extends Enum<?>> titlePacketClassAction, final Constructor<?> titlePacketConstructor, final Constructor<?> titlePacketConstructorTimes, final Method serializeMethod) {
    this.getHandleMethod = getHandleMethod;
    this.playerConnectionField = playerConnectionField;
    this.sendPacketMethod = sendPacketMethod;
    this.chatPacketConstructor = chatPacketConstructor;
    this.titlePacketClassAction = titlePacketClassAction;
    this.titlePacketConstructor = titlePacketConstructor;
    this.titlePacketConstructorTimes = titlePacketConstructorTimes;
    this.serializeMethod = serializeMethod;
  }

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
    return new CraftReflection(getHandleMethod, playerConnectionField, sendPacketMethod, chatPacketConstructor, titlePacketClassAction, titlePacketConstructor, titlePacketConstructorTimes, serializeMethod);
  }

  final Method getHandleMethod;
  final Field playerConnectionField;
  final Method sendPacketMethod;

  final Constructor<?> chatPacketConstructor;

  final Class<? extends Enum> titlePacketClassAction;
  final Constructor<?> titlePacketConstructor;
  final Constructor<?> titlePacketConstructorTimes;
  final Method serializeMethod;

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

  Object createTitleClearPacket() throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return this.titlePacketConstructor.newInstance(this.titleType("CLEAR", 4), null);
  }

  Object createTitleResetPacket() throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return this.titlePacketConstructor.newInstance(this.titleType("RESET", 5), null);
  }

  Object createTitleTitlePacket(final Component title) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return this.titlePacketConstructor.newInstance(this.titleType("TITLE", 0), this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(title)));
  }

  Object createTitleSubtitlePacket(final Component title) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return this.titlePacketConstructor.newInstance(this.titleType("SUBTITLE", 1), this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(title)));
  }

  Object createTitleActionbarPacket(final Component title) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return this.titlePacketConstructor.newInstance(this.titleType("ACTIONBAR", 2), this.serializeMethod.invoke(null, GsonComponentSerializer.INSTANCE.serialize(title)));
  }

  Object createTitleTimesPacket(final Title.Times times) throws IllegalAccessException, InstantiationException, InvocationTargetException {
    return this.titlePacketConstructorTimes.newInstance(times.fadeIn(), times.stay(), times.fadeOut());
  }

  private Enum titleType(final String name, final int ordinal) {
    try {
      return Enum.valueOf(this.titlePacketClassAction, name);
    } catch(final IllegalArgumentException e) {
      return this.titlePacketClassAction.getEnumConstants()[ordinal];
    }
  }
}
