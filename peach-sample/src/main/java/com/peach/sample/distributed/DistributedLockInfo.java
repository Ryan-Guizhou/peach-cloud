package com.peach.sample.distributed;

import lombok.Data;


/**
 * Distributed锁信息。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Data
public class DistributedLockInfo{
       private Integer id;
       private String name;
       private Integer age;
    }
