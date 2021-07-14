package com.fragmenterworks.ffxivextract.shaders;

import com.jogamp.opengl.GL3;

import java.io.IOException;

public class SkinShader extends Shader {
		
	public SkinShader(GL3 gl)
			throws IOException {
		//super(gl, "/res/shaders/model_vert_boned.glsl", "/res/shaders/skin_frag.glsl", true);		
		super(gl, MinifiedShaders.model_vert_glsl, MinifiedShaders.skin_frag_glsl, false);
	}

}
