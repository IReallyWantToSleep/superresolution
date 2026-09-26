package io.homo.superresolution.core.graphics.opengl.shader;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class GlShaderProgramTest {
    private static final String NANOVG_VERT = "/shader/nanovg/nanovg_rhi.vert.glsl";
    private static final String NANOVG_FRAG = "/shader/nanovg/nanovg_rhi.frag.glsl";

    @Test
    public void selectsLegacy410ForOpenGL41And42() {
        assertEquals(410, GlShaderProgram.resolveCompatibilityGlslVersion(4, 0));
        assertEquals(410, GlShaderProgram.resolveCompatibilityGlslVersion(4, 1));
        assertEquals(410, GlShaderProgram.resolveCompatibilityGlslVersion(4, 2));
    }

    @Test
    public void selectsMatchingGlslForOpenGL43Through45() {
        assertEquals(430, GlShaderProgram.resolveCompatibilityGlslVersion(4, 3));
        assertEquals(440, GlShaderProgram.resolveCompatibilityGlslVersion(4, 4));
        assertEquals(450, GlShaderProgram.resolveCompatibilityGlslVersion(4, 5));
    }

    @Test
    public void capsAt460ForOpenGL46AndNewer() {
        assertEquals(460, GlShaderProgram.resolveCompatibilityGlslVersion(4, 6));
        assertEquals(460, GlShaderProgram.resolveCompatibilityGlslVersion(4, 7));
        assertEquals(460, GlShaderProgram.resolveCompatibilityGlslVersion(5, 0));
    }

    @Test
    public void preparesSourceForOpenGL43Through46Ceilings() {
        String source = "#version 460\nvoid main() {}\n";
        assertEquals("#version 430", versionDirective(GlShaderProgram.prepareCompatibilityShaderSource(
                source, GlShaderProgram.resolveCompatibilityGlslVersion(4, 3))));
        assertEquals("#version 440", versionDirective(GlShaderProgram.prepareCompatibilityShaderSource(
                source, GlShaderProgram.resolveCompatibilityGlslVersion(4, 4))));
        assertEquals("#version 450", versionDirective(GlShaderProgram.prepareCompatibilityShaderSource(
                source, GlShaderProgram.resolveCompatibilityGlslVersion(4, 5))));
        assertEquals("#version 460", versionDirective(GlShaderProgram.prepareCompatibilityShaderSource(
                source, GlShaderProgram.resolveCompatibilityGlslVersion(4, 6))));
    }

    @Test
    public void nanovgShadersReceiveSupportedVersionOnOpenGL45() throws IOException {
        String vertex = readResource(NANOVG_VERT);
        String fragment = readResource(NANOVG_FRAG);
        int glslVersion = GlShaderProgram.resolveCompatibilityGlslVersion(4, 5);

        String preparedVertex = GlShaderProgram.prepareCompatibilityShaderSource(
                glslangLikePreprocessed(vertex), glslVersion);
        String preparedFragment = GlShaderProgram.prepareCompatibilityShaderSource(
                glslangLikePreprocessed(fragment), glslVersion);

        assertEquals(450, glslVersion);
        assertEquals("#version 450", versionDirective(preparedVertex));
        assertEquals("#version 450", versionDirective(preparedFragment));
        assertNanoVgVertexInterfaces(preparedVertex);
        assertNanoVgFragmentInterfaces(preparedFragment);
        assertFalse(preparedVertex.contains("#line"));
        assertFalse(preparedFragment.contains("#line"));
        assertFalse(preparedVertex.contains("GL_GOOGLE_include_directive"));
        assertFalse(preparedFragment.contains("GL_GOOGLE_include_directive"));
    }

    @Test
    public void nanovgShadersKeepVersion460OnOpenGL46() throws IOException {
        String vertex = readResource(NANOVG_VERT);
        String fragment = readResource(NANOVG_FRAG);
        int glslVersion = GlShaderProgram.resolveCompatibilityGlslVersion(4, 6);

        String preparedVertex = GlShaderProgram.prepareCompatibilityShaderSource(
                glslangLikePreprocessed(vertex), glslVersion);
        String preparedFragment = GlShaderProgram.prepareCompatibilityShaderSource(
                glslangLikePreprocessed(fragment), glslVersion);

        assertEquals(460, glslVersion);
        assertEquals("#version 460", versionDirective(preparedVertex));
        assertEquals("#version 460", versionDirective(preparedFragment));
        assertNanoVgVertexInterfaces(preparedVertex);
        assertNanoVgFragmentInterfaces(preparedFragment);
    }

    @Test
    public void keepsAlreadySupportedVersionDirective() {
        String source = "#version 430 core\nvoid main() {}\n";
        String prepared = GlShaderProgram.prepareCompatibilityShaderSource(source, 450);
        assertEquals("#version 430 core", versionDirective(prepared));
        assertTrue(prepared.contains("void main() {}"));
    }

    @Test
    public void preservesWhitespaceNewlinesExtensionOrderAndBodies() {
        String source = "  #version  430\n"
                + "\n"
                + "#extension GL_ARB_separate_shader_objects : enable\n"
                + "void main() {\n"
                + "    int value = 1;\n"
                + "}\n"
                + "#extension GL_ARB_shading_language_420pack : enable\n";

        String prepared = GlShaderProgram.prepareCompatibilityShaderSource(source, 450);
        List<String> lines = lines(prepared);

        assertEquals("  #version  430", lines.get(0));
        assertEquals("#extension GL_ARB_separate_shader_objects : enable", lines.get(1));
        assertEquals("#extension GL_ARB_shading_language_420pack : enable", lines.get(2));
        assertEquals("", lines.get(3));
        assertEquals("void main() {", lines.get(4));
        assertEquals("    int value = 1;", lines.get(5));
        assertEquals("}", lines.get(6));
        assertFalse(prepared.contains("#line"));
    }

    @Test
    public void rewritesVersionAboveCeilingAndRemovesLineDirectives() {
        String source = "#version 460 core\n"
                + "#extension GL_GOOGLE_include_directive : enable\n"
                + "#line 1\n"
                + "layout(location = 0) in vec2 vertex;\n"
                + "void main() { gl_Position = vec4(vertex, 0.0, 1.0); }\n";

        String prepared = GlShaderProgram.prepareCompatibilityShaderSource(source, 450);
        List<String> lines = lines(prepared);

        assertEquals("#version 450 core", lines.get(0));
        assertEquals("layout(location = 0) in vec2 vertex;", lines.get(1));
        assertTrue(prepared.contains("void main() { gl_Position = vec4(vertex, 0.0, 1.0); }"));
        assertFalse(prepared.contains("#line"));
        assertFalse(prepared.contains("GL_GOOGLE_include_directive"));
    }

    @Test
    public void legacyCeilingRewritesNewerDeclarationsWithoutChangingTheBody() {
        String source = "#version 460\n"
                + "layout(location = 0) out vec4 color;\n"
                + "void main() { color = vec4(1.0); }\n";

        String prepared = GlShaderProgram.prepareCompatibilityShaderSource(
                source, GlShaderProgram.resolveCompatibilityGlslVersion(4, 1));

        assertEquals("#version 410", versionDirective(prepared));
        assertTrue(prepared.contains("layout(location = 0) out vec4 color;"));
        assertTrue(prepared.contains("void main() { color = vec4(1.0); }"));
    }

    @Test
    public void insertsCeilingWhenVersionDirectiveIsMissing() {
        String prepared = GlShaderProgram.prepareCompatibilityShaderSource("void main() {}\n", 450);
        assertEquals("#version 450", versionDirective(prepared));
        assertTrue(prepared.contains("void main() {}"));
    }

    @Test
    public void leavesUnsupportedSyntaxVisibleForLaterCompilationFailure() {
        String source = "#version 460\n"
                + "layout(binding = 9) uniform not_a_real_type brokenUniform;\n"
                + "void main() { not_a_real_function(); }\n";

        String prepared = GlShaderProgram.prepareCompatibilityShaderSource(source, 450);
        assertEquals("#version 450", versionDirective(prepared));
        assertTrue(prepared.contains("uniform not_a_real_type brokenUniform;"));
        assertTrue(prepared.contains("not_a_real_function();"));
    }

    private static void assertNanoVgVertexInterfaces(String source) {
        assertTrue(source.contains("layout(std140, binding = 0) uniform frame"));
        assertTrue(source.contains("vec2 viewSize;"));
        assertTrue(source.contains("layout(location = 0) in vec2 vertex;"));
        assertTrue(source.contains("layout(location = 1) in vec2 tcoord;"));
        assertTrue(source.contains("layout(location = 0) out vec2 ftcoord;"));
        assertTrue(source.contains("layout(location = 1) out vec2 fpos;"));
        assertTrue(source.contains("gl_Position"));
    }

    private static void assertNanoVgFragmentInterfaces(String source) {
        assertTrue(source.contains("layout(binding = 2) uniform sampler2D tex;"));
        assertTrue(source.contains("layout(binding = 3) uniform sampler2D fontTex;"));
        assertTrue(source.contains("layout(std140, binding = 1) uniform frag"));
        assertTrue(source.contains("layout(location = 0) in vec2 ftcoord;"));
        assertTrue(source.contains("layout(location = 1) in vec2 fpos;"));
        assertTrue(source.contains("layout(location = 0) out vec4 outColor;"));
        assertTrue(source.contains("scissorMask"));
        assertTrue(source.contains("strokeMask"));
    }

    private static String glslangLikePreprocessed(String source) {
        StringBuilder output = new StringBuilder();
        boolean injected = false;
        for (String line : source.split("\n", -1)) {
            output.append(line).append('\n');
            if (!injected && line.trim().startsWith("#version")) {
                output.append("#extension GL_GOOGLE_include_directive : enable\n");
                output.append("#line 1\n");
                injected = true;
            }
        }
        if (output.length() > 0 && source.charAt(source.length() - 1) != '\n') {
            output.setLength(output.length() - 1);
        }
        return output.toString();
    }

    private static String versionDirective(String source) {
        for (String line : source.split("\n")) {
            if (line.trim().startsWith("#version")) {
                return line.trim();
            }
        }
        fail("Missing #version directive in:\n" + source);
        return null;
    }

    private static List<String> lines(String source) {
        return new ArrayList<>(List.of(source.split("\n", -1)));
    }

    private static String readResource(String path) throws IOException {
        try (InputStream in = GlShaderProgramTest.class.getResourceAsStream(path)) {
            assertNotNull("Missing shader resource " + path, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
