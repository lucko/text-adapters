/*
 * This file is part of adventure-platform, licensed under the MIT License.
 *
 * Copyright (c) 2018-2020 KyoriPowered
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
package net.kyori.adventure.platform.bukkit;

import net.kyori.adventure.platform.facet.Knob;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * The entrypoint for Adventure in {@link org.bukkit.Bukkit}.
 *
 * @see #of(Plugin)
 * @see BukkitAudienceProvider
 */
public final class BukkitAdventure {
  private BukkitAdventure() {
  }

  static {
    Knob.OUT = message -> Bukkit.getLogger().log(Level.INFO, message);
    Knob.ERR = (message, error) -> Bukkit.getLogger().log(Level.WARNING, message, error);
  }

  private static final Map<Plugin, BukkitAudienceProvider> INSTANCES = Collections.synchronizedMap(new IdentityHashMap<>(4));

  /**
   * Creates an audience provider for a plugin.
   *
   * <p>There will only be one provider for each plugin.</p>
   *
   * @param plugin a plugin
   * @return an audience provider
   */
  public static BukkitAudienceProvider of(final @NonNull Plugin plugin) {
    return INSTANCES.computeIfAbsent(plugin, BukkitAudienceProviderImpl::new);
  }
}