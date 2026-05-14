package com.innowise.orderservice.client;

import feign.FeignException;

public class ThrowFeignException {
    public static void throwFeignEx(Throwable cause) {
        if(cause instanceof FeignException feignException){
            throw feignException;
        }
    }
}
