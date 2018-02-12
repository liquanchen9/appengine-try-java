package com.zp.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLStreamHandler;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public abstract class CglibTool {
	
	
	public static void main(String[] args) throws IOException {
		
	 
		WeixinTool.ms10086Alert();
		
//		System.out.println(Arrays.asList(c.getConstructors()));
//		System.out.println(Arrays.asList(c.getDeclaredMethods()).toString().replace(',', '\n'));
	}
	
	
}
