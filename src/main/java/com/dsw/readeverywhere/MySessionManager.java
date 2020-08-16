package com.dsw.readeverywhere;

import com.dsw.readeverywhere.model.User;
import com.dsw.readeverywhere.utils.JedisUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;

@Component
public class MySessionManager {
    @Value("${myConfig.MySessionManager.timeToRefresh}")
    private long _timeToRefresh;
    private static long timeToRefresh;
    @Value("${myConfig.MySessionManager.timeout}")
    private long _timeout;
    private static long timeout;
    @PostConstruct
    public void init() {
        timeToRefresh = this._timeToRefresh;
        timeout = this._timeout;
    }
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
            updateLastLoginTime(email);
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
            updateLastLoginTime(email);
            return email;
        }
    }
    public static void updateLastLoginTime(String email){
        JedisUtils.hset("user:"+email,"lastLoginTime",String.valueOf(System.currentTimeMillis()));
    }
    public static long getFreeSpace(String email){
        Jedis jedis = JedisUtils.getJedis();
        long totalSpace = Long.parseLong(jedis.hget("user:"+email,"totalSpace"));
        long usedSpace = Long.parseLong(jedis.hget("user:"+email,"usedSpace"));
        jedis.close();
        long freeSpace = totalSpace - usedSpace;
        return freeSpace;
    }
    public static void addUsedSpace(String email,long size){
        Jedis jedis = JedisUtils.getJedis();
        long usedSpace = Long.parseLong(jedis.hget("user:"+email,"usedSpace"));
        jedis.hset("user:"+email,"usedSpace", String.valueOf(usedSpace+size));
        jedis.close();
    }
    public static void subUsedSpace(String email,long size){
        Jedis jedis = JedisUtils.getJedis();
        long usedSpace = Long.parseLong(jedis.hget("user:"+email,"usedSpace"));
        jedis.hset("user:"+email,"usedSpace", String.valueOf(usedSpace-size));
        jedis.close();
    }

}
