package com.zhuoan.webapp.controller.verification;

import com.alibaba.fastjson.JSON;
import com.zhuoan.constant.CacheKeyConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.RegexConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.SmsUtils;
import com.zhuoan.webapp.controller.BaseController;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.regex.Pattern;

/**
 * Verification Code
 *
 * @author wqm
 * @Date Created in 14:14 2018/11/13
 **/
@Controller
public class VerificationController extends BaseController {

    @Resource
    private RedisService redisService;

    @RequestMapping(value = "getVerificationCode", method = RequestMethod.GET)
    @ResponseBody
    public String getVerificationCode(String tel) {
        JSONObject result = new JSONObject();
        // 验证手机号是否正确
        boolean matches = Pattern.matches(RegexConstant.REGEX_MOBILE, tel);
        if (matches) {
            // 是否重新获取验证码
            if (!redisService.sHasKey(CacheKeyConstant.VERIFICATION_SET, tel)) {
                String verificationCode = SmsUtils.sendMsg(tel);
                // 短信验证码有效时长为30分钟
                redisService.hset(CacheKeyConstant.VERIFICATION_MAP, tel, verificationCode, 30 * 60);
                // 1分钟内无法重复获取
                redisService.sSetAndTime(CacheKeyConstant.VERIFICATION_SET, 60, tel);
                result.put("code", CommonConstant.GLOBAL_YES);
                result.put("msg", "获取验证码成功");
                result.put("verificationCode", verificationCode);
            } else {
                result.put("code", CommonConstant.GLOBAL_NO);
                result.put("msg", "请勿重复获取验证码");
            }
        } else {
            result.put("code", CommonConstant.GLOBAL_NO);
            result.put("msg", "请输入正确的手机号");
        }
        return JSON.toJSONString(result);
    }

}
