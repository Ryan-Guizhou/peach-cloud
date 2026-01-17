package com.peach.userservice.rest;



import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 11:41
 */
@Tag(name = "订单服务")
@RestController
@RequestMapping("/user")
public class UserController {

//    @Autowired
//    private UserFeignClient userFeignClient;

    @GetMapping()
    public Map<String,Object> map(){
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-userservice");
        return map;
    }

//    @Operation(summary = "远程调用测试")
//    @GetMapping("/remote/{id}")
//    public UserDTO get(@PathVariable("id") Long id){
//        return userFeignClient.getUser(id);
//    }
//
//    @Autowired
//    private IUserservice iUserservice;

//    @Operation(summary = "查询所有人员信息")
//    @PostMapping("/allUser")
//    public Response allUser(@RequestBody UserQO userQO){
//        List<UserVO> userVOList = iUserservice.list(userQO);
//        return Response.success(userVOList);
//    }
//
//    @Operation(summary = "按ID查询人员信息")
//    @PostMapping("/selectById/{userId}")
//    public Response allUser(@PathVariable("userId") String userId){
//        UserVO userVO  = iUserservice.getById(userId);
//        return Response.success(userVO);
//    }


}
