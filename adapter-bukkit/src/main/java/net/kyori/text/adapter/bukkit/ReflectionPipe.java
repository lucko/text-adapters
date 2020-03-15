package net.kyori.text.adapter.bukkit;

import java.lang.reflect.Field;

interface ReflectionPipe extends Pipe {
  static Field field(final Class<?> klass, final String name) throws NoSuchFieldException {
    final Field field = klass.getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }
}
