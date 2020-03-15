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

import net.kyori.text.Component;
import net.kyori.text.title.Title;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

final class TextAdapter0 {
  static final TextAdapter.Type<Component> MESSAGES = new Messages();
  static final TextAdapter.Type<Component> ACTION_BARS = new ActionBars();
  static final TextAdapter.Type<Title> TITLES = new Titles();

  static class Messages implements TextAdapter.Type<Component> {
    @Override
    public void send(final @NonNull Component type, final @NonNull Iterable<? extends CommandSender> viewers) {
      Pipe.send(type, viewers, Pipe::message);
    }
  }

  static class ActionBars implements TextAdapter.Type<Component> {
    @Override
    public void send(final @NonNull Component type, final @NonNull Iterable<? extends CommandSender> viewers) {
      Pipe.send(type, viewers, Pipe::actionBar);
    }
  }

  static class Titles implements TextAdapter.Type<Title> {
    @Override
    public void send(final @NonNull Title type, final @NonNull Iterable<? extends CommandSender> viewers) {
      Pipe.send(type, viewers, Pipe::title);
    }
  }
}
