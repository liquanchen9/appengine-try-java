package com.zp.util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;

public abstract class WebDriverWaits {
	
	public static <T> T waitBy(WebDriver driver, long timeOutInSeconds,final By by){
		return (T) new WebDriverWait(driver, timeOutInSeconds).until(new Function<WebDriver,T>() {
			@Override@SuppressWarnings("unchecked")
			public T apply(WebDriver driver) {
				try {
					WebElement ele = driver.findElement(by);
					if( ele!=null && (ele.getTagName().toLowerCase().equals("script") || ele.isDisplayed())){
						return (T) ele;
					};
				}catch (NoSuchElementException e) {}
				return null;
			}
		});
	}
}
