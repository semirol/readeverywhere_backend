package com.dsw.readeverywhere;

import com.dsw.readeverywhere.utils.JedisUtils;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;


import java.io.*;
import java.util.*;

public class FileSystemManager {
    public final static String rootPath = "D:/readeverywhereFileSystem";
    public static String normalizePath(String email,String path){
        String[] pathList = path.split("/");
        String res = String.join("/",Arrays.asList(pathList).subList(1,pathList.length));
        return rootPath+"/user"+email+"/"+res;
    }
    public static File loadFile(String email,String path){
        return new File(normalizePath(email,path));
    }
    public static String saveFile(String email,String path,MultipartFile file){
        if (!file.isEmpty()) {
            try {
                BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(new File(FileSystemManager.normalizePath(email,path+"/"+file.getOriginalFilename()))));
                out.write(file.getBytes());
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return "上传失败," + e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return "上传失败," + e.getMessage();
            }
            return "1";
        } else {
            return "上传失败，因为文件是空的.";
        }
    }
    public static Object emailToPathTree(String email){
        updatePathTree(email);
        Jedis jedis = JedisUtils.getJedis();
        String res = jedis.hget("user:"+email,"pathTree");
        jedis.close();
        System.out.println(res);
        return res;
    }
    public static String updatePathTree(String email){
        File file = new File(normalizePath(email,"root"));
        ObjectMapper om = new ObjectMapper();
        Map map = new HashMap();
        String res = "null";
        if (!file.exists()){
            return "fileNotExist";
        }
        if (file.isDirectory()){
            map = dfsOnDir(file);
            Jedis jedis = JedisUtils.getJedis();
            try {
                res = om.writeValueAsString(map);
                jedis.hset("user:"+email,"pathTree", res);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            jedis.close();
        }
        return res;
    }
    private static Map<String,Object> dfsOnDir(File file){
        Map<String,Object> map = new HashMap();
        List<Map<String,Object>> list = new ArrayList<>();
        map.put("name",file.getName());
        map.put("type","dir");
        for(File item : file.listFiles()){
            if (item.isDirectory()){
                Map<String,Object> map1 = dfsOnDir(item);
                list.add(map1);
            }
            else{
                Map<String,Object> map1 = new HashMap();
                map1.put("name",item.getName());
                map1.put("type","pdf");
                list.add(map1);
            }
        }
        map.put("data",list);
        return map;
    }
    public static void initUserSpace(String email) {
        File dir = new File(rootPath + "/user" + email);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
    public static int getUserSpace(String email){
        return 1;
    }
}
