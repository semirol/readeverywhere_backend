package com.dsw.readeverywhere.mapper;

import com.dsw.readeverywhere.model.User;

public interface UserMapper {
    User selectUserByNameAndPassword(String name,String password);
    User selectUserByPhone(long phone);
}
