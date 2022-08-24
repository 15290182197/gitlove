package com.lx.wxpush.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lx.wxpush.utils.AncientPoetry;
import com.lx.wxpush.utils.DateUtil;
import com.lx.wxpush.utils.HttpUtil;
import com.lx.wxpush.utils.RandomAncientPoetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

@RestController
@RequestMapping("/wx")
public class wxController {

    @Value("${wx.config.appId}")
    private String appId;
    @Value("${wx.config.appSecret}")
    private String appSecret;
    @Value("${wx.config.templateId}")
    private String templateId;
    @Value("${wx.config.openid}")
    private String openid;
    @Value("${weather.config.appid}")
    private String weatherAppId;
    @Value("${weather.config.appSecret}")
    private String weatherAppSecret;
    @Value("${message.config.togetherDate}")
    private String togetherDate;
    @Value("${message.config.birthday}")
    private String birthday;
    @Value("${message.config.message}")
    private String message;
    @Value("${love.appSecret}")
    private String loveAppSecret;

    private String accessToken = "";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    /**
     * 获取Token
     * 每天早上7：30执行推送
     * @return
     */
    @Scheduled(cron = "0 30 7 ? * *")
    @RequestMapping("/getAccessToken")
    public String getAccessToken() {
        String accessToken = this.getToken();

        return sendWeChatMsg(accessToken);
    }

    private String getToken() {
        //这里直接写死就可以，不用改，用法可以去看api
        String grant_type = "client_credential";
        //封装请求数据
        String params = "grant_type=" + grant_type + "&secret=" + appSecret + "&appid=" + appId;
        //发送GET请求
        String sendGet = HttpUtil.sendGet("https://api.weixin.qq.com/cgi-bin/token", params);
        // 解析相应内容（转换成json对象）
        com.alibaba.fastjson.JSONObject jsonObject1 = com.alibaba.fastjson.JSONObject.parseObject(sendGet);
        logger.info("微信token响应结果=" + jsonObject1);
        //拿到accesstoken
        accessToken = (String) jsonObject1.get("access_token");
        return accessToken;
    }


