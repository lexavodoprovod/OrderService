package com.innowise.orderservice.exception;

public class UserServiceException extends RuntimeException {
  public UserServiceException() {
      super("UserService is unavailable (Callback)");
  }
}
