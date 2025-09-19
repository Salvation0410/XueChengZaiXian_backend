package com.xuecheng.orders.config;
 /**
 * @description 支付宝配置参数
 * @author Mr.M
 * @date 2022/10/20 22:45
 * @version 1.0
 */
 public class AlipayConfig {
  // 商户appid
	public static String APPID = "9021000154656443";
  // 私钥 pkcs8格式的
	//public static String RSA_PRIVATE_KEY = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDG3EBS6dKGSJjVzZNmETwK8JTWe5+O0MlwVMPvQWGuZf6clbZAp9YG6BCunKUglAWHZboRFMB2VLRsGKL8TaGwkFQmabu2ArikZe0cf9+UVm97P8JALfBI8iQ/mOsh/n7wht72E+sIsObyBUhLHxv7eVdBD5FwssELeWJe1fjLD2bSoC8seQp6xKg6IECnLQZdSEoxWdd12Y/7iMHXZcqCMUNTuqnJO3mZgcsb3ujx7jcquCItTE0kfntFzgbTj323EmYohyzHoNyFeTIrs8ABIN3QgLXc/ThtPp6RK+F3XuN5BYHxO7ovgR+0WVNcCa1/1DkUEf/NeBej1qdQG0ffAgMBAAECggEAToA9yD1ThPz9kFER4dXl3O+x4aV1jDwxeO2NV1J5DA5pX0jeZFGc9KcBhtyBdwXX1OY8eGe3vj6b5pwnIvBGZtvizDMrDaOvXf56hqiYCwfABGhb91frJfCojI6CK646UBr9wyiDvK+Qy3N4YWdLdY8l+aH6qQF3oV7ujT9sckPUUFMHaCxgegZuKBV96RK3lbM6Gy8TEdhzLPkU95hzL+1Wd3UXPOT2JitBhwpjK3MMF6Oyur0oXVO/kek5XLRZiD+IqjZwbju7C2QC0B6z6jKCTvOxNP02gX9ZsHsVO4DMd+bb9atY81dJ3EJ+7pHKLtyPu6gzaO+8TJmCAj/r+QKBgQD3VmeWGJZx6sIKAvoO1+EZTw7ldNKXdchEEnQpGomory4VZ0N3+oyjePz1FEb0QZKGV/WlLW/fyjo78N+1EmToVtv7EQeI9lqBl0egVaoT7bn4rud8KbYQYTryQAGonisHDTehdk9erTQH3OfthSwbBiMNw2mbVBDGtSGTkFgZZQKBgQDN0zTzA3sf8gOBveBiNjNy00exrYZ8TX4k4SHMKBm46T3p5+DrHaer/eQzF2/MfvqNt9EoxtNw9aaMPbLs5JqlFWTZAr9KKOzjthZGzJKkUaOMRHCQcfmcw6bdHLjsnppEacrPi06IMGpkFsTfSv+5HqiGVtzdx1teyFDP/SIp8wKBgQDhdp00oLM7otFAyRfjQf0K0ht1JTn3IUHuc5f6BBvqbc4napuBGh7rwZVtXw+TBtcnqbTd3n511OWi0F9MszjS5qLeydZBjrhQG3QKfyxrwC/ftqiDcY5qfgd63sfkSlJUukK056FQX7jnVgMBbH0ZIU4A9Nom4snsKR8zYcg9VQJ/MQsiksVBkVsZetDDpqNDnxi4/J1cbkIIzMDYTM1BU84/8nxOG7f1PAZDWek7un6HNT2XmR/HQiNGjCQnfkr6jluKrCr7abBWweuhYz1bs8vALKwiJkkCMVhtakJtzfP8zlBnAw0uqUTgFPlX79zvEi07+sa32iCmpO8WUnj+qwKBgQDqrAj1O0RzaGEAPF2PBk5RIfhGji0D0258d6cV10gMNG6x41wHXS466QQfZ4VuvIFCFok/r69zQKKR9VlpaFgQDsJgwhXqHB3/eHLTjGo43WF3nr7cIfLPqLYRAaYbw9ZTIats+kvKU+UVMG3R4B9sphTMmUemgWhZMyk49cOdJw==";
  // 服务器异步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
  public static String notify_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/notify_url.jsp";
  // 页面跳转同步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问 商户可以自定义同步跳转地址
  public static String return_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/return_url.jsp";
  // 请求网关地址
  public static String URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
  // 编码
  public static String CHARSET = "UTF-8";
  // 返回格式
  public static String FORMAT = "json";
  // 支付宝公钥
//	public static String ALIPAY_PUBLIC_KEY = "";
  // 日志记录目录
  public static String log_path = "/log";
  // RSA2
  public static String SIGNTYPE = "RSA2";
 }
