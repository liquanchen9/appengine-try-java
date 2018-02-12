package com.zp.util;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.sun.jna.platform.win32.Crypt32Util;


public class WeixinWebDriverTool {

	public static void main(String[] args) throws Exception {
//		String cookieFilePath = "C:\\Users\\cx\\AppData\\Local\\360Chrome\\Chrome\\User Data\\Default\\Cookies";
//		Map<String,String> cookies_ = chromeCookies(cookieFilePath);  
//		System.out.println(cookies_);
//		if(true)return;
		DesiredCapabilities chrome = DesiredCapabilities.chrome();
		// Map<String,Object> object = new HashMap<>();
		// object.put("args",Arrays.asList("--start-maximized","--test-type",
		// "--ignore-certificate-errors","no-default-browser-check"));
		// chrome.setCapability("chromeOptions", object);
		WebDriver webDriver = createRometeWebDriver(new URL(
				"http://localhost:4444/wd/hub/"), chrome);
		
		webDriver.get("https://wx.qq.com/");
		//WebElement qrcodeImg = webDriver.findElement(By.cssSelector(".login_box .qrcode img"));
		File qrcodeFile = ((TakesScreenshot)webDriver).getScreenshotAs(OutputType.FILE);
		System.out.println("请扫一扫！");
		print(qrcodeFile, true);
		//Desktop.getDesktop().browse(new File("/qrcode.log").toURI());
		WebElement scriptElement = WebDriverWaits.waitBy(webDriver, 24*60*60, By.cssSelector("script[src^='https://webpush.wx2.qq.com/cgi-bin/mmwebwx-bin/synccheck']"));
		String url = scriptElement.getAttribute("src");
		Map<String,String> urlParams = parseUrlParams(url);
		url = "https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxsync?sid="+urlParams.get("sid")+"&skey="+urlParams.get("skey");
		System.out.println("准备链接 ："+url);
		Connection conn = Jsoup.connect(url);
		Set<Cookie> jsCookies = webDriver.manage().getCookies();
		for (Cookie cookie : jsCookies) {
			conn.cookie(cookie.getName(), cookie.getValue());
			System.out.println("准备cookie： "+cookie.getName()+" ===> "+cookie.getValue());
		}
		Capabilities cap = (((RemoteWebDriver)webDriver).getCapabilities());
		String chromeDir = (String) ((Map)cap.getCapability("chrome")).get("userDataDir");
		Thread.sleep(5000);
		Map<String,String> cookies = chromeCookies(chromeDir+"\\Default\\Cookies");
		for (Entry<String,String> cookie : cookies.entrySet()) {
			conn.cookie(cookie.getKey(), cookie.getValue());
			System.out.println("准备cookie： "+cookie.getKey()+" ===> "+cookie.getValue());
		}
		conn.ignoreContentType(true)
		.header("Accept", "application/json, text/plain, */*")
		.header("Origin", "https://wx2.qq.com")
		.header("Content-Type", "application/json;charset=UTF-8")
		.userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
		.referrer("https://wx2.qq.com/")
		.method(Method.POST)
		.maxBodySize(0);
		while(true){ 
			System.out.println(conn.execute().body());
			Thread.sleep(5000);
		}
		//https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxsync?sid=jllHVeXTXSaugyS7&skey=@crypt_6ce376ef_ddce29c60550584525e0675dac131c81
		//Query String: sid=jllHVeXTXSaugyS7&skey=@crypt_6ce376ef_ddce29c60550584525e0675dac131c81
		//		Accept:application/json, text/plain, */*
		//		Accept-Encoding:gzip, deflate, br
		//		Accept-Language:zh-CN,zh;q=0.8
		//		Cache-Control:no-cache
		//		Connection:keep-alive
		//		Content-Length:443
		//		Content-Type:application/json;charset=UTF-8
		//		Cookie:RK=0N07oK6Kfm; pgv_pvi=5260233728; o_cookie=609018423; pgv_pvid=2473889769; pac_uid=1_609018423; ptcz=25e71cef786721fab141de5d19c2b8e5219a118470628b2a72cc9cea69247832; pt2gguin=o0609018423; pgv_si=s4634620928; webwxuvid=685d3e8aafc43523282cc69dc593aa1ea35588198789a8a0f3c13871a561d902dcc5c3c0fc3de8d74deff7cc1d192d9e; webwx_auth_ticket=CIsBEOWR5MMLGoABnbkAwtwvfibKKjfeE2wCzn6I/ANm95wCSw8RbkeXrn6rrs5LKq04OMYDp8qN+N/k+HpE4RQBx8P+gtCe4h7AjM2PGrj0yj0oZPq7BN+7RdHZCdhNN/OBrbLKwRZEZEcC1SHvlvdnoBlab3teIuPsDj7MXw5DYLPKurIFmErsXSc=; mm_lang=zh_CN; MM_WX_NOTIFY_STATE=1; MM_WX_SOUND_STATE=1; wxloadtime=1498460825_expired; wxpluginkey=1498437722; wxuin=1934018523; wxsid=jllHVeXTXSaugyS7; webwx_data_ticket=gSeWzr+U0zCSOD6Dmd/eB6Ww
		//		Host:wx2.qq.com
		//		Origin:https://wx2.qq.com
		//		Pragma:no-cache
		//		Referer:https://wx2.qq.com/
		//		User-Agent:Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36
		
//		准备链接 ：https://wx2.qq.com/cgi-bin/mmwebwx-bin/webwxsync?sid=Lc5lKEEutAv5LigK&skey=%40crypt_6ce376ef_1ba417643b8d58f4028d050401954059
//			准备cookie： mm_lang ===> zh_CN
//			准备cookie： wxsid ===> Lc5lKEEutAv5LigK
//			准备cookie： webwx_data_ticket ===> gSdH5u+531SSxlLI8DsB2NDE
//			准备cookie： webwx_auth_ticket ===> CIsBEN2U7JsKGoABbLi1AKfQ0+dI/4BDYToLAn6I/ANm
//			准备cookie： MM_WX_SOUND_STATE ===> 1
//			准备cookie： wxuin ===> 1934018523
//			准备cookie： MM_WX_NOTIFY_STATE ===> 1
//			准备cookie： pgv_pvi ===> 2781205504
//			准备cookie： wxloadtime ===> 1498559745
//			准备cookie： webwxuvid ===> 685d3e8aafc43523282cc69dc593aa1e2644779738be8a3055329a397fe91276656ba3288f90166eded21a93c0bf6b5f
//			准备cookie： pgv_si ===> s2008954880
//			准备cookie： mm_lang ===> zh_CN
//			准备cookie： MM_WX_SOUND_STATE ===> 1
//			准备cookie： MM_WX_NOTIFY_STATE ===> 1
//			准备cookie： pgv_pvi ===> 2781205504
//			准备cookie： pgv_si ===> s2008954880
	}