    /**
     * 发送微信消息
     *
     * @return
     */
    public String sendWeChatMsg(String accessToken) {
       List<String> openIds = this.getOpenId(accessToken);
        List<JSONObject> errorList = new ArrayList();
        for (String opedId : openIds) {
            if (!opedId.equals("o5q_H5kWHf0Nzre6pVyTytqG18O8")){
                continue;
            }
            JSONObject templateMsg = new JSONObject(new LinkedHashMap<>());

            templateMsg.put("touser", opedId);
            templateMsg.put("template_id", templateId);


            JSONObject first = new JSONObject();
            String date = DateUtil.formatDate(new Date(), "yyyy-MM-dd");
            String week = DateUtil.getWeekOfDate(new Date());
            String day = date + " " + week;
            first.put("value", day);
            first.put("color", "#EED016");


            String TemperatureUrl = "https://www.yiketianqi.com/free/day?appid=" + weatherAppId + "&appsecret=" + weatherAppSecret +"&cityid=101210101"+
                    "&unescape=1";
            String sendGet = HttpUtil.sendGet(TemperatureUrl, null);
            JSONObject temperature = JSONObject.parseObject(sendGet);
            String address = "无法识别";
            String tem_day = "无法识别"; //最高温度
            String tem_night = "无法识别"; //最低温度
            String weatherStatus = "";
            if (temperature.getString("city") != null) {
                tem_day = temperature.getString("tem_day") + "°";
                tem_night = temperature.getString("tem_night") + "°";
                address = temperature.getString("city");
                weatherStatus = temperature.getString("wea");
            }

            JSONObject city = new JSONObject();
            city.put("value", address);
            city.put("color", "#60AEF2");

            String weather = weatherStatus + ", 温度：" + tem_night + " ~ " + tem_day;


            JSONObject temperatures = new JSONObject();
            temperatures.put("value", weather);
            temperatures.put("color", "#44B549");

            JSONObject birthDate = new JSONObject();
            String birthDay = "无法识别";
            try {
                Calendar calendar = Calendar.getInstance();
                String newD = calendar.get(Calendar.YEAR) + "-" + birthday;
                birthDay = DateUtil.daysBetween(date, newD);
                if (Integer.parseInt(birthDay) < 0) {
                    Integer newBirthDay = Integer.parseInt(birthDay) + 365;
                    birthDay = newBirthDay + "天";
                } else {
                    birthDay = birthDay + "天";
                }
            } catch (ParseException e) {
                logger.error("togetherDate获取失败" + e.getMessage());
            }
            birthDate.put("value", birthDay);
            birthDate.put("color", "#6EEDE2");


            JSONObject togetherDateObj = new JSONObject();
            String togetherDay = "";
            try {
                togetherDay = "第" + DateUtil.daysBetween(togetherDate, date) + "天";
            } catch (ParseException e) {
                logger.error("togetherDate获取失败" + e.getMessage());
            }
            togetherDateObj.put("value", togetherDay);
            togetherDateObj.put("color", "#FEABB5");
            final AncientPoetry next = RandomAncientPoetry.getNext();
            JSONObject messageObj1 = new JSONObject();
            messageObj1.put("value", next.getAuthor());
            messageObj1.put("color", "#F53F3F");

            JSONObject messageObj2 = new JSONObject();
            messageObj2.put("value", next.getOrigin());
            messageObj2.put("color", "#F53F3F");


            JSONObject messageObj = new JSONObject();
            messageObj.put("value", next.getContent());
            messageObj.put("color", "#F53F3F");


            JSONObject data = new JSONObject(new LinkedHashMap<>());
            data.put("first", first);
            data.put("city", city);
            data.put("temperature", temperatures);
            data.put("togetherDate", togetherDateObj);
            data.put("birthDate", birthDate);
            data.put("message", messageObj);
            data.put("origin", messageObj2);
            data.put("author", messageObj1);


            templateMsg.put("data", data);
            String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;

            String sendPost = HttpUtil.sendPost(url, templateMsg.toJSONString());
            JSONObject WeChatMsgResult = JSONObject.parseObject(sendPost);
            if (!"0".equals(WeChatMsgResult.getString("errcode"))) {
                JSONObject error = new JSONObject();
                error.put("openid", opedId);
                error.put("errorMessage", WeChatMsgResult.getString("errmsg"));
                errorList.add(error);
            }
            logger.info("sendPost=" + sendPost);
        }
        JSONObject result = new JSONObject();
        result.put("result", "success");
        result.put("errorData", errorList);
        return result.toJSONString();

    }

    private List<String> getOpenId(String accessToken) {
        String httpUrl = "https://api.weixin.qq.com/cgi-bin/user/get?access_token="+accessToken;
        BufferedReader reader = null;
        String result = null;
        StringBuffer sbf = new StringBuffer();
        try {
            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod("GET");
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            result = sbf.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final JSONObject jsonObject = JSON.parseObject(result);
        final String newslist = jsonObject.getString("data");
        final JSONObject jsonObject1 = JSON.parseObject(newslist);
        final String openidlist = jsonObject1.getString("openid");
        final JSONArray objects = JSONObject.parseArray(openidlist);
        List<String> list = new ArrayList<>();
        for (Object object : objects) {
            list.add(object.toString());
        }
        return list;
    }

    private String getMsg() {
        String httpUrl = "http://api.tianapi.com/saylove/index?key="+loveAppSecret;
        BufferedReader reader = null;
        String result = null;
        StringBuffer sbf = new StringBuffer();
        try {
            URL url = new URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setRequestMethod("GET");
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String strRead = null;
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            result = sbf.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        final JSONObject jsonObject = JSON.parseObject(result);
        final String newslist = jsonObject.getString("newslist");
        final JSONArray objects = JSONObject.parseArray(newslist);
        final String string = objects.getString(0);
        final JSONObject jsonObject1 = JSON.parseObject(string);
        final String content = jsonObject1.getString("content");
        return content;
    }

}
