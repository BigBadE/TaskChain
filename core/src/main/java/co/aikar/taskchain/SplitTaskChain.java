/*
 * Copyright (c) 2016-2020 Daniel Ennis (Aikar) - MIT License
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

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SplitTaskChain<T> extends TaskChain<T> {
    private final TaskChain<T> parent;

    private final List<TaskHolder<?, ?>> tasks = new ArrayList<>();

    private final ThreadPoolExecutor executor = TaskChainAsyncQueue.createCachedThreadPool();

    public SplitTaskChain(TaskChain<T> parent, TaskChainFactory factory) {
        super(factory);
        this.parent = parent;
    }

    public TaskChain<T> collect() {
        parent.async(() -> {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for(TaskHolder<?, ?> task : tasks) {
                futures.add(CompletableFuture.runAsync(() -> task.getTask().run(null), executor));
            }
            for(int i = 0; i < futures.size(); i++) {
                try {
                    futures.get(i).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    getErrorHandler().accept(e, tasks.get(i).getTask());
                }
            }
        });
        return parent;
    }

    @Override
    public boolean hasTaskData(String key) {
        return parent.hasTaskData(key);
    }

    @Override
    public <R> R getTaskData(String key) {
        return parent.getTaskData(key);
    }

    @Override
    public <R> R removeTaskData(String key) {
        return parent.removeTaskData(key);
    }

    @Override
    public <R> R setTaskData(String key, Object val) {
        return parent.setTaskData(key, val);
    }

    @Override
    public TaskChain<T> delay(int gameUnits) {
        throw new TaskChainException("Cannot delay simultaneous execution, collect first!");
    }

    @Override
    public TaskChain<T> delay(int duration, TimeUnit unit) {
        throw new TaskChainException("Cannot delay simultaneous execution, collect first!");
    }
    
    

    @Override
    void execute0() {
        throw new TaskChainException("Cannot execute a split chain, collect it first!");
    }

    @Override
    public TaskChain add0(TaskHolder<?, ?> task) {
        if(!task.async) {
            throw new TaskChainException("Split chains can only run async tasks");
        }
        
        synchronized (this) {
            if (isExecuted()) {
                throw new TaskChainException("TaskChain is executing");
            }
        }

        tasks.add(task);
        return this;
    }
}