	private static Map<String, String> chromeCookies(String cookieFilePath) throws ClassNotFoundException {
		Map<String, String> rst = new HashMap<>();
		System.out.println("cookie路径："+cookieFilePath);
		if(!new File(cookieFilePath).exists())return rst;
		// load the sqlite-JDBC driver using the current class loader  
	    Class.forName("org.sqlite.JDBC");  
	  
	    java.sql.Connection connection = null;  
	    try  
	    {  
	    // create a database connection  
	    //  String cookieFilePath = "C:\\Users\\cx\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\Cookies";
		connection = DriverManager.getConnection("jdbc:sqlite:" +
	      		cookieFilePath);  
	      Statement statement = connection.createStatement();  
	      statement.setQueryTimeout(30);  // set timeout to 30 sec.  
	  
	      ResultSet rs = statement.executeQuery("select * from cookies");  
	      while(rs.next())  
	      {  
	        // read the result set  
	        //System.out.println("name = " + rs.getString("name"));  
	    	//	byte[] bytes = rs.getBytes("encrypted_value");
	        //System.out.println("value = " + decrypt(bytes));  
	        String value =  decrypt(rs.getBytes("encrypted_value"));
	        if(value!=null){
	        	rst.put(rs.getString("name"), decrypt(rs.getBytes("encrypted_value")));
	        }
	      }  
	    }  
	    catch(SQLException e)  
	    {  
	      // if the error message is "out of memory",   
	      // it probably means no database file is found  
	      System.err.println(e.getMessage());  
	    }  
	    finally  
	    {  
	      try  
	      {  
	        if(connection != null)  
	          connection.close();  
	      }  
	      catch(SQLException e)  
	      {  
	        // connection close failed.  
	        System.err.println(e);  
	      }  
	    }
	    return rst;
	}
	
	/**
	 * @link https://github.com/benjholla/CookieMonster/blob/master/CookieMonster/src/main/java/cmonster/browsers/ChromeBrowser.java
     * Decrypts an encrypted cookie
     * @param encryptedCookie
     * @return
     */
    protected static String decrypt(byte[] encryptedValue) {
        byte[] decryptedBytes = null;
        try {
            decryptedBytes = Crypt32Util.cryptUnprotectData(encryptedValue,1|4);
        } catch (Throwable e){
        	e.printStackTrace();
        	System.out.println("window api 解密失败！！");
        	System.exit(0);
            decryptedBytes = null;
        } 
        if(decryptedBytes == null){
            return null;
        }      
        return new String(decryptedBytes) ;
    }


