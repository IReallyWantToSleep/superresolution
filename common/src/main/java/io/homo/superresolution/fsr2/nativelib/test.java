package io.homo.superresolution.fsr2.nativelib;

import com.mojang.blaze3d.systems.RenderSystem;
import io.homo.superresolution.SuperResolution;
import io.homo.superresolution.config.Config;
import io.homo.superresolution.fsr2.FFXError;
import io.homo.superresolution.utils.FrameBuffer;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.*;
import static io.homo.superresolution.fsr2.types.enums.FfxFsr2InitializationFlagBits.FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class test {
    private long window;
    private ffx_fsr2_api api;
    private FrameBuffer mv;
    private FrameBuffer color;
    private FrameBuffer out;
    public void run() throws InterruptedException {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        RenderSystem.initRenderThread();
        api = new ffx_fsr2_api("I:/superresolution/fsr2_win64/x64/Release/fsr2_win64.dll");
        api.init();
        init();
        initFsr2();
        System.out.println((api.ffxGetTextureResourceGL(0,1,1,0)));
        //loop();
        SuperResolution.LOGGER.debug("FSR2 ffxFsr2ContextDestroy: {}",
                FFXError.returnErrorText(
                        api.ffxFsr2ContextDestroy()
                )
        );
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(1600, 900, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    public void initFsr2(){
        SuperResolution.LOGGER.debug("FSR2 ffxFsr2GetInterfaceGL: {}",
                FFXError.returnErrorText(
                        api.ffxFsr2GetInterfaceGL(
                                api.ffxFsr2GetScratchMemorySizeGL(),
                                Config.getFsr2Ratio(),
                                1600,
                                900,
                                FFX_FSR2_ENABLE_DEBUG_CHECKING.getValue() |
                                        FFX_FSR2_ENABLE_AUTO_EXPOSURE.getValue() |
                                        FFX_FSR2_ENABLE_HIGH_DYNAMIC_RANGE.getValue() |
                                        FFX_FSR2_ALLOW_NULL_DEVICE_AND_COMMAND_LIST.getValue()
                        )
                )
        );
        SuperResolution.LOGGER.debug("FSR2 ffxFsr2CreateContext: {}",
                FFXError.returnErrorText(
                        api.ffxFsr2CreateContext()
                )
        );

    }
    public static void main(String[] args) throws InterruptedException {
        new test().run();
    }
}
