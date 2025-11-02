package io.homo.superresolution.core.gui.core.backends.interfaces;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class BatchManager {
    private final Stack<CommandsBatch> stack = new Stack<>();
    private final List<CommandsBatch> rootBatches = new ArrayList<>();

    public CommandsBatch beginBatch() {
        CommandsBatch batch = new CommandsBatch();
        if (!stack.isEmpty()) {
            stack.peek().addChildBatch(batch);
        } else {
            rootBatches.add(batch);
        }
        stack.push(batch);
        return batch;
    }

    public void endBatch(int zIndex) {
        if (stack.isEmpty())
            throw new IllegalStateException("No active batch to end");

        CommandsBatch current = stack.pop();
        current.zIndex(zIndex);

        if (!stack.isEmpty()) {
            CommandsBatch parent = stack.peek();

            if (zIndex > parent.zIndex()) {
                CommandsBatch grandParent = stack.size() >= 2 ? stack.get(stack.size() - 2) : null;
                if (grandParent != null) {
                    grandParent.addChildBatch(current);
                } else {
                    rootBatches.add(current);
                }
            } else {
                parent.addChildBatch(current);
            }
        } else {
            rootBatches.add(current);
        }
    }

    public void clearBatches() {
        rootBatches.clear();
        stack.clear();
    }

    public void closeCurrentBatch() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    public CommandsBatch getCurrentBatch() {
        return stack.isEmpty() ? null : stack.peek();
    }

    public void executeAll() {
        rootBatches.sort(Comparator.comparingInt(CommandsBatch::zIndex));
        for (CommandsBatch batch : rootBatches) {
            batch.execute();
        }
    }

    public void clear() {
        rootBatches.clear();
        stack.clear();
    }
}
