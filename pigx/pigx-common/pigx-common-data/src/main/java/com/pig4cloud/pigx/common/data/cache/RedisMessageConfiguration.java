package com.pig4cloud.pigx.common.data.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * @author lengleng
 * @date 2022/2/4
 *
 * redis message 信道相关配置
 */
@Configuration(proxyBeanMethods = false)
public class RedisMessageConfiguration {

	/**
	 * 共享 Redis 消息监听容器
	 * <p>
	 * Bean 名称必须为 redisMessageListenerContainer（Spring Data Redis 默认容器名）： 1. Boot 4.1 的
	 * @RedisListener 自动配置按该名称回退，避免重复创建容器 2. @RedisListener 端点默认挂载到该名称的容器
	 */
	@Bean
	@ConditionalOnMissingBean
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		return container;
	}

}
