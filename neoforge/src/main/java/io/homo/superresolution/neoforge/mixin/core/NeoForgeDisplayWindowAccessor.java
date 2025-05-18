package io.homo.superresolution.neoforge.mixin.core;

import net.neoforged.fml.earlydisplay.DisplayWindow;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

@Mixin(DisplayWindow.class)
public interface NeoForgeDisplayWindowAccessor {
    /*
    @Invoker(value = "crashElegantly")
    void crashElegantlyInvoker(String errorDetails);

    @Invoker(value = "getLastGlfwError")
    Optional<String> getLastGlfwErrorInvoker();

    @Accessor(value = "LOGGER")
    Logger getLogger();

    @Accessor(value = "renderScheduler")
    ScheduledExecutorService getRenderScheduler();

    @Accessor(value = "winWidth")
    int getWinWidth();

    @Accessor(value = "winWidth")
    void setWinWidth(int v);

    @Accessor(value = "winHeight")
    int getWinHeight();

    @Accessor(value = "winHeight")
    void setWinHeight(int v);

    @Accessor(value = "window")
    long getWindow();

    @Accessor(value = "window")
    void setWindow(long window);

    @Accessor(value = "winX")
    int getWinX();

    @Accessor(value = "winX")
    void setWinX(int v);

    @Accessor(value = "winY")
    int getWinY();

    @Accessor(value = "winY")
    void setWinY(int v);

    @Accessor(value = "fbWidth")
    int getFbWidth();

    @Accessor(value = "fbWidth")
    void setFbWidth(int v);

    @Accessor(value = "fbHeight")
    int getFbHeight();

    @Accessor(value = "fbHeight")
    void setFbHeight(int v);

    @Accessor(value = "maximized")
    boolean isMaximized();


    @Invoker(value = "fbResize")
    void fbResizeInvoker(long window, int width, int height);

    @Invoker(value = "fbResize")
    void winMoveInvoker(long window, int x, int y);

    @Invoker(value = "fbResize")
    void winResizeInvoker(long window, int width, int height);
*/
}
