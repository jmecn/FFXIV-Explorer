package com.fragmenterworks.ffxivextract.gui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.HavokNative;
import com.fragmenterworks.ffxivextract.helpers.Matrix;
import com.fragmenterworks.ffxivextract.models.Model;
import com.fragmenterworks.ffxivextract.shaders.BlendShader;
import com.fragmenterworks.ffxivextract.shaders.BlurShader;
import com.fragmenterworks.ffxivextract.shaders.DefaultShader;
import com.fragmenterworks.ffxivextract.shaders.FXAAShader;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL3bc;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class ModelRenderer implements GLEventListener {

	private ArrayList<Model> models;
	private ArrayList<Model> modelsToUnload = new ArrayList<Model>();
	private float zoom = -7;
	private float panX = 0;
	private float panY = 0;
	private float angleX = 0;
	private float angleY = 0;
	
	private boolean isGlowOn = true;
	
	DefaultShader defaultShader;
	FXAAShader fxaaShader;
	BlurShader blurShader;
	BlendShader blendShader;
	
	static int currentLoD = 0;		
	
	//Matrices
	float[] modelMatrix = new float[16];
	float[] viewMatrix = new float[16];
	float[] projMatrix = new float[16];
	
	//Frame Buffer
	int rboId[] = new int[4];
	int fboId[] = new int[4];
	int fboTexture[] = new int[4];
	int canvasWidth, canvasHeight;
	
	//Frame Buffer Quad
	FloatBuffer drawQuad;
	
	public ModelRenderer()
	{
		models = new ArrayList<Model>();
		drawQuad = Buffers.newDirectFloatBuffer(new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f});
	}
	
	public ModelRenderer(Model model)
	{
		models = new ArrayList<Model>();		
		models.add(model);
		drawQuad = Buffers.newDirectFloatBuffer(new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f});
	}
	
	public ModelRenderer(ArrayList<Model> models) {
		this.models = models;		
		drawQuad = Buffers.newDirectFloatBuffer(new float[]{-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f});
	}

	public void clear()
	{
		for (int i = 0; i < models.size(); i++)		
			modelsToUnload.add(models.get(i));					
		
		models.clear();
	}
	
	public void setModel(Model model)
	{
		clear();
		models.add(model);
		model.resetVRAM();
	}
	
	public void setModels(ArrayList<Model> modelList)
	{
		for (Model m : modelList)
			m.resetVRAM();
		
		clear();
		models.addAll(modelList);
		
	}
	
	public void addModel(Model model)
	{
		models.add(model);
		model.resetVRAM();
	}
	
	public void resetMaterial() {
		
		for (Model m : models)
			m.resetVRAM();
		
	}

	public void zoom(int notches) {
		zoom += notches * 0.25f;
	}
	
	public void rotate(float x, float y)
	{
		angleX += x * 1.0f;
		angleY += y * 1.0f;
	}
	
	public void pan(float x, float y)
	{
		panX += x * 0.05f;
		panY += -y * 0.05f;
	}
	
	public void resetCamera(){
		panX = 0.0f;
		panY = 0.0f;
		angleX = 0.0f;
		angleY = 0.0f;
		zoom = -7;
	}
	
	public void toggleGlow(boolean f) {
		isGlowOn = f;
	}
	
	public void setLoD(int level)
	{
		currentLoD = level;
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL3bc gl = drawable.getGL().getGL3bc();
		
		for (Model model : models)
		{
			if (!model.isVRAMLoaded())
				model.loadToVRAM(gl);			
		}
		
		for (Model model : modelsToUnload)
		{
				model.unload(gl);
		}
		modelsToUnload.clear();

	    gl.glClearColor(0.3f,0.3f,0.3f,1.0f);
	    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
	    
	    Matrix.setIdentityM(viewMatrix, 0);
	    Matrix.setIdentityM(modelMatrix, 0);
	    Matrix.translateM(modelMatrix, 0, panX, panY, zoom);
	    Matrix.rotateM(modelMatrix, 0, angleX, 0, 1, 0);
	    Matrix.rotateM(modelMatrix, 0, angleY, 1, 0, 0);		     		   		    		    		    	 
	    
	    if (isGlowOn)
	    {
		    //Not Glowed
		    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[0]);
		    gl.glViewport(0,0, canvasWidth, canvasHeight);		    
		    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		    				    	    
		    for (Model model : models)
		    	model.render(defaultShader, viewMatrix, modelMatrix, projMatrix, gl, currentLoD, false);
		        	
		    //Glowed
		    gl.glClearColor(0.0f,0.0f,0.0f,1.0f);
		    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[1]);
		    gl.glViewport(0,0, canvasWidth/2, canvasHeight/2);	
		    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		    
		    gl.glColorMask(false, false, false, false);
		    
		    for (Model model : models)
		    	model.render(defaultShader, viewMatrix, modelMatrix, projMatrix, gl, currentLoD, false);	    	    
		    
		    gl.glColorMask(true, true, true, true);
		    
		    for (Model model : models)
		    	model.render(defaultShader, viewMatrix, modelMatrix, projMatrix, gl, currentLoD, true);	   	    
		   
		    //Blur	
		    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[2]);
		    gl.glViewport(0,0, canvasWidth/2, canvasHeight/2);		    
		    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
	    
		    gl.glUseProgram(blurShader.getShaderProgramID());	    
		    blurShader.setUniforms(gl, fboTexture[1], 1, 1, canvasWidth/2, canvasHeight/2);
		    gl.glVertexAttribPointer(blurShader.getAttribPosition(), 2, GL3.GL_FLOAT, false, 0, drawQuad);
		    gl.glEnableVertexAttribArray(blurShader.getAttribPosition());		
		    gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);
		    gl.glDisableVertexAttribArray(blurShader.getAttribPosition());
		       
		    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[1]);
		    gl.glViewport(0,0, canvasWidth/2, canvasHeight/2);		    
		    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		    
		    blurShader.setUniforms(gl, fboTexture[2], 0, 1, canvasWidth/2, canvasHeight/2);
		    gl.glVertexAttribPointer(blurShader.getAttribPosition(), 2, GL3.GL_FLOAT, false, 0, drawQuad);
		    gl.glEnableVertexAttribArray(blurShader.getAttribPosition());		
		    gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);
		    gl.glDisableVertexAttribArray(blurShader.getAttribPosition());	    	 
		    	   
		    //Combine blur/notblur
		    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[3]);
		    gl.glViewport(0,0, canvasWidth, canvasHeight);
		    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		    
		    gl.glUseProgram(blendShader.getShaderProgramID());	    
		    blendShader.setUniforms(gl, fboTexture[0], fboTexture[1], 1.5f);
		    gl.glVertexAttribPointer(blendShader.getAttribPosition(), 2, GL3.GL_FLOAT, false, 0, drawQuad);
		    gl.glEnableVertexAttribArray(blendShader.getAttribPosition());		
		    gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);
		    gl.glDisableVertexAttribArray(blendShader.getAttribPosition());
	    }
	    else
	    {
	    	gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[3]);
		    gl.glViewport(0,0, canvasWidth, canvasHeight);		    
		    gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
		    				    	    
		    for (Model model : models)
		    	model.render(defaultShader, viewMatrix, modelMatrix, projMatrix, gl, currentLoD, false);
	    }
	    
	    //FXAA
	    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	    gl.glViewport(0,0, canvasWidth, canvasHeight);
	    gl.glUseProgram(fxaaShader.getShaderProgramID());
	    fxaaShader.setTexture(gl, fboTexture[3]);
	    fxaaShader.setCanvasSize(gl, canvasWidth, canvasHeight);
	    gl.glVertexAttribPointer(fxaaShader.getAttribPosition(), 2, GL3.GL_FLOAT, false, 0, drawQuad);
		gl.glEnableVertexAttribArray(fxaaShader.getAttribPosition());		   		
	    gl.glDrawArrays(GL3.GL_TRIANGLES, 0, 6);
	    gl.glDisableVertexAttribArray(fxaaShader.getAttribPosition());
	    
	    for (Model model : models)
	    	model.stepAnimation();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {	
		if (Constants.HAVOK_ENABLED)
			HavokNative.endHavok();
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl = drawable.getGL().getGL3();      // get the OpenGL graphics context
	      gl.glClearColor(0.3f, 0.3f, 0.3f, 1.0f); // set background (clear) color
	      gl.glClearDepth(1.0f);      // set clear depth value to farthest
	      gl.glEnable(GL3.GL_DEPTH_TEST); // enables depth testing
	      gl.glDepthFunc(GL3.GL_LEQUAL);  // the type of depth test to do
	      gl.glEnable(GL3.GL_BLEND);
	      gl.glEnable(GL3.GL_CULL_FACE);
	      gl.glBlendFunc (GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);	
	      gl.glEnable(GL3.GL_TEXTURE_2D);

	      try {
			defaultShader = new DefaultShader(gl);
			fxaaShader = new FXAAShader(gl);
			blurShader = new BlurShader(gl);
			blendShader = new BlendShader(gl);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height){
			GL3bc gl = drawable.getGL().getGL3bc();  // get the OpenGL 2 graphics context
		 

			((Component)drawable).setMinimumSize(new Dimension(0,0));
			
	      if (height == 0) height = 1;   // prevent divide by zero
	      float aspect = (float)width / height;
	 
	      canvasWidth = width;
	      canvasHeight = height;
	      
	      gl.glViewport(0, 0, width, height);
	 
	      deleteFrameBuffers(gl);
	      genFrameBuffers(gl);
	      
	      for (int i = 0; i < fboId.length; i++)
	      {
	    	  if (i == 1 || i == 2)
	    		  initFrameBuffer(gl, i, canvasWidth/2, canvasHeight/2);
	    	  else
	    		  initFrameBuffer(gl, i, width, height);
	      }
	      
	      Matrix.setIdentityM(projMatrix, 0);
	      Matrix.perspectiveM(projMatrix, 0, 45.0f, aspect, 0.1f, 5000.0f);		     
	      Matrix.setIdentityM(modelMatrix, 0);
	      Matrix.setIdentityM(viewMatrix, 0);
	}
	
	private void deleteFrameBuffers(GL3 gl)
	{
		gl.glDeleteTextures(fboTexture.length, fboTexture, 0);
		gl.glDeleteFramebuffers(fboId.length, fboId, 0);
		gl.glDeleteRenderbuffers(rboId.length, rboId, 0);
	}
	
	private void genFrameBuffers(GL3 gl)
	{			
	
	    gl.glGenTextures(fboTexture.length, fboTexture,0);		    
	    gl.glGenFramebuffers(fboId.length, fboId, 0);		    
	    gl.glGenRenderbuffers(rboId.length, rboId, 0);
	}
	
	private void initFrameBuffer(GL3 gl, int id, int width, int height)
	{ 				
	    gl.glBindTexture(GL3.GL_TEXTURE_2D, fboTexture[id]);
	    
	    gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
	    gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
	    gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
	    gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
	    
	    gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0,GL3.GL_RGBA, width, height, 0,GL3.GL_RGBA, GL3.GL_UNSIGNED_BYTE, null);		     		    		    
	 
	    //Init FBO/RBO		    
	    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, fboId[id]);
	    gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, rboId[id]);
	    gl.glRenderbufferStorage(GL3.GL_RENDERBUFFER, GL3.GL_DEPTH_COMPONENT, width, height);
	    gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, 0);
	    		    
	    gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D, fboTexture[id], 0);
	    gl.glFramebufferRenderbuffer(GL3.GL_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_RENDERBUFFER, rboId[id]); 
	    
	    gl.glBindTexture(GL3.GL_TEXTURE_2D, 0);
	    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	    
	    if(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE)
	    	System.out.println("Error creating framebuffer!");
	}

}
