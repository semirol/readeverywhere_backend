package com.dsw.readeverywhere.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisUtils {
    public static JedisPool jedisPool;
    static {
        String ip = "127.0.0.1";
        JedisPoolConfig config = new JedisPoolConfig();
        config.setLifo(true);
        jedisPool = new JedisPool(config,ip,6379);
    }
    public static Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        jedis.auth("jlccdsw1");
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
