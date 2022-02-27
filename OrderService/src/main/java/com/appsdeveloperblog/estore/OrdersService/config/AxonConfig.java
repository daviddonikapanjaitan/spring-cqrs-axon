package com.appsdeveloperblog.estore.OrdersService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.AnyTypePermission;

@Configuration
public class AxonConfig {

	@Bean
    public XStream xStream() {
        XStream xStream = new XStream();
      
        xStream.allowTypesByWildcard(new String[] {
                "com.appsdeveloperblog.**"
        });
        xStream.addPermission(AnyTypePermission.ANY);
        xStream.allowTypes(new Class[] {com.appsdeveloperblog.estore.core.commands.ReserveProductCommand.class});
        
        return xStream;
    }
}