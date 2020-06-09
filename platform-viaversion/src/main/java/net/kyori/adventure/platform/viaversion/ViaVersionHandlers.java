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
package net.kyori.adventure.platform.viaversion;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.kyori.adventure.platform.impl.Handler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.ViaAPI;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.platform.ViaPlatform;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;

public class ViaVersionHandlers {

  private ViaVersionHandlers() {}

  /**
   * A provider for the ViaVersion api objects
   *
   * @param <V> native player type
   */
  public interface ViaAPIProvider<V> {

    /**
     * Whether ViaVersion is available and should be used (generally true if the running game version is less than what is supported).
     *
     * @return if available
     */
    boolean isAvailable();

    ViaPlatform<? extends V> platform();

    default UserConnection connection(final V viewer) {
      final UUID viewerId = id(viewer);
      return viewerId == null ? null : platform().getConnectionManager().getConnectedClient(viewerId);
    }

    @Nullable UUID id(final V viewer);
  }

  static abstract class ConnectionBased<V> implements Handler<V> {
    private final ViaAPIProvider<? super V> via;

    public ConnectionBased(final ViaAPIProvider<? super V> via) {
      this.via = via;
    }

    @Override
    public boolean isAvailable() {
      if(!via.isAvailable()) return false;
      if(ProtocolRegistry.SERVER_PROTOCOL >= ProtocolVersion.v1_16.getId()) return false; // newest features we rely on were added 1.16, only adapt for older servers

      try {
        Class.forName("us.myles.ViaVersion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2"); // make sure we're on a new version
        return true;
      } catch(ClassNotFoundException e) {
        return false;
      }
    }

    @Override
    public boolean isAvailable(final @NonNull V viewer) {
      final ViaPlatform<?> platform = this.via.platform();
      if(platform == null) {
        return false;
      }
      final UUID viewerId = this.via.id(viewer);
      if(viewerId == null) {
        return false;
      }

      return platform.getApi().isInjected(viewerId) && platform.getApi().getPlayerVersion(viewerId) >= ProtocolVersion.v1_16.getId();
    }

    protected UserConnection connection(final V viewer) {
      return this.via.connection(viewer);
    }
  }

  public static class Chat<V> extends ConnectionBased<V> implements Handler.Chat<V, String> {
    private static final byte CHAT_TYPE_CHAT = 0;
    private static final byte CHAT_TYPE_SYSTEM = 1;
    private static final byte CHAT_TYPE_ACTIONBAR = 2;

    public Chat(final ViaAPIProvider<? super V> provider) {
      super(provider);
    }

    @Override
    public String initState(@NonNull final Component component) {
      return GsonComponentSerializer.INSTANCE.serialize(component);
    }

    @Override
    public void send(@NonNull final V target, @NonNull final String message) {
      final ByteBuf buffer = Unpooled.buffer(4 + message.length() + 1 + 16); // string + byte + uuid (2 longs)
      final PacketWrapper wrapper = new PacketWrapper(ClientboundPackets1_16.CHAT_MESSAGE.ordinal(), buffer, connection(target));
      wrapper.write(Type.STRING, message);
      wrapper.write(Type.BYTE, CHAT_TYPE_SYSTEM);
      wrapper.write(Type.UUID, NIL_UUID);

      try {
        wrapper.send(Protocol1_16To1_15_2.class);
      } catch(Exception ignore) {
      }
    }
  }
}
