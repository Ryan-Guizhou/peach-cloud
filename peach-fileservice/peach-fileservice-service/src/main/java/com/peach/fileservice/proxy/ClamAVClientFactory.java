package com.peach.fileservice.proxy;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClamAVClientFactory {

  @Value("${clamd.host}")
  private String hostname;

  @Value("${clamd.port}")
  private int port;

  @Value("${clamd.timeout}")
  private int timeout;

  public ClamAVClient newClient() {
        return new ClamAVClient(hostname, port, timeout);
    }
}
