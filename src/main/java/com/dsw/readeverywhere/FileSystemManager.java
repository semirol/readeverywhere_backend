package com.dsw.readeverywhere;

import com.dsw.readeverywhere.utils.JedisUtils;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;

@Component
public class FileSystemManager {
    @Value("${myConfig.FileSystemManager.rootPath}")
    private String _rootPath;
    private static String rootPath;
    @PostConstruct
    public void init() {
        rootPath = this._rootPath;
    }
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
            updatePathTree(email);
            return "1";
        } else {
            return "上传失败，因为文件是空的.";
        }
    }
    public static String deleteFileOrDir(String email,String path){
        File file = new File(FileSystemManager.normalizePath(email,path));
        if (!file.exists()) {  // 不存在返回 false
            return "0";
        } else {
            // 判断是否为文件
            if (file.isFile()) {  // 为文件时调用删除文件方法
                deleteFile(email,file);
            } else {  // 为目录时调用删除目录方法
                deleteDirectory(email,file);
            }
            updatePathTree(email);
            return "1";
        }
    }
    private static boolean deleteFile(String email,File file){
        boolean flag = false;
        if (file.isFile() && file.exists()) {
            file.delete();
            MySessionManager.subUsedSpace(email,file.length()/1024);
            flag = true;
        }
        return flag;
    }
    private static boolean deleteDirectory(String email,File dirFile){
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        //删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            //删除子文件
            if (files[i].isFile()) {
                flag = FileSystemManager.deleteFile(email,files[i]);
                if (!flag) break;
            } //删除子目录
            else {
                flag = deleteDirectory(email,files[i]);
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //删除当前目录
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }
    public static Object emailToPathTree(String email){
//        updatePathTree(email);
        Jedis jedis = JedisUtils.getJedis();
        String res = jedis.hget("user:"+email,"pathTree");
        jedis.close();
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
//    public static int getUserSpace(String email){
//        return 1;
//    }
    public static String newDir(String email,String path,String dirName){
        File dir = new File(FileSystemManager.normalizePath(email,path+'/'+dirName));
        if (!dir.exists()){
            dir.mkdir();
            updatePathTree(email);
            return "true";
        }
        else return "false";
    }
}
