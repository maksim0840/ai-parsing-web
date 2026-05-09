package io.github.maksim0840.parsingtaskorchestrator.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoConfiguration {
    // Разрешаем сохранять спец-символы в json-ключах
    @Bean
    public BeanPostProcessor mappingMongoConverterPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName)
                    throws BeansException {

                if (bean instanceof MappingMongoConverter converter) {
                    converter.preserveMapKeys(true);
                }

                return bean;
            }
        };
    }
}
