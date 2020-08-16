package com.dsw.readeverywhere.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;

@Component
public class JedisUtils {
    public static JedisPool jedisPool;
    @Value("${myConfig.redis.ip}")
    private String _ip;
    private static String ip;
    @Value("${myConfig.redis.port}")
    private int _port;
    private static int port;
    @Value("${myConfig.redis.password}")
    private String _password;
    private static String password;
    @PostConstruct
    public void init() {
        ip = this._ip;
        port = this._port;
        password = this._password;
        JedisPoolConfig config = new JedisPoolConfig();
        config.setLifo(true);
        jedisPool = new JedisPool(config,ip,port);
    }
    public synchronized static Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        jedis.auth(password);
        return jedis;
    }
    public static String set(String k,String v){
        Jedis jedis = getJedis();
        String ret = jedis.set(k,v);
        jedis.close();
        return ret;
    }
    public static String set(String table,String id,String key,String v){
        Jedis jedis = getJedis();
        String k = table+':'+id+':'+key;
        String ret = jedis.set(k,v);
        jedis.close();
        return ret;
    }
    public static String get(String k){
        Jedis jedis = getJedis();
        String ret = jedis.get(k);
        jedis.close();
        return ret;
    }
    public static String hget(String k1,String k2){
        Jedis jedis = getJedis();
        String ret = jedis.hget(k1,k2);
        jedis.close();
        return ret;
    }
    public static Long hset(String k1,String k2,String k3){
        Jedis jedis = getJedis();
        Long ret = jedis.hset(k1,k2,k3);
        jedis.close();
        return ret;
    }
    public static long del(String k){
        Jedis jedis = getJedis();
        Long ret = jedis.del(k);
        jedis.close();
        return ret;
    }
}
