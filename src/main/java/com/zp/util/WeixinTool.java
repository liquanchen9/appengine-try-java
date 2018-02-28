package com.zp.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.net.ssl.HttpsURLConnection;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.openqa.selenium.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class WeixinTool {
	public static String webAppBaseDir = "";
	private static Bindings jsScope;
	private static ScriptEngine jsEngine ;
	private static Connection conn;
	private static Field URL_handler;
	private static Gson gson;
	public static byte[] img;
	static {
		try {
			URL_handler = URL.class.getDeclaredField("handler");
		}catch (Exception e1) {}
		if(!URL_handler.isAccessible()){
			URL_handler.setAccessible(true);
		}
		gson = new Gson();
		jsEngine = getJsEngine();
		jsScope = jsEngine.createBindings();
		try {
			jsEngine.eval("var window = {QRLogin:{}};", jsScope);
			//jsEngine.eval(new InputStreamReader(WeixinTool.class.getResourceAsStream("json2.js"), "UTF-8"), jsScope);
		}
		catch (Exception e) {}
		System.setProperty ("jsse.enableSNIExtension", "false");
		conn = Jsoup.connect("http://localhost");
		conn.validateTLSCertificates(false)
		.timeout(0)
		.maxBodySize(0)
		.ignoreContentType(true)
		.ignoreHttpErrors(true)
		.followRedirects(false)
		.userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");
	}
	
	
	
	 
	public static void main2(String[] args) throws Exception {
		Map<String, Object> loginData = readData();
		do{
			try {
				syncMsg(loginData,System.currentTimeMillis());
			} catch (NullPointerException e) {
				System.out.println("ignore null!!");
			}
		}while(true);
	}

	private static ScriptEngine getJsEngine() {
		ScriptEngineManager manager = new ScriptEngineManager(null);
		ScriptEngine r = manager.getEngineByName("JavaScript");
		if(r!=null)return r;
		try {
			URLClassLoader c = new URLClassLoader(new URL[] {new File(System.getProperty("java.home"),"lib\\ext\\nashorn.jar").toURI().toURL()});
			ScriptEngineFactory  factory =  (ScriptEngineFactory) c.loadClass("jdk.nashorn.api.scripting.NashornScriptEngineFactory").newInstance();
			return factory.getScriptEngine();
		} catch (MalformedURLException e) {} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// java.home=C:\Program Files\Java\jdk1.8.0_121\jre
		// C:\Program Files\Java\jdk1.8.0_121\jre\lib\ext\nashorn.jar
		return null;
	}

	private static void syncMsg(Map<String, Object> loginData, long syncBegin) throws IOException,
			InterruptedException {
		conn.request().cookie("login_frequency","2");
		conn.request().cookie("last_wxuin",conn.request().cookie("wxuin"));
		
		conn.method(Method.POST);
		conn.request().header("Accept", "application/json, text/plain, */*");
		conn.request().header("Content-Type", "application/json;charset=UTF-8");
		conn.referrer("https://wx2.qq.com/?&lang=zh_CN");
		
		conn.url("https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxinit?r=-304478574&pass_ticket=" +
				loginData.get("pass_ticket"));
		String deviceId = "e867905462123552";//return "e" + ("" + Math.random().toFixed(15)).substring(2, 17)
		hackUrlHandler(conn.request().url(),"{\"BaseRequest\":{\"Uin\":" +loginData.get("wxuin")
				+",\"Sid\":\"" +loginData.get("wxsid")+
				"\",\"Skey\":\"" +loginData.get("skey")+
				"\",\"DeviceID\":\"" +
				deviceId +
				"\"}}");
		JsonObject initData = getJsonResp();
		String myUserName = initData.get("User").getAsJsonObject().get("UserName").getAsString();
		
		String targetUserName = "";
		String targetNickName = "";
		//获取通讯录 信息
		conn.url("https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxgetcontact?pass_ticket="+loginData.get("pass_ticket")
				+"&r=" +System.currentTimeMillis() +"&seq=0&skey="+loginData.get("skey"));
		conn.method(Method.GET);
		JsonObject contactData = getJsonResp();
		JsonArray contactMemberList = contactData.getAsJsonArray("MemberList");
		for (JsonElement jsonElement : contactMemberList) {
			JsonObject memberInfo = jsonElement.getAsJsonObject();
			if(memberInfo.get("StarFriend").getAsInt()!=0){//星标用户
				targetUserName = memberInfo.get("UserName").getAsString();
				targetNickName = memberInfo.get("NickName").getAsString();
				break;
			}
		}
		System.out.println("目标id："+targetUserName);
		while(true){
			conn.url("https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxsync?sid=" +
					loginData.get("wxsid")+"&skey=" +
					loginData.get("skey")+"&lang=zh_CN&pass_ticket=" +
					loginData.get("pass_ticket"));
			conn.method(Method.POST);
			hackUrlHandler(conn.request().url(),"{\"BaseRequest\":{\"Uin\":" +loginData.get("wxuin")
					+",\"Sid\":\"" +loginData.get("wxsid")+
					"\",\"Skey\":\"" +loginData.get("skey")+
					"\",\"DeviceID\":\"" +deviceId+"\"},\"SyncKey\":"+initData.get("SyncKey")+"}");
			JsonObject syncData = getJsonResp();
			if(syncData==null){
				System.err.println("服务器返回信息失败！");
				continue;
			}
//			System.out.println("code:"+jsVar("testObj.BaseResponse.Ret"));
//			System.out.println("ErrMsg:"+jsVar("testObj.BaseResponse.ErrMsg"));
//			System.out.println("消息数量:"+jsVar("testObj.AddMsgCount")); 
//			System.out.println(jsVar("testObj.AddMsgList[0] ? testObj.AddMsgList[0].Content :'' ")); 
			//发送给我的信息
			//ToUserName:"@6a87333f54a93bd0255deb61dea7e4aff4c47c4c2ce8c8419570531e643445e0"
			JsonArray addMsgList = syncData.getAsJsonArray("AddMsgList");
			boolean msAlert = false;
			for (JsonElement jsonElement : addMsgList) {
				JsonObject msgInfo =  jsonElement.getAsJsonObject();
				if( (msgInfo.get("ToUserName").getAsString().equals(myUserName)
				&& msgInfo.get("FromUserName").getAsString().equals(targetUserName)
				&& msgInfo.get("Content").getAsString().trim().length()>0)){
					System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
							+"> 接受到("+targetNickName+")消息："+msgInfo.get("Content"));
					msAlert = true;
				}
			}
			if(msAlert){
				boolean fristTest = ms10086Alert();
				System.out.println("第一次已经尝试发送提醒短信："+fristTest);
				if(!fristTest){
					activeExecutor.schedule(new Runnable() {
						public void run() {
							System.out.println("第二次已经尝试发送提醒短信："+ms10086Alert());
						}
					},2,TimeUnit.SECONDS);
				}
			}
			initData.add("SyncKey", syncData.get("SyncCheckKey"));
			if(System.currentTimeMillis()-syncBegin>=2*60*60*1000){
				return;
			}
			Thread.sleep(1000);
		}
	}

	private static JsonObject getJsonResp() throws IOException {
		JsonObject r = null;
		try {
			String body = conn.execute().body();
			//System.out.println(body);
			r = gson.fromJson(body, JsonObject.class);
		}
		catch (Exception e) {
			Throwable t = getCase(e);
			if(t instanceof SocketTimeoutException){
				return null;
			}
			//throw new RuntimeException("服务器返回状态码不正常！");
		}
		if(r!=null&&r.get("BaseResponse").getAsJsonObject().get("Ret").getAsInt() != 0){
			throw new RuntimeException("服务器返回状态码不正常！");
		}
		return r;//BaseResponse Ret 0
	}

	private static Throwable getCase(Throwable e) {
		if(e.getCause()!=null){
			return e;
		}
		return getCase(e.getCause());
	}

	private static  Map<String, Object> saveData(Map<String, Object> loginData,
			Map<String, String> cookies) {
		File weixinLoginDataAndCookies = new File("weixin.jSerializa");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(weixinLoginDataAndCookies);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(cookies);
			oos.writeObject(loginData);
			return loginData;
		}catch (Exception e) {throw new RuntimeException(e);}finally{
			if(fos!=null)IOUtils.closeQuietly(fos);
		}
	}
	private static Map<String, Object> readData() {
		if(true){
			try {
				return login();
			}
			catch (Exception e) {}
		}
		File weixinLoginDataAndCookies = new File("weixin.jSerializa");
		if(!weixinLoginDataAndCookies.exists()){
			try {return saveData(login(),conn.request().cookies());}catch (Exception e) {throw new RuntimeException(e);}
		}
		System.out.println(System.currentTimeMillis()+" 到  "+weixinLoginDataAndCookies.lastModified());
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(weixinLoginDataAndCookies);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Map<String, String> cookies = (Map<String, String>) ois.readObject();
			conn.cookies(cookies);
			return (Map<String, Object>) ois.readObject();
		}catch (Exception e) {throw new RuntimeException(e);} finally{
			if(fis!=null)IOUtils.closeQuietly(fis);
		}
	}

	private static Map<String, Object> login() throws IOException, ScriptException,
			InterruptedException {
		conn.url("https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxstatreport?fun=new&lang=zh_CN");
		System.out.println(conn.execute().body());
		
		conn.url("https://login.wx.qq.com/jslogin?appid=wx782c26e4c19acffb&redirect_uri=https%3A%2F%2Fwx.qq.com%2Fcgi-bin%2Fmmwebwx-bin%2Fwebwxnewloginpage&fun=new&lang=zh_CN&_=" +
				System.currentTimeMillis());
		jsExecute(conn.execute().body());
	 
	 
		String qrImgUrl = "https://login.weixin.qq.com/qrcode/"+jsVar("window.QRLogin.uuid");
		byte[] qrImg = conn.url(qrImgUrl).execute().bodyAsBytes();
//		if(webAppBaseDir!=null && webAppBaseDir.trim().length()>0 ) {
			writeImage2File(qrImg);
//		}		
		//print(new ByteArrayInputStream(qrImg), true);
		System.out.println("已输出二维码!可以扫了");
		
		double code = 408; 
		do{
			//等待扫描成功!!
			conn.url("https://login.wx.qq.com/cgi-bin/mmwebwx-bin/login?loginicon=true&uuid=" +
					jsVar("window.QRLogin.uuid") +
					"&tip=0&r=-221190867&_=" +
					System.currentTimeMillis());
			jsExecute(conn.execute().body());
			code = ((Number)jsVar("window.code")).doubleValue();
			System.out.println("code>>>"+code);
			System.out.println(code==408?"请扫码!!":code==201?"请在手机确认登录!":code==200?"扫码成功":"二维码已经失效!");
			if(code!=408){
				Thread.sleep(1000);
			}
		}while(code!=200);
		
		
		String loginPageUrl = jsVar("window.redirect_uri");
		conn.url(loginPageUrl);
		conn.request().parser(Parser.xmlParser());
		Response resp = conn.execute();
		Document doc = resp.parse();
		conn.cookies(resp.cookies());
		conn.request().parser(Parser.htmlParser());
		
		Map<String,Object> loginData = new HashMap<String, Object>();
		loginData.put("ret", doc.select("ret").text().trim());
		loginData.put("message", doc.select("message").text().trim());
		loginData.put("skey", doc.select("skey").text().trim());
		loginData.put("wxsid", doc.select("wxsid").text().trim());
		loginData.put("wxuin", doc.select("wxuin").text().trim());
		loginData.put("pass_ticket", doc.select("pass_ticket").text().trim());
		loginData.put("isgrayscale", doc.select("isgrayscale").text().trim());
		return loginData;
	}
	private static void writeImage2File(byte[] qrImg) throws IOException {
		img =  (qrImg);
	}
	private static ScheduledExecutorService activeExecutor;
	static {
		activeExecutor = Executors.newScheduledThreadPool(1);
		activeExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					Jsoup.connect("https://mbusihall.sh.chinamobile.com:1443/cmbh3/")
					.method(Method.GET)
					.maxBodySize(0)
					.timeout(0)
					.validateTLSCertificates(false)
					.ignoreHttpErrors(true)
					.ignoreContentType(true)
					.userAgent("上海移动客户端  4.1.8 rv:20170534 (iPhone; iOS 10.3.2; zh_CN)")
					.cookie("JSESSIONID", "b0fcf3c8005d92b223eadf3c0836")
					.cookie("WT_FPC", "id=255cadd7f0ee8f8f8ec1493191804839:lv=1499393950449:ss=1499393950186")
					.cookie("WT_USER_ID", "1145-213546e6095c30a").execute().body();
				}
				catch (IOException e) {}
			}
		},  0,  30, TimeUnit.MINUTES);
	}
	public static boolean ms10086Alert() {
		
//		POST /cmbh3/user/onlyLogin? HTTP/1.1
//				Host: mbusihall.sh.chinamobile.com:1443
//				Accept: application/json
//				Content-Type: application/json;charset=UTF-8
//				Cookie: JSESSIONID=b0fcf3c8005d92b223eadf3c0836; JSESSIONID=b0fcf3c8005d92b223eadf3c0836; WT_FPC=id=255cadd7f0ee8f8f8ec1493191804839:lv=1499393950449:ss=1499393950186; WT_USER_ID=1145-213546e6095c30a
//				User-Agent: ä¸æµ·ç§»å¨æä¸è¥ä¸å 4.1.8 rv:20170534 (iPhone; iOS 10.3.2; zh_CN)
//				Content-Length: 440
//				Accept-Encoding: gzip
//				Connection: close
//
//				8fAkFYwU/g8cZFWZh13Ad22kiGRmXX9QCsgvqsAw4wfZmobtrQQLo/68Vs7GpRjU2V+8trfe5XsPRDemzQPQjA8LyHWsyLj/ESyulG+w3vTUhOO5WO3GswYZ/nuGJTDs84VJ1n9CNmgOFM8puvi2GZGlkSrTg1mrX3H1QaVHVK/C0E81cebbEHrqi7EYOP6WOteHIwiy9pAuQQ7efaGJcA4oA6FJ1PpP7rIFo73RhoUDauiNKcIbwla3exvDlqQ5E7RSVDgIMFVqXTIov9PntIlL+rP00wJ3GRM2TYNwsNqCMvUUl5TGo+Qm29jhcZ6fckPjm+8Yk4QModZTUTx1Hcnh4oaDH44R8yTk3LTpii1SdstI2n9AL6J7r80Xf/NaTRWunsoi7OCk6upHHJBtfka+6+w3n+4EASb3pHWBBtw0tOL8yFTqLw==
		try {
			Connection conn = Jsoup.connect("https://mbusihall.sh.chinamobile.com:1443/cmbh3/user/onlyLogin?")
					.method(Method.POST)
					.maxBodySize(0)
					.timeout(0)
					.validateTLSCertificates(false)
					.ignoreHttpErrors(true)
					.ignoreContentType(true)
					.userAgent("上海移动客户端  4.1.8 rv:20170534 (iPhone; iOS 10.3.2; zh_CN)")
					.cookie("JSESSIONID", "b0fcf3c8005d92b223eadf3c0836")
					.cookie("WT_FPC", "id=255cadd7f0ee8f8f8ec1493191804839:lv=1499393950449:ss=1499393950186")
					.cookie("WT_USER_ID", "1145-213546e6095c30a");
			String body = "8fAkFYwU/g8cZFWZh13Ad22kiGRmXX9QCsgvqsAw4wfZmobtrQQLo/68Vs7GpRjU2V+8trfe5XsPRDemzQPQjA8LyHWsyLj/ESyulG+w3vTUhOO5WO3GswYZ/nuGJTDs84VJ1n9CNmgOFM8puvi2GZGlkSrTg1mrX3H1QaVHVK/C0E81cebbEHrqi7EYOP6WOteHIwiy9pAuQQ7efaGJcA4oA6FJ1PpP7rIFo73RhoUDauiNKcIbwla3exvDlqQ5E7RSVDgIMFVqXTIov9PntIlL+rP00wJ3GRM2TYNwsNqCMvUUl5TGo+Qm29jhcZ6fckPjm+8Yk4QModZTUTx1Hcnh4oaDH44R8yTk3LTpii1SdstI2n9AL6J7r80Xf/NaTRWunsoi7OCk6upHHJBtfka+6+w3n+4EASb3pHWBBtw0tOL8yFTqLw==";
			hackUrlHandler(conn.request().url(), body);
			(conn.execute().body()).trim();
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
		return true;
	}
	
	private static void hackUrlHandler(final URL url,final String body) {
		hackUrlHandler(url, body,"application/json;charset=UTF-8");
	}
	private static void hackUrlHandler(final URL url,final String body,final String contentType) {
		try {
			final URLStreamHandler oldHandler = (URLStreamHandler) URL_handler.get(url);
			URL_handler.set(url,Enhancer.create(oldHandler.getClass(), new MethodInterceptor(){
				public Object intercept(Object obj,
						java.lang.reflect.Method method, Object[] args,
						MethodProxy proxy) throws Throwable {
					final Object oldRet = invoke(method, oldHandler, args);
					if(!method.getName().equals("openConnection")){
						return oldRet;
					}
					
					Enhancer enhancer = new Enhancer();
					enhancer.setSuperclass(oldRet instanceof HttpsURLConnection ? HttpsURLConnection.class:HttpURLConnection.class);
					enhancer.setCallback(new MethodInterceptor(){
						public Object intercept(Object obj,
								java.lang.reflect.Method method, Object[] args,
								MethodProxy proxy) throws Throwable {
							HttpURLConnection conn = (HttpURLConnection) oldRet;
							if(method.getName().equals("connect")){
								conn.setRequestProperty("Content-Type", contentType);
							}
							Object oldRet2 = invoke(method, oldRet, args);
							if(!method.getName().equals("getOutputStream")){
								return oldRet2;
							}
							PrintStream out = new PrintStream((OutputStream)oldRet2, false, "UTF-8");
							out.print(body);
							out.close();
							return new ByteArrayOutputStream();
						}});
					return enhancer.create(new Class[]{URL.class}, new Object[]{url});
				}
			 }));
		}catch (Exception e) {}
	}
	
	private static <T> T invoke(
			java.lang.reflect.Method method,
			Object obj, Object ... args) {
		try {
			if(!method.isAccessible()){
				method.setAccessible(true);
			}
			return (T) method.invoke(obj, args);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> T jsVar(String js) throws ScriptException {
		return (T) jsEngine.eval(js,jsScope);
	}

	private static void jsExecute(String js) throws ScriptException {
		jsEngine.eval(js,jsScope);
	}
	
	
	private static void print(InputStream is,boolean toFile) throws IOException {
		//398 141  ---- 631 375
		BufferedImage qrcode = ImageIO.read(is);
		qrcode = qrcode.getSubimage(30, 30, 370, 370);
		int newh=64,neww=64;
		Image scaledQRcode = qrcode.getScaledInstance(newh, neww, 1);
		qrcode = new BufferedImage(newh, neww, BufferedImage.TYPE_INT_RGB);
		qrcode.createGraphics().drawImage(scaledQRcode, 0, 0, null);
		PrintStream out = toFile? new PrintStream(new File("/qrcode.log"),"UTF-8"):System.out;
		out.println();
		for (int i = 0; i < qrcode.getWidth(); i++) {
			for (int j = 0; j < qrcode.getHeight(); j++) {
				int rgb = -qrcode.getRGB(i, j);
				if(16777216-rgb>rgb-1){
					out.print("　");
				}else{
					out.print("█");
				}
			}
			out.println();
		}
		out.flush();
	}
	
}

/*
Request URL:https://file.wx2.qq.com/cgi-bin/mmwebwx-bin/webwxuploadmedia?f=json
Request Method:POST
Status Code:200 OK
Remote Address:58.251.61.163:443
Referrer Policy:no-referrer-when-downgrade
 
Accept:* /* 
Accept-Encoding:gzip, deflate, br
Accept-Language:zh-CN,zh;q=0.8
Cache-Control:no-cache
Connection:keep-alive
Content-Length:38841
Content-Type:multipart/form-data; boundary=----WebKitFormBoundarypgWCAwD9ExGQ3bmO
Host:file.wx2.qq.com
Origin:https://wx2.qq.com
Pragma:no-cache
Referer:https://wx2.qq.com/
User-Agent:Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36


------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="id"

WU_FILE_0
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="name"

Lighthouse.jpg
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="type"

image/jpeg
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="lastModifiedDate"

Tue Jul 14 2009 13:32:31 GMT+0800 (中国标准时间)
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="size"

561276
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="chunks"

2
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="chunk"

1
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="mediatype"

pic
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="uploadmediarequest"

{"UploadType":2,"BaseRequest":{"Uin":1934018523,"Sid":"xds8DjvAjwtIuJ2N","Skey":"@crypt_6ce376ef_1a830b5d32400d14957714146c0df493","DeviceID":"e283973453983393"},"ClientMediaId":1500274554751,"TotalLen":561276,"StartPos":0,"DataLen":561276,"MediaType":4,"FromUserName":"@458a40f7867384482ac76d17fa0dae90c8454f9afa2c68adecae3a045dfc6ffb","ToUserName":"filehelper","FileMd5":"8969288f4245120e7c3870287cce0ff3"}
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="webwx_data_ticket"

gScBoEAWbhTWBQIFWXJx8yax
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="pass_ticket"

8FcCCc2pC2B/7t8vrs0jLuTvd0iNhIx3lh3XcOic1Dn5zWsxJ+RHuk2weABWKTNh
------WebKitFormBoundarypgWCAwD9ExGQ3bmO
Content-Disposition: form-data; name="filename"; filename="Lighthouse.jpg"
Content-Type: application/octet-stream


------WebKitFormBoundarypgWCAwD9ExGQ3bmO--


HTTP/1.1 200 OK
Server: nginx
Date: Mon, 17 Jul 2017 06:56:54 GMT
Content-Length: 789
Connection: keep-alive
Access-Control-Allow-Origin: *

{
"BaseResponse": {
"Ret": 0,
"ErrMsg": ""
}
,
"MediaId": "@crypt_35273310_86d3009c75096d41af3c3d3cfc7f7541fb34fbd7d2af9a53a8b39628e57b68900cdabd66f2629fdbc133549ce0f4313249e8ba9cee69163c00bc81dcc9ccfbb4388fd5da5ecf9bd7a8664f1a048c06384ecb8fee2c897d0d77c5141aa5032244e0301108b8a0db98f4f001313281b8a18b9a5c15baad85e4c64f36debb70ff3c32db62569bab623d41bd70322c44942f0f52b360809003e0dc715ce53518bf8353becf72a6fe36a3b52c54a7c51856e7b46c8239a42f38a30e8bf5b416b623f34a94db38b4f61e8ab885c79c06c56785161bbf3bbc0f2ace8e33158908b372b571d51bf38ea1a8997addab08561fdf7054df35e803f8b3e51208ef864705cc064de17b53d5a32f6f22e201156ad5f348bf73cdd8b74b8a1ec1469e77dbb316bffb1c082d4076209f113b8bb1c17ea5ca20caff2716bbd569ed7781bfb8fada59",
"StartPos": 561276,
"CDNThumbImgHeight": 75,
"CDNThumbImgWidth": 100
}

*/
/*
POST /cgi-bin/mmwebwx-bin/webwxsendmsgimg?fun=async&f=json&pass_ticket=8FcCCc2pC2B%252F7t8vrs0jLuTvd0iNhIx3lh3XcOic1Dn5zWsxJ%252BRHuk2weABWKTNh HTTP/1.1
Host: wx2.qq.com
Connection: keep-alive
Content-Length: 1029
Pragma: no-cache
Cache-Control: no-cache
Accept: application/json, text/plain, * /*
Origin: https://wx2.qq.com
User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36
Content-Type: application/json;charset=UTF-8
Referer: https://wx2.qq.com/
Accept-Encoding: gzip, deflate, br
Accept-Language: zh-CN,zh;q=0.8
Cookie: pgv_pvi=4795313152; webwxuvid=685d3e8aafc43523282cc69dc593aa1ed2add2ca5acb3f051f6804982a0c93c89dca385b3f8c9b449abeb14b870f5ade; pgv_si=s2094930944; MM_WX_NOTIFY_STATE=1; MM_WX_SOUND_STATE=1; mm_lang=zh_CN; webwx_auth_ticket=CIsBEIqP4MEJGoABno7bMwlCfkzl3UR/1vsA/n6I/ANm95wCSw8RbkeXrn7HmnC+fHc3VFbLhvZA9qG/PcUqwu+1jP66KE8imIaNEKhKpHKogkggaJ3fRP3IZGgaHRFUUaECwnnGPvZ/IBVRoLfb8lLjmzKeGWx3HaAoS4EgqIU8yyjNntC4JP6RUq4=; login_frequency=1; last_wxuin=1934018523; wxloadtime=1500274586_expired; wxpluginkey=1500252122; wxuin=1934018523; wxsid=xds8DjvAjwtIuJ2N; webwx_data_ticket=gScBoEAWbhTWBQIFWXJx8yax

RequestBody:
{"BaseRequest":{"Uin":1934018523,"Sid":"xds8DjvAjwtIuJ2N","Skey":"@crypt_6ce376ef_1a830b5d32400d14957714146c0df493","DeviceID":"e689066935887328"},
"Msg":{"Type":3,"MediaId":"@crypt_35273310_86d3009c75096d41af3c3d3cfc7f7541fb34fbd7d2af9a53a8b39628e57b68900cdabd66f2629fdbc133549ce0f4313249e8ba9cee69163c00bc81dcc9ccfbb4388fd5da5ecf9bd7a8664f1a048c06384ecb8fee2c897d0d77c5141aa5032244e0301108b8a0db98f4f001313281b8a18b9a5c15baad85e4c64f36debb70ff3c32db62569bab623d41bd70322c44942f0f52b360809003e0dc715ce53518bf8353becf72a6fe36a3b52c54a7c51856e7b46c8239a42f38a30e8bf5b416b623f34a94db38b4f61e8ab885c79c06c56785161bbf3bbc0f2ace8e33158908b372b571d51bf38ea1a8997addab08561fdf7054df35e803f8b3e51208ef864705cc064de17b53d5a32f6f22e201156ad5f348bf73cdd8b74b8a1ec1469e77dbb316bffb1c082d4076209f113b8bb1c17ea5ca20caff2716bbd569ed7781bfb8fada59",
"Content":"","FromUserName":"@458a40f7867384482ac76d17fa0dae90c8454f9afa2c68adecae3a045dfc6ffb",
"ToUserName":"filehelper","LocalID":"15002745547520720","ClientMsgId":"15002745547520720"},"Scene":0}
 
 
 
HTTP/1.1 200 OK
Connection: keep-alive
Content-Type: text/plain
Content-Length: 112
{
"BaseResponse": {
"Ret": 0,
"ErrMsg": ""
}
,
"MsgID": "1208214496845458075",
"LocalID": "15002745547520720"
}


 */

