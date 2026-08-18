package com.liwx.laborder.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器装配：向调度中心注册本服务，到点回调 9999 端口触发 @XxlJob 方法。
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Value("${xxl.job.accessToken}")
    private String accessToken;

    /**
     * xxl-job-core 是普通 SDK 不是 starter，没有自动装配入口，
     * 必须手动创建 XxlJobSpringExecutor bean（启动时开端口/注册，停机时下线）；
     * 执行器自己会扫描容器内所有 @XxlJob 方法，调度中心按 handler 名回调。
     * 
     * starter（mybatis-plus 等）则靠条件注解（@ConditionalOnClass/OnProperty 等）
     * 判断"配置有值才装配"，无需手写此类配置。
     */
    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        log.info(">>>>>>>>>>> xxl-job 执行器注册中，adminAddresses={}, appname={}", adminAddresses, appname);
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        return xxlJobSpringExecutor;
    }
}
