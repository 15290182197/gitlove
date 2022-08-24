package com.lx.wxpush.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

/**
 * 随机爱情古诗
 * @author DokiYolo
 * Date 2022-08-22
 */
public class RandomAncientPoetry {

    public static AncientPoetry getNext() {
        String res = HttpUtil.get("https://v1.jinrishici.com/shuqing/aiqing", 4000);
        return JSONUtil.parseObj(res).toBean(AncientPoetry.class);
    }
}
