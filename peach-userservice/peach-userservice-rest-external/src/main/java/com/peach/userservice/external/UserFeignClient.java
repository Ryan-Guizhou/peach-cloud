//package com.peach.userservice.external;
//
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//@FeignClient(
//    name = "peach-monitor",
//    path = "/monitor"
//)
//public interface UserFeignClient {
//
//    @GetMapping("/{id}")
//    UserDTO getUser(@PathVariable("id") Long id);
//
//}
//
