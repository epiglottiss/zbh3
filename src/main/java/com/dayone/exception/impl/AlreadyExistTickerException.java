package com.dayone.exception.impl;

import com.dayone.exception.AbstractException;
import org.springframework.http.HttpStatus;

public class AlreadyExistTickerException extends AbstractException {
    @Override
    public int getStatusCode() {
        return HttpStatus.BAD_REQUEST.value();
    }

    @Override
    public String getMessage() {
        return "이미 존재하는 ticker입니다.";
    }
}