	private static Map<String, String> parseUrlParams(String url) {
		Map<String, String> params = new HashMap<String, String>();
		int  start = url.indexOf("?")+1;
		while(start>0){
			int end = url.indexOf("=",start);
			String name = url.substring(start,end);
			start = url.indexOf("&",start)+1;
			String value;
			if(start>0){
				value = url.substring(end+1,start-1);
			}else{
				value = url.substring(end+1);
			}
			params.put(name, value);
		}
		return params;
	}

	/*
	 *
	 Cookie:RK=0N07oK6Kfm; 
	 o_cookie=609018423; 
	 pgv_pvid=2473889769; 
	 pac_uid=1_609018423; 
	 ptcz=25e71cef786721fab141de5d19c2b8e5219a118470628b2a72cc9cea69247832; 
	 pt2gguin=o0609018423; 
	 pgv_si=s4634620928; 
	 webwxuvid=685d3e8aafc43523282cc69dc593aa1ea35588198789a8a0f3c13871a561d902dcc5c3c0fc3de8d74deff7cc1d192d9e; 
	 wxloadtime=1498460825_expired; 
	 wxpluginkey=1498437722; 
	
	  
	 
请扫一扫！
准备链接 ：https://webpush.wx2.qq.com/cgi-bin/mmwebwx-bin/synccheck?r=1498475865914&skey=%40crypt_6ce376ef_1c84f289727687b15e8cb305cd3a6e00&sid=OUKZ982FKGxhcNfs&uin=1934018523&deviceid=e086514073697513&synckey=1_658658460%7C2_658659843%7C3_658658804%7C1000_1498470481&_=1498475865133
准备cookie： mm_lang ===> zh_CN
准备cookie： wxsid ===> OUKZ982FKGxhcNfs
准备cookie： webwx_data_ticket ===> gSeNjhOXccnCVucdI2b/1/FZ
准备cookie： webwx_auth_ticket ===> CIsBEP+EjIUEGoABC7GILPFYvmI6PA/LlsYI3H6I/ANm95wCSw8RbkeXrn5cMfHFnMybH3cKTmy6DuleQk2G13jq82VKh5uNtzsOa4mXmEoqCnziCHOmP3GLlRuWdbbZML3qy14ifUQTTBmBuzsYca80SiGrGvRFKMzegz7MXw5DYLPKurIFmErsXSc=
准备cookie： MM_WX_SOUND_STATE ===> 1
准备cookie： wxuin ===> 1934018523
准备cookie： MM_WX_NOTIFY_STATE ===> 1
准备cookie： pgv_pvi ===> 1120516096
准备cookie： wxloadtime ===> 1498475908
准备cookie： webwxuvid ===> 685d3e8aafc43523282cc69dc593aa1e3935c05d33aebe78d497ae016a0c589cac07e776b53080a33751422c8441abfc
准备cookie： pgv_si ===> s3091869696
test!!


	 * */
	private static void print(File qrcodeFile,boolean toFile) throws IOException {
		//398 141  ---- 631 375
		BufferedImage qrcode = ImageIO.read(qrcodeFile);
		qrcode = qrcode.getSubimage(398, 141, 631-398, 375-141);
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
	private static WebDriver createRometeWebDriver(URL url, Capabilities capabilities)
			throws MalformedURLException {
		RemoteWebDriver webDriver = new RemoteWebDriver(url, capabilities);
		webDriver.manage().window().setSize(new Dimension(2000, 8000));
		Runtime.getRuntime().addShutdownHook(new WebDriverQuitor(webDriver));
		return webDriver;
	}
	private static class WebDriverQuitor extends Thread {
		private WebDriver webDriver;

		public WebDriverQuitor(WebDriver webDriver) {
			super("WebDriverQuitor");
			this.webDriver = webDriver;
		}

		public void run() {
			// scoped_dir
			webDriver.quit();
		}
	}
	 
}
 class OS {

    public static String getOsArchitecture() {
        return System.getProperty("os.arch");
    }

    public static String getOperatingSystem() {
        return System.getProperty("os.name");
    }

    public static String getOperatingSystemVersion() {
        return System.getProperty("os.version");
    }

    public static String getIP() throws UnknownHostException {
        InetAddress ip = InetAddress.getLocalHost();
        return ip.getHostAddress();
    }

    public static String getHostname() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName(); 
    }

    public static boolean isWindows() {
        return (getOperatingSystem().toLowerCase().indexOf("win") >= 0);
    }

    public static boolean isMac() {
        return (getOperatingSystem().toLowerCase().indexOf("mac") >= 0);
    }

    public static boolean isLinux() {
        return (getOperatingSystem().toLowerCase().indexOf("nix") >= 0 || getOperatingSystem().toLowerCase().indexOf("nux") >= 0 || getOperatingSystem().toLowerCase().indexOf("aix") > 0 );
    }

    public static boolean isSolaris() {
        return (getOperatingSystem().toLowerCase().indexOf("sunos") >= 0);
    }

}
