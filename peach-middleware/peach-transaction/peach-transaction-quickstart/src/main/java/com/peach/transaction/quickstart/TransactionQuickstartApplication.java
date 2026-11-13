package com.peach.transaction.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Peach Transaction 最小启动入口；AT 联调必须先部署 Seata TC 和独立 MySQL 分支。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
@SpringBootApplication
public class TransactionQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionQuickstartApplication.class, args);
    }
}
