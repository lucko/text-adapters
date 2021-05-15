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
package net.kyori.adventure.platform.spongeapi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.AudienceIdentity;
import net.kyori.adventure.platform.facet.FacetAudienceProvider;
import net.kyori.adventure.platform.facet.Knob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameState;
import org.spongepowered.api.block.tileentity.CommandBlock;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.command.source.RconSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppedServerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.world.World;

import static java.util.Objects.requireNonNull;

@Singleton // one instance per plugin module
final class SpongeAudiencesImpl extends FacetAudienceProvider<MessageReceiver, SpongeAudience> implements SpongeAudiences {

  static {
    final Logger logger = LoggerFactory.getLogger(SpongeAudiences.class);
    Knob.OUT = logger::debug;
    Knob.ERR = logger::warn;
  }

  private static final Map<String, SpongeAudiences> INSTANCES = Collections.synchronizedMap(new HashMap<>(4));

  static Builder builder(final @NonNull PluginContainer plugin, final @NonNull Game game) {
    return new Builder(plugin, game);
  }

  static SpongeAudiences instanceFor(final @NonNull PluginContainer plugin, final @NonNull Game game) {
    return builder(plugin, game).build();
  }

  private final Game game;
  private final EventManager eventManager;
  private final EventListener eventListener;

  @Inject
  SpongeAudiencesImpl(final @NonNull PluginContainer plugin, final @NonNull Game game, final @NonNull ComponentRenderer<AudienceIdentity> componentRenderer, final @NonNull ToIntFunction<AudienceIdentity> partitionFunction) {
    super(componentRenderer, partitionFunction);
    this.game = game;
    this.eventManager = game.getEventManager();
    this.eventListener = new EventListener();
    this.eventManager.registerListeners(plugin, this.eventListener);
    if(game.isServerAvailable() && game.getState().compareTo(GameState.POST_INITIALIZATION) > 0) { // if we've already post-initialized
      this.addViewer(game.getServer().getConsole());
      for(final Player player : game.getServer().getOnlinePlayers()) {
        this.addViewer(player);
      }
    }
  }

  @NonNull
  @Override
  public Audience receiver(final @NonNull MessageReceiver receiver) {
    if(receiver instanceof Player) {
      return this.player((Player) receiver);
    } else if(receiver instanceof ConsoleSource
      || (receiver instanceof RconSource && ((RconSource) receiver).getLoggedIn())) {
      return this.console();
    } else if(receiver instanceof World) {
      return this.world(Key.key(((World) receiver).getName()));
    } else if(receiver instanceof ProxySource) {
      return this.receiver(((ProxySource) receiver).getOriginalSource());
    } else if(receiver instanceof CommandBlock) {
      return Audience.empty();
    }
    return new SpongeAudience(this, Collections.singletonList(receiver));
  }

  @NonNull
  @Override
  public Audience player(final @NonNull Player player) {
    return this.player(player.getUniqueId());
  }

  @Override
  protected @NonNull AudienceIdentity createIdentity(final @NonNull MessageReceiver viewer) {
    return new SpongeIdentity(viewer);
  }

  @NonNull
  @Override
  protected SpongeAudience createAudience(final @NonNull Collection<MessageReceiver> viewers) {
    return new SpongeAudience(this, viewers);
  }

  @Override
  public void close() {
    this.eventManager.unregisterListeners(this.eventListener);
    super.close();
  }

  final static class Builder implements SpongeAudiences.Builder {
    private final @NonNull PluginContainer plugin;
    private final @NonNull Game game;
    private @MonotonicNonNull ComponentRenderer<AudienceIdentity> componentRenderer;
    private @MonotonicNonNull ToIntFunction<AudienceIdentity> partitionFunction;

    Builder(final @NonNull PluginContainer plugin, final @NonNull Game game) {
      super();
      this.plugin = requireNonNull(plugin, "plugin");
      this.game = requireNonNull(game, "game");
      this.componentRenderer(new ComponentRenderer<AudienceIdentity>() {
        @Override
        public @NonNull Component render(final @NonNull Component component, final @NonNull AudienceIdentity context) {
          return GlobalTranslator.render(component, context.locale());
        }
      });
      this.partitionBy(context -> context.locale().hashCode());
    }

    @Override
    public @NonNull Builder componentRenderer(final @NonNull ComponentRenderer<AudienceIdentity> componentRenderer) {
      this.componentRenderer = requireNonNull(componentRenderer, "component renderer");
      return this;
    }

    @Override
    public @NonNull Builder partitionBy(final @NonNull ToIntFunction<AudienceIdentity> partitionFunction) {
      this.partitionFunction = requireNonNull(partitionFunction, "partition function");
      return this;
    }

    @Override
    public @NonNull SpongeAudiences build() {
      return INSTANCES.computeIfAbsent(this.plugin.getId(), id -> new SpongeAudiencesImpl(this.plugin, this.game, this.componentRenderer, this.partitionFunction));
    }
  }

  public final class EventListener {
    @Listener(order = Order.FIRST)
    public void onLogin(final ClientConnectionEvent.@NonNull Join event) {
      SpongeAudiencesImpl.this.addViewer(event.getTargetEntity());
    }

    @Listener(order = Order.LAST)
    public void onDisconnect(final ClientConnectionEvent.@NonNull Disconnect event) {
      SpongeAudiencesImpl.this.removeViewer(event.getTargetEntity());
    }

    @Listener
    public void onStart(final @NonNull GameStartingServerEvent event) {
      SpongeAudiencesImpl.this.addViewer(SpongeAudiencesImpl.this.game.getServer().getConsole());
    }

    @Listener
    public void onStop(final @NonNull GameStoppedServerEvent event) {
      SpongeAudiencesImpl.this.removeViewer(SpongeAudiencesImpl.this.game.getServer().getConsole());
    }
  }
}
