package io.homo.superresolution.common.render.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;

import java.nio.LongBuffer;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

public class Utils {
    public static PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection) {

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public static PointerBuffer asPointerBuffer(MemoryStack stack, List<? extends Pointer> list) {

        PointerBuffer buffer = stack.mallocPointer(list.size());

        list.forEach(buffer::put);

        return buffer.rewind();
    }

    public static LongBuffer createLongBuffer() {
        return MemoryStack.stackCallocLong(1);
    }

    public static void VK_CHECK(int code) {
        if (code != VK_SUCCESS) throw new VkException();
    }

    public static void VK_CHECK(int code, String msg) {
        if (code != VK_SUCCESS) throw new VkException(msg);
    }
}
