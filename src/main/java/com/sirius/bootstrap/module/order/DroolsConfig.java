package com.sirius.bootstrap.module.order;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.KieRepository;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools配置类
 */
@Configuration
public class DroolsConfig {

    // 规则文件路径
    private static final String RULES_PATH = "rules/";

    private final KieServices kieServices = KieServices.Factory.get();

    /**
     * 创建KieFileSystem
     */
    private KieFileSystem getKieFileSystem() {
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        // 加载规则文件
        kieFileSystem.write(ResourceFactory.newClassPathResource(RULES_PATH + "order-discount.drl"));
        return kieFileSystem;
    }

    /**
     * 创建KieContainer
     */
    @Bean
    public KieContainer kieContainer() {
        KieRepository kieRepository = kieServices.getRepository();
        KieBuilder kieBuilder = kieServices.newKieBuilder(getKieFileSystem());
        kieBuilder.buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }

    /**
     * 创建KieSession
     */
    @Bean
    public KieSession kieSession() {
        return kieContainer().newKieSession();
    }
}
