package com.dsw.readeverywhere;

import com.dsw.readeverywhere.model.User;
import com.dsw.readeverywhere.utils.JedisUtils;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

public class MySessionManager {
    private static final long timeToRefresh = 86400000;//1 day
    private static final long timeout = 604800000;//7 days
    public static String generalToken(String email){
        long time = System.currentTimeMillis();
        String emailString = String.valueOf(email);
        String timeString = String.valueOf(time);
        String token = emailString + timeString;
        token = DigestUtils.md5DigestAsHex(token.getBytes());
        String p1 = "emailToTokenList:"+emailString;
        Jedis jedis = JedisUtils.getJedis();
        //判断是否存在5对token
        jedis.rpush(p1,token);
        if (jedis.llen(p1)>5){
            p1 = jedis.lpop(p1);
            jedis.del("token:"+p1);
            System.out.println("token "+p1+" has deleted.");
        }
        jedis.hset("token:"+token,"email",emailString);
        jedis.hset("token:"+token,"createdTime",timeString);
        jedis.close();
        return token;
    }
    public static void deleteToken(String token){
        Jedis jedis = JedisUtils.getJedis();
        jedis.del("token:"+token);
        jedis.close();
    }
    public static String tokenToEmail(String token){
        long time = System.currentTimeMillis();
        String timeString = String.valueOf(time);
        Jedis jedis = JedisUtils.getJedis();
        String createdTime = jedis.hget("token:"+token,"createdTime");
        if (createdTime==null){
            jedis.close();
            return "null";
        }
        long timeDiff = time-Long.parseLong(createdTime);
        if (timeDiff<timeToRefresh){
            String email = jedis.hget("token:"+token,"email");
            jedis.close();
            return email;
        }
        else if (timeDiff>timeout){
            jedis.close();
            return "expired";
        }
        else{
            jedis.hset("token:"+token,"createdTime",timeString);
            String email = jedis.hget("token:"+token,"email");
            jedis.close();
            return email;
        }
    }
    public static void updateLastLoginTime(String email){
        JedisUtils.hset("user:"+email,"lastLoginTime",String.valueOf(System.currentTimeMillis()));
    }

}
