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

import java.util.List;
import net.kyori.text.Component;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.title.Title;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

final class LegacyPipe implements Pipe {
  @Override
  public void message(final @NonNull Component type, final @NonNull List<? extends CommandSender> viewers) {
    final String legacy = LegacyComponentSerializer.INSTANCE.serialize(type);
    for(int i = 0, size = viewers.size(); i < size; i++) {
      viewers.get(i).sendMessage(legacy);
    }
    // this is the end of the line - nobody left after this
    viewers.clear();
  }

  @Override
  public void actionBar(final @NonNull Component type, final @NonNull List<? extends CommandSender> viewers) {
    // unsupported
  }

  @Override
  public void title(final @NonNull Title type, final @NonNull List<? extends CommandSender> viewers) {
    // unsupported
  }
}
