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

import java.util.Arrays;
import java.util.Collections;
import net.kyori.text.Component;
import net.kyori.text.title.Title;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An adapter for sending text {@link Component}s to Bukkit objects.
 */
public interface TextAdapter {
  static @NonNull Type<Component> messages() {
    return TextAdapter0.MESSAGES;
  }

  static @NonNull Type<Component> actionBars() {
    return TextAdapter0.ACTION_BARS;
  }

  static @NonNull Type<Title> titles() {
    return TextAdapter0.TITLES;
  }

  interface Type<T> {
    default void send(final @NonNull T type, final @NonNull CommandSender viewer) { this.send(type, Collections.singleton(viewer)); }
    default void send(final @NonNull T type, final @NonNull CommandSender@NonNull... viewers) { this.send(type, Arrays.asList(viewers)); }
    void send(final @NonNull T type, final @NonNull Iterable<? extends CommandSender> viewers);
  }

  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @deprecated use {@link #messages()}
   */
  @Deprecated
  static void sendMessage(final @NonNull CommandSender viewer, final @NonNull Component component) {
    messages().send(component, viewer);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @deprecated use {@link #messages()}
   */
  @Deprecated
  static void sendMessage(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    messages().send(component, viewers);
  }

  /**
   * Sends {@code component} to the given {@code viewer}'s action bar.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @deprecated use {@link #actionBars()}
   */
  @Deprecated
  static void sendActionBar(final @NonNull CommandSender viewer, final @NonNull Component component) {
    actionBars().send(component, viewer);
  }

  /**
   * Sends {@code component} to the given {@code viewers}'s action bar.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @deprecated use {@link #actionBars()}
   */
  @Deprecated
  static void sendActionBar(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    actionBars().send(component, viewers);
  }

  /**
   * Sends {@code component} to the given {@code viewer}.
   *
   * @param viewer the viewer to send the component to
   * @param component the component
   * @deprecated use {@link #messages()}
   */
  @Deprecated
  static void sendComponent(final @NonNull CommandSender viewer, final @NonNull Component component) {
    sendComponent(viewer, component);
  }

  /**
   * Sends {@code component} to the given {@code viewers}.
   *
   * @param viewers the viewers to send the component to
   * @param component the component
   * @deprecated use {@link #messages()}
   */
  @Deprecated
  static void sendComponent(final @NonNull Iterable<? extends CommandSender> viewers, final @NonNull Component component) {
    sendMessage(viewers, component);
  }
}
