#include <glad/gl.h>
#include "glfw/glfw3.h"
#ifndef GL_FUNCS_H
#define GL_FUNCS_H
struct GlFunctionTable
{
    void (*glActiveTexture)(GLenum texture);
    void (*glAttachShader)(GLuint program, GLuint shader);
    void (*glBindAttribLocation)(GLuint program, GLuint index, const GLchar *name);
    void (*glBindBuffer)(GLenum target, GLuint buffer);
    void (*glBindBufferRange)(GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size);
    void (*glBindTexture)(GLenum target, GLuint texture);
    void (*glBindVertexArray)(GLuint array);
    void (*glBlendFuncSeparate)(GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha);
    void (*glBufferData)(GLenum target, GLsizeiptr size, const void *data, GLenum usage);
    void (*glColorMask)(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
    void (*glCompileShader)(GLuint shader);
    GLuint (*glCreateProgram)(void);
    GLuint (*glCreateShader)(GLenum type);
    void (*glCullFace)(GLenum mode);
    void (*glDeleteBuffers)(GLsizei n, const GLuint *buffers);
    void (*glDeleteProgram)(GLuint program);
    void (*glDeleteShader)(GLuint shader);
    void (*glDeleteTextures)(GLsizei n, const GLuint *textures);
    void (*glDeleteVertexArrays)(GLsizei n, const GLuint *arrays);
    void (*glDisable)(GLenum cap);
    void (*glDisableVertexAttribArray)(GLuint index);
    void (*glDrawArrays)(GLenum mode, GLint first, GLsizei count);
    void (*glEnable)(GLenum cap);
    void (*glEnableVertexAttribArray)(GLuint index);
    void (*glFinish)(void);
    void (*glFrontFace)(GLenum mode);
    void (*glGenBuffers)(GLsizei n, GLuint *buffers);
    void (*glGenTextures)(GLsizei n, GLuint *textures);
    void (*glGenVertexArrays)(GLsizei n, GLuint *arrays);
    void (*glGenerateMipmap)(GLenum target);
    GLenum (*glGetError)(void);
    void (*glGetIntegerv)(GLenum pname, GLint *data);
    void (*glGetProgramInfoLog)(GLuint program, GLsizei bufSize, GLsizei *length, GLchar *infoLog);
    void (*glGetProgramiv)(GLuint program, GLenum pname, GLint *params);
    void (*glGetShaderInfoLog)(GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog);
    void (*glGetShaderiv)(GLuint shader, GLenum pname, GLint *params);
    GLuint (*glGetUniformBlockIndex)(GLuint program, const GLchar *uniformBlockName);
    GLint (*glGetUniformLocation)(GLuint program, const GLchar *name);
    void (*glLinkProgram)(GLuint program);
    void (*glPixelStorei)(GLenum pname, GLint param);
    void (*glShaderSource)(GLuint shader, GLsizei count, const GLchar *const *string, const GLint *length);
    void (*glStencilFunc)(GLenum func, GLint ref, GLuint mask);
    void (*glStencilMask)(GLuint mask);
    void (*glStencilOp)(GLenum fail, GLenum zfail, GLenum zpass);
    void (*glStencilOpSeparate)(GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass);
    void (*glTexImage2D)(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const void *pixels);
    void (*glTexParameteri)(GLenum target, GLenum pname, GLint param);
    void (*glTexSubImage2D)(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *pixels);
    void (*glUniform1i)(GLint location, GLint v0);
    void (*glUniform2fv)(GLint location, GLsizei count, const GLfloat *value);
    void (*glUniformBlockBinding)(GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding);
    void (*glUseProgram)(GLuint program);
    void (*glVertexAttribPointer)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void *pointer);
};
#endif // GL_FUNCS_H