package com.fragmenterworks.ffxivextract.shaders;

import com.jogamp.opengl.GL3;

import java.io.IOException;

public class BGShader extends Shader {
		
	public BGShader(GL3 gl)
			throws IOException {
		//super(gl, "/res/shaders/model_vert.glsl", "/res/shaders/bg_frag.glsl", true);		
		super(gl, MinifiedShaders.model_vert_glsl, MinifiedShaders.bg_frag_glsl, false);
	}

}
