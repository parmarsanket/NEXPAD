package com.sanket.tools.nexpad.runtime.engine

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * Built-in GPU AGSL (Android Graphics Shading Language) shaders for Android 13+ (API 33+).
 * Renders high-end holographic, glowing cyber, and glass effects directly on the GPU.
 */
object NxpShaderLibrary {

    const val CYBER_GLOW_SRC = """
        uniform float2 iResolution;
        uniform float iTime;
        uniform float4 uColor;
        
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution;
            float dist = length(uv - float2(0.5, 0.5)) * 2.0;
            float glow = 0.05 / (abs(dist - 0.7) + 0.05);
            return half4(uColor.rgb * glow, clamp(glow, 0.0, 1.0) * uColor.a);
        }
    """

    const val HOLOGRAPHIC_GLASS_SRC = """
        uniform float2 iResolution;
        uniform float iTime;
        
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / iResolution;
            float scanline = sin(uv.y * 120.0 + iTime * 4.0) * 0.15;
            float alpha = 0.85 + scanline;
            return half4(0.04, 0.09 + scanline * 0.5, 0.18 + scanline, alpha);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createShaderEffect(shaderSource: String, width: Float, height: Float, timeSec: Float = 0f): androidx.compose.ui.graphics.RenderEffect? {
        return try {
            val shader = RuntimeShader(shaderSource)
            shader.setFloatUniform("iResolution", width, height)
            shader.setFloatUniform("iTime", timeSec)
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        } catch (_: Exception) {
            null
        }
    }
}
