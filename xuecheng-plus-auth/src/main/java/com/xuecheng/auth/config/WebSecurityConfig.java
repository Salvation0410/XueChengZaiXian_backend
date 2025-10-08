package com.xuecheng.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

/**
 * @author Mr.M
 * @version 1.0
 * @description 安全管理配置
 * @date 2022/9/26 20:53
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true,prePostEnabled = true)

public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    DaoAuthenticationProviderCustom daoAuthenticationProviderCustom;

    /*
     * 配置认证管理Bean
     * */
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(daoAuthenticationProviderCustom);
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
//        //密码为明文方式
//        return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();
    }

        //配置安全拦截机制
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests()
                    .antMatchers("/media/upload/coursefile").permitAll()  // 放行完整路径
                    .antMatchers("/media/open/**").permitAll()  // 如果有开放接口也放行
                    .anyRequest().authenticated()
                    .and()
                    .csrf().disable()  // 文件上传需要禁用CSRF
                    .cors();  // 启用CORS支持
        }



}
