package com.xuecheng.auth.config;

import com.xuecheng.auth.Enums.AuthEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;

import javax.annotation.Resource;

/**
 * @description 授权服务器配置
 * @author Mr.M
 * @date 2022/9/26 22:25
 * @version 1.0
 */
 @Configuration
 @EnableAuthorizationServer
 public class AuthorizationServer extends AuthorizationServerConfigurerAdapter {

  @Resource(name="authorizationServerTokenServicesCustom")
  private AuthorizationServerTokenServices authorizationServerTokenServices;

 @Autowired
 private AuthenticationManager authenticationManager;

  //客户端详情服务
  @Override
  public void configure(ClientDetailsServiceConfigurer clients)
          throws Exception {
        clients.inMemory()// 使用in-memory存储
                .withClient(AuthEnum.AUTH_CLIENT_ID.getValue())// client_id
 //               .secret("XcWebApp")//客户端密钥
                //对密钥进行加密处理
                .secret(new BCryptPasswordEncoder().encode(AuthEnum.AUTH_CLIENT_SECRET.getValue()))//客户端密钥
                .resourceIds(AuthEnum.AUTH_RESOURCE_ID.getValue())//资源列表
                .authorizedGrantTypes(AuthEnum.AUTH_AUTHORIZED_GRANT_TYPES.getValue())
                .autoApprove(false)//false跳转到授权页面
                //客户端接收授权码的重定向地址
                .redirectUris(AuthEnum.AUTH_REDIRECT_URI.getValue())
   ;
  }


  //令牌端点的访问配置
  @Override
  public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
   endpoints
           .authenticationManager(authenticationManager)//认证管理器
           .tokenServices(authorizationServerTokenServices)//令牌管理服务
           .allowedTokenEndpointRequestMethods(HttpMethod.POST);
  }

  //令牌端点的安全配置
  @Override
  public void configure(AuthorizationServerSecurityConfigurer security){
   security
           .tokenKeyAccess("permitAll()")                    //oauth/token_key是公开
           .checkTokenAccess("permitAll()")                  //oauth/check_token公开
           .allowFormAuthenticationForClients()				//表单认证（申请令牌）
   ;
  }



 }
