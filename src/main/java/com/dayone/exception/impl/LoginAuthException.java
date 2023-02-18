package com.dayone.exception.impl;

import com.dayone.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class LoginAuthException extends AbstractException {

    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "패스워드와 아이디가 일치하지 않습니다.";
    }
}
