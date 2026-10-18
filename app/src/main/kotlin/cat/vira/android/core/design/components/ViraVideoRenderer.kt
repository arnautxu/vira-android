package cat.vira.android.core.design.components

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import cat.vira.android.core.resources.ViraVideoAlpha
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/// Dibuixa un vídeo de Vira sobre la superfície d'una `TextureView` transparent, desempaquetant
/// l'alfa quan el clip en porta.
///
/// H.264 no té canal alfa, i els clips que a l'iPhone són HEVC amb transparència arriben aquí amb
/// el color i l'alfa (en grisos) a dues meitats del fotograma (`ViraVideoAlpha`). Aquest
/// renderitzador rep els fotogrames del reproductor en una textura externa i, amb un shader, en
/// treu la meitat del color i hi aplica la de l'alfa. Corre en un fil propi amb EGL; la
/// `TextureView` es compon amb la resta de la interfície respectant la transparència.
///
/// `fill` decideix com cap a la vista: `false` hi cap sencer (`.resizeAspect`), `true` l'omple i
/// en retalla el que sobra (`.resizeAspectFill`).
internal class ViraVideoRenderer(
    private val alpha: ViraVideoAlpha,
    private val contentAspect: Float,
    private val fill: Boolean,
    /// Es crida (al fil principal) quan la superfície on el reproductor ha d'escriure és a punt.
    private val onVideoSurface: (Surface?) -> Unit,
) {
    private val thread = HandlerThread("ViraVideo").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())

    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var windowSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var program = 0
    private var texture = 0
    private var videoTexture: SurfaceTexture? = null
    private var videoSurface: Surface? = null
    private var width = 0
    private var height = 0
    private val matrix = FloatArray(16)

    fun attach(output: SurfaceTexture, width: Int, height: Int) {
        handler.post {
            this.width = width
            this.height = height
            setUp(output)
        }
    }

    fun resize(width: Int, height: Int) {
        handler.post {
            this.width = width
            this.height = height
        }
    }

    /// Es crida quan la `TextureView` perd la superfície. Espera que el fil l'hagi deixada.
    fun detach() {
        main.post { onVideoSurface(null) }
        val done = CountDownLatch(1)
        handler.post { tearDown(); done.countDown() }
        done.await(500, TimeUnit.MILLISECONDS)
    }

    fun release() {
        handler.post { tearDown() }
        thread.quitSafely()
    }

    private fun setUp(output: SurfaceTexture) {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(display, version, 0, version, 1)
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)
        val config = configs[0] ?: return
        context = EGL14.eglCreateContext(
            display, config, EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0,
        )
        windowSurface = EGL14.eglCreateWindowSurface(display, config, output, intArrayOf(EGL14.EGL_NONE), 0)
        EGL14.eglMakeCurrent(display, windowSurface, windowSurface, context)

        program = buildProgram()
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        texture = ids[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        val input = SurfaceTexture(texture)
        input.setOnFrameAvailableListener({ draw() }, handler)
        videoTexture = input
        val surface = Surface(input)
        videoSurface = surface
        clear()
        main.post { onVideoSurface(surface) }
    }

    private fun draw() {
        val input = videoTexture ?: return
        if (windowSurface == EGL14.EGL_NO_SURFACE) return
        input.updateTexImage()
        input.getTransformMatrix(matrix)

        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        viewport()
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "texMatrix"), 1, false, matrix, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "mode"), alpha.ordinal)
        val position = GLES20.glGetAttribLocation(program, "position")
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 0, quad)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        EGL14.eglSwapBuffers(display, windowSurface)
    }

    private fun clear() {
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        EGL14.eglSwapBuffers(display, windowSurface)
    }

    /// Cap sencer o omple, centrat: el viewport pot sortir de la vista, i el que en sobra es retalla.
    private fun viewport() {
        if (width == 0 || height == 0) return
        val viewAspect = width.toFloat() / height
        val (w, h) = if (fill) {
            if (viewAspect > contentAspect) width to (width / contentAspect).roundToInt()
            else (height * contentAspect).roundToInt() to height
        } else {
            if (viewAspect > contentAspect) (height * contentAspect).roundToInt() to height
            else width to (width / contentAspect).roundToInt()
        }
        GLES20.glViewport((width - w) / 2, (height - h) / 2, w, h)
    }

    private fun tearDown() {
        videoSurface?.release()
        videoSurface = null
        videoTexture?.release()
        videoTexture = null
        if (display != EGL14.EGL_NO_DISPLAY) {
            if (program != 0) GLES20.glDeleteProgram(program)
            if (texture != 0) GLES20.glDeleteTextures(1, intArrayOf(texture), 0)
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (windowSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, windowSurface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
        program = 0
        texture = 0
        windowSurface = EGL14.EGL_NO_SURFACE
        context = EGL14.EGL_NO_CONTEXT
        display = EGL14.EGL_NO_DISPLAY
    }

    private fun buildProgram(): Int {
        fun shader(type: Int, source: String) = GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, source)
            GLES20.glCompileShader(it)
        }
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, shader(GLES20.GL_VERTEX_SHADER, VERTEX))
            GLES20.glAttachShader(it, shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT))
            GLES20.glLinkProgram(it)
        }
    }

    private companion object {
        val quad: FloatBuffer = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }

        // `uv` té l'origen a baix a l'esquerra del fotograma, que és el que espera la matriu de
        // la `SurfaceTexture`.
        const val VERTEX = """
            attribute vec2 position;
            varying vec2 uv;
            void main() {
                uv = position * 0.5 + 0.5;
                gl_Position = vec4(position, 0.0, 1.0);
            }
        """

        // mode 0: sense alfa · 1: color a dalt, alfa a sota · 2: color a l'esquerra, alfa a la dreta.
        // Surt premultiplicat, que és com la TextureView compon.
        const val FRAGMENT = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 uv;
            uniform samplerExternalOES frame;
            uniform mat4 texMatrix;
            uniform int mode;
            vec4 sampleAt(vec2 p) { return texture2D(frame, (texMatrix * vec4(p, 0.0, 1.0)).xy); }
            void main() {
                if (mode == 0) {
                    gl_FragColor = vec4(sampleAt(uv).rgb, 1.0);
                } else if (mode == 1) {
                    vec3 color = sampleAt(vec2(uv.x, 0.5 + uv.y * 0.5)).rgb;
                    float a = sampleAt(vec2(uv.x, uv.y * 0.5)).r;
                    gl_FragColor = vec4(color * a, a);
                } else {
                    vec3 color = sampleAt(vec2(uv.x * 0.5, uv.y)).rgb;
                    float a = sampleAt(vec2(0.5 + uv.x * 0.5, uv.y)).r;
                    gl_FragColor = vec4(color * a, a);
                }
            }
        """
    }
}
