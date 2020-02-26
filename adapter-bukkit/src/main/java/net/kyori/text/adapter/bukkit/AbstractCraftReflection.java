package net.kyori.text.adapter.bukkit;

abstract class AbstractCraftReflection {
  static boolean isCompatibleServer(final Class<?> serverClass) {
    return serverClass.getPackage().getName().startsWith("org.bukkit.craftbukkit")
      && serverClass.getSimpleName().equals("CraftServer");
  }

  static Class<?> craftBukkitClass(final String version, final String name) throws ClassNotFoundException {
    return Class.forName("org.bukkit.craftbukkit." + version + name);
  }

  static Class<?> minecraftClass(final String version, final String name) throws ClassNotFoundException {
    return Class.forName("net.minecraft.server." + version + name);
  }

  static String maybeVersion(final String version) {
    if(version.isEmpty()) {
      return "";
    } else if(version.charAt(0) == '.') {
      return version.substring(1) + '.';
    }
    throw new IllegalArgumentException("Unknown version " + version);
  }

  static Class<?> optionalMinecraftClass(final String version, final String name) {
    try {
      return minecraftClass(version, name);
    } catch(final ClassNotFoundException e) {
      return null;
    }
  }
}
