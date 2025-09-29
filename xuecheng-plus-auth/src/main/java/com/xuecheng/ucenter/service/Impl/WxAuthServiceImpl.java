package com.xuecheng.ucenter.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.auth.Enums.AuthEnum;
import com.xuecheng.auth.Enums.CommonEnum;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.mapper.XcUserRoleMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.model.po.XcUserRole;
import com.xuecheng.ucenter.service.AuthService;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * @author huang
 * @version 1.0
 * @description 微信扫码认证
 * @date 2025/9/16
 */
@Service("wx_authservice")
@Slf4j
@RequiredArgsConstructor
public class WxAuthServiceImpl implements AuthService, WxAuthService {

    private  final XcUserMapper xcUserMapper;

    private  final RestTemplate restTemplate;

    private final XcUserRoleMapper xcUserRoleMapper;

    @Autowired
    WxAuthServiceImpl currentProxy;

    @Value("${weixin.appid}")
    String appid;
    @Value("${weixin.secret}")
    String secret;

    @Override
    public XcUserExt execute(AuthParamsDto authParamsDto) {

            //账号
            String username = authParamsDto.getUsername();
            XcUser user = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getUsername, username));
            if(user==null){
                //返回空表示用户不存在
                throw new RuntimeException("账号不存在");
            }
            XcUserExt xcUserExt = new XcUserExt();
            BeanUtils.copyProperties(user,xcUserExt);
            return xcUserExt;

    }
    /*
    * @description 微信扫码认证 申请令牌 携带令牌查询用户信息 保存用户信息到数据库
    * @param code 微信扫码返回的code
    * */

    @Override
    public XcUser wxAuth(String code) {
        //申请令牌
        Map<String,String> access_token_map = getAccess_token(code);
        //携带令牌查询用户信息
        String access_token = access_token_map.get(AuthEnum.AUTH_ACCESS_TOKEN.getValue());
        // 获取openid
        String openid = access_token_map.get(AuthEnum.AUTH_CLIENT_ID.getValue());
        //获取用户信息
        Map<String,String> userinfo =getUserinfo(access_token,openid);

        //存储用户信息到数据库
        //非事务调用事务方法 事务会失效 需要用代理对象调用
        XcUser xcUser = currentProxy.addWxUser(userinfo);

        return xcUser;
    }

    /**
     * 申请访问令牌,响应示例
     {
     "access_token":"ACCESS_TOKEN",
     "expires_in":7200,
     "refresh_token":"REFRESH_TOKEN",
     "openid":"OPENID",
     "scope":"SCOPE",
     "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
     }
     //携带授权码申请令牌
     //https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
     */
    private Map<String,String> getAccess_token(String code){
        String urlTemplate = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
        //最终的请求路径 传入的参数会代替占位符
        String url = String.format(urlTemplate, appid, secret, code);

        //远程调用url 请求路径 请求方式 请求内容 响应数据的格式
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
        //获取响应的结果 并解决乱码
        String result = new String(responseEntity.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        Map<String,String> map = JSONObject.parseObject(result, Map.class);

        return map;
    }


    /*
    * {
    "openid":"OPENID",
    "nickname":"NICKNAME",
    "sex":1,
    "province":"PROVINCE",
    "city":"CITY",
    "country":"COUNTRY",
    "headimgurl": "https://thirdwx.qlogo.cn/mmopen/g3MonUZtNHkdmzicIlibx6iaFqAc56vxLSUfpb6n5WKSYVY0ChQKkiaJSgQ1dZuTOgvLLrhJbERQQ4eMsv84eavHiaiceqxibJxCfHe/0",
    "privilege":[
    "PRIVILEGE1",
    "PRIVILEGE2"
    ],
    "unionid": " o6_bmasdasdsad6_2sgVt7hMZOPfL"

}
    *
    * */

    /*
     * 授权后携带令牌获取微信用户信息
     * 请求接口：https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
     * */
    private Map<String,String> getUserinfo(String access_token,String openid){
        String urlTemplate = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s";
        String url = String.format(urlTemplate, access_token, openid);
        //远程调用url获取用户信息
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, null, String.class);
        String result = new String(responseEntity.getBody().getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        Map<String,String> map = JSONObject.parseObject(result, Map.class);
        return map;
    }

    /*
    * 保存用户信息到数据库
    * */
    @Transactional
    public XcUser addWxUser(Map userInfo_map){
        String unionid = userInfo_map.get("unionid").toString();
        //根据unionid查询数据库
        XcUser xcUser = xcUserMapper.selectOne(new LambdaQueryWrapper<XcUser>().eq(XcUser::getWxUnionid, unionid));
        if(xcUser!=null){
            return xcUser;
        }
        String userId = UUID.randomUUID().toString();
        xcUser = new XcUser();
        xcUser.setId(userId);
        xcUser.setWxUnionid(unionid);
        //记录从微信得到的昵称
        xcUser.setNickname(userInfo_map.get("nickname").toString());
        xcUser.setUserpic(userInfo_map.get("headimgurl").toString());
        xcUser.setName(userInfo_map.get("nickname").toString());
        xcUser.setUsername(unionid);
        xcUser.setPassword(unionid);
        xcUser.setUtype(CommonEnum.STUDENT.getValue());//学生类型
        xcUser.setStatus(CommonEnum.USE_STATUS.getValue());//用户状态
        xcUser.setCreateTime(LocalDateTime.now());
        xcUserMapper.insert(xcUser);
        XcUserRole xcUserRole = new XcUserRole();
        xcUserRole.setId(UUID.randomUUID().toString());
        xcUserRole.setUserId(userId);
        xcUserRole.setRoleId(CommonEnum.STUDENT_ROLE_ID.getValue());//学生角色
        xcUserRoleMapper.insert(xcUserRole);
        return xcUser;
    }
}
