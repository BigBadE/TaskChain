/*
 * Copyright (c) 2016-2017 Daniel Ennis (Aikar) - MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files (the
 *  "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to
 *  permit persons to whom the Software is furnished to do so, subject to
 *  the following conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 *  LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 *  OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package co.aikar.taskchain;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class SpongeTaskChainFactory extends TaskChainFactory {
    private SpongeTaskChainFactory(Object plugin, AsyncQueue asyncQueue) {
        super(new SpongeGameInterface(plugin, asyncQueue));
    }

    public static TaskChainFactory create(PluginContainer pluginContainer) {
        return create(pluginContainer.getInstance().orElse(null));
    }
    public static TaskChainFactory create(Object plugin) {
        return new SpongeTaskChainFactory(plugin, new TaskChainAsyncQueue());
    }
/* @TODO: #9 - Not Safe to do this
    public static TaskChainFactory create(Object plugin, ThreadPoolExecutor executor) {
        return new SpongeTaskChainFactory(new SpongeGameInterface(plugin, new TaskChainAsyncQueue(executor)));
    }

    public static TaskChainFactory create(Object plugin, AsyncQueue asyncQueue) {
        return new SpongeTaskChainFactory(new SpongeGameInterface(plugin, asyncQueue));
    }*/


    private static class SpongeGameInterface implements GameInterface {
        private final Object plugin;
        private final AsyncQueue asyncQueue;

        private SpongeGameInterface(Object plugin, AsyncQueue asyncQueue) {
            this.asyncQueue = asyncQueue;
            if (plugin == null || !Sponge.getPluginManager().fromInstance(plugin).isPresent()) {
                throw new IllegalArgumentException("Not a valid Sponge Plugin");
            }
            this.plugin = plugin;
        }

        @Override
        public boolean isAsync() {
            return !Sponge.getServer().isMainThread();
        }

        @Override
        public AsyncQueue getAsyncQueue() {
            return asyncQueue;
        }

        @Override
        public void postToMain(Runnable run) {
            Task.builder().execute(run).submit(plugin);
        }

        @Override
        public void scheduleTask(int gameUnits, Runnable run) {
            Task.builder().delayTicks(gameUnits).execute(run).submit(plugin);
        }

        @Override
        public void registerShutdownHandler(TaskChainFactory factory) {
            Sponge.getEventManager().registerListener(plugin, GameStoppingEvent.class, event -> {
                factory.shutdown(60, TimeUnit.SECONDS);
            });
        }
    }
}
