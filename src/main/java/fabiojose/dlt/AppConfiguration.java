package fabiojose.dlt;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações da aplicação
 * 
 * @author fabiojose
 *
 */
@Configuration
public class AppConfiguration {

    @Autowired
    BuildProperties buildProperties;

    @PostConstruct
    public void init() {
        System.setProperty("APP_NAME", buildProperties.getName());
    }
}
