package com.dsw.readeverywhere;

import com.dsw.readeverywhere.mapper.UserMapper;
import com.dsw.readeverywhere.model.User;
import com.dsw.readeverywhere.utils.JedisUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;

import javax.mail.internet.MimeMessage;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin
@RestController
public class RequestController {
    @Autowired
    private Environment env;
    @Autowired
    private JavaMailSender javaMailSender;
    private static final long activateTokenTimeOut = 60000;
//    private static SqlSessionFactory sqlSessionFactory;
//    static{
//        String resource = "mybatis-config.xml";
//        try {
//            InputStream inputStream = Resources.getResourceAsStream(resource);
//            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    @GetMapping("/activate")
    public Object activate(@RequestParam("email")String email,
                           @RequestParam("activateToken")String activateToken){
        Jedis jedis = JedisUtils.getJedis();
        String activateToken1 = jedis.hget("signUpUser:"+email,"activateToken");
        if (activateToken1==null){
            jedis.close();
            return "验证失败，激活码不存在";
        }
        else if (activateToken1.equals(activateToken)){
            Map<String,String> map = new HashMap<String, String>();
            jedis.hset("signUpUser:"+email,"activate","1");
            Map<String,String> resMap = jedis.hgetAll("signUpUser:"+email);
            map.put("name",resMap.get("name"));
            map.put("password",resMap.get("password"));
            map.put("signUpTime",resMap.get("signUpTime"));
            map.put("usedSpace","0");
            map.put("totalSpace","20480");
            map.put("vip","0");
            map.put("lastLoginTime","0");
            jedis.hmset("user:"+email,map);
            jedis.close();
            FileSystemManager.initUserSpace(email);
            FileSystemManager.updatePathTree(email);
            return "验证成功，请回到readeverywhere登录账户";
        }
        else{
            jedis.close();
            return "验证失败";
        }
    }
    @PostMapping("/signUp")
    public Map<String,Object> signUp(@RequestParam("email")String email,
                                    @RequestParam("name")String name,
                                    @RequestParam("password")String password,
                                     @RequestParam("invCode")String invCode){
        Map<String,Object> map = new HashMap();
        if (!invCode.equals("dswtxdys")){
            map.put("status","invCodeError");
            return map;
        }
        long time = System.currentTimeMillis();
        String timeString = String.valueOf(time);
        String activateString = email + timeString;
        String activateToken = DigestUtils.md5DigestAsHex(activateString.getBytes());

        Jedis jedis = JedisUtils.getJedis();
        String activate = jedis.hget("signUpUser:"+email,"activate");
        jedis.close();
        if (activate==null||activate.equals("0")){
            if (activate!=null){
                String signUpTimeString = jedis.hget("signUpUser:"+email,"signUpTime");
                long signUpTime = Long.parseLong(signUpTimeString);
                long timeStamp = time - activateTokenTimeOut;
                if (timeStamp<activateTokenTimeOut){
                    map.put("status","wait");
                    return map;
                }
            }
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            try {
                messageHelper.setSubject("readeverywhere账户激活");
                messageHelper.setFrom("350395090@qq.com");
                messageHelper.setTo(email);
                messageHelper.setText("<h1>请点击此链接完成注册:</h1><br/><a href=\"http://"+
                        env.getProperty("myConfig.RequestController.serverIp")+":"+
                        env.getProperty("myConfig.RequestController.serverPort")+
                        "activate?email="+email+"&activateToken="+activateToken+"\">"+
                        "http://127.0.0.1:8080/activate</a>", true);
                javaMailSender.send(messageHelper.getMimeMessage());

                map.put("status","true");
                Map<String,String> jedisMap = new HashMap();
                jedisMap.put("activate","0");
                jedisMap.put("activateToken",activateToken);
                jedisMap.put("name",name);
                jedisMap.put("password",password);
                jedisMap.put("signUpTime",timeString);
                Jedis jedis1 = JedisUtils.getJedis();
                jedis1.hmset("signUpUser:"+email,jedisMap);
                jedis1.close();
            } catch (Exception e) {
                e.printStackTrace();
                map.put("status","false");
            }
        }
        else{
            jedis.close();
            map.put("status","activated");
        }
        return map;
    }
    @PostMapping("/login")
    public Map<String,Object> login(@RequestParam("email")String email,
                      @RequestParam("password")String password){
        Map<String,Object> map = new HashMap();
        Jedis jedis = JedisUtils.getJedis();
        Map<String,String> jedisMap = jedis.hgetAll("user:"+email);
        jedis.close();
        if (jedisMap==null){
            map.put("status","false");
            return map;
        }
        String password1 = jedisMap.get("password");
        if (password1==null){
            map.put("status","false");
            return map;
        }
        else if (password1.equals(password)){
            String token = MySessionManager.generalToken(email);
            map.put("status","true");
            User user = new User().setEmail(email).setName(jedisMap.get("name"))
                    .setUsedSpace(jedisMap.get("usedSpace")).setTotalSpace(jedisMap.get("totalSpace"));
            map.put("user",user);
            map.put("token",token);
            System.out.println("token "+token+" has created.");
            return map;
        }
        else{
            map.put("status","false");
            return map;
        }
    }
    @PostMapping("/logout")
    public void logout(@RequestHeader("token")String token){
        MySessionManager.deleteToken(token);
        System.out.println("token "+token+" has deleted.");
    }
    @PostMapping("/tokenToPath")
    public Map<String,Object> tokenToPath(@RequestHeader("token")String token){
        Map<String,Object> map = new HashMap<String,Object>();
        String email = MySessionManager.tokenToEmail(token);
        Object data = null;
        if (email.equals("null")||email.equals("expired")){
            map.put("status",email);
            return map;
        }
        else{
            data = FileSystemManager.emailToPathTree(email);
            map.put("status","true");
            map.put("data",data);
        }
        return map;
    }
    public ResponseEntity<FileSystemResource> export(File file) {
        if (file == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + System.currentTimeMillis() + ".pdf");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Last-Modified", new Date().toString());
        headers.add("ETag", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok().headers(headers).contentLength(file.length()).contentType(MediaType.parseMediaType("application/pdf")).body(new FileSystemResource(file));
    }
    @GetMapping("/getPdfStream")
    public Object getPdfStream(@RequestParam("token")String token,
                               @RequestParam("path")String path){
        String email = MySessionManager.tokenToEmail(token);
        if (email.equals("null")||email.equals("expired")){
            return email;
        }
        else{
            File file = FileSystemManager.loadFile(email,path);
            if (file.exists()&&file.canRead()){
                return export(file);
            }
            else{
                return "fileNotAvailable";
            }
        }
    }
    @PostMapping("/checkLogin")
    public String checkLogin(@RequestHeader("token")String token){
        String email = MySessionManager.tokenToEmail(token);
        if (email.equals("null")||email.equals("expired")){
            return email;
        }
        else{
            return "true";
        }
    }
    @PostMapping("/uploadPdf")
    public Object uploadPdf(@RequestHeader("token")String token,
                             @RequestParam("path")String path,
                             @RequestParam("file")MultipartFile file){
        Map<String,Object> map = new HashMap();
        String email = MySessionManager.tokenToEmail(token);
        if (email.equals("null")||email.equals("expired")){
            map.put("status",email);
            return map;
        }
        long size = file.getSize() / 1024;
        long freeSpace = MySessionManager.getFreeSpace(email);
        if (size >= freeSpace){
            map.put("status","noEnoughSpace");
        }
        else{
            String res = FileSystemManager.saveFile(email,path,file);
            if (res.equals("1")){
                map.put("status","true");
                MySessionManager.addUsedSpace(email,size);
            }
            else{
                map.put("status","false");
            }

        }
        return map;
    }
    @PostMapping("/deletePdfOrDir")
    public Object deletePdfOrDir(@RequestHeader("token")String token,
                            @RequestParam("path")String path){
        Map<String,Object> map = new HashMap();
        String email = MySessionManager.tokenToEmail(token);
        if (email.equals("null")||email.equals("expired")){
            map.put("status",email);
            return map;
        }
        String res = FileSystemManager.deleteFileOrDir(email,path);
        if (res.equals("1")){
            map.put("status","true");
        }
        else{
            map.put("status","false");
        }
        return map;
    }
    @PostMapping("/newDir")
    public Object newDir(@RequestHeader("token")String token,
                         @RequestParam("path")String path,
                         @RequestParam("dirName")String dirName){
        Map<String,Object> map = new HashMap();
        String email = MySessionManager.tokenToEmail(token);
        if (email.equals("null")||email.equals("expired")){
            map.put("status",email);
            return map;
        }
        String res = FileSystemManager.newDir(email,path,dirName);
        map.put("status",res);
        return map;
    }
    @PostMapping("/getUserInfo")
    public Object getUserInfo(@RequestHeader("token")String token){
        Map<String,Object> map = new HashMap();
        String email = MySessionManager.tokenToEmail(token);
        if (email.equals("null")||email.equals("expired")){
            map.put("status",email);
            return map;
        }
        Jedis jedis = JedisUtils.getJedis();
        Map<String,String> jedisMap = jedis.hgetAll("user:"+email);
        jedis.close();
        if (jedisMap==null){
            map.put("status","false");
            return map;
        }
        else{
            User user = new User().setEmail(email).setName(jedisMap.get("name"))
                    .setUsedSpace(jedisMap.get("usedSpace")).setTotalSpace(jedisMap.get("totalSpace"));
            map.put("user",user);
            map.put("status","true");
        }
        return map;
    }
    @GetMapping("/hello")
    public String hello(){
        return "hello";
    }
}
