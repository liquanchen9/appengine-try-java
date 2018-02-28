package com.zp;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.zp.util.WeixinTool;

@SpringBootApplication(scanBasePackages = "com.zp")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@EnableAsync
@EnableScheduling
@Controller
public class Application {
	
	public static void main(String[] args) {
		new SpringApplicationBuilder(Application.class).web(true).run(args);
	}
	
	@RequestMapping("/qr-img")
	public  ResponseEntity<byte[]> qrImg(){
		return new ResponseEntity<byte[]>(WeixinTool.img, HttpStatus.OK);
	}
	
	@Autowired
	private AsyncIniter initer;
	
	@PostConstruct
	public void init() throws Exception {
		System.out.println("init beging");
		initer.init();
		System.out.println("init end");
	}
}

@Component
class AsyncIniter {
	
	@Async
	public void init() throws Exception {
		WeixinTool.main2(null);
	}
	
}