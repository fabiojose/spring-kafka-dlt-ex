package fabiojose.dlt.kafka;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author fabiojose
 */
@Component
@ConfigurationProperties(prefix = "app.kafka.dlt.excecoes")
@Slf4j
public class Excecoes {
    
    private List<Class<? extends Exception>> recuperaveis;
    private List<Class<? extends Exception>> naoRecuperaveis;

    @SuppressWarnings("unchecked")
    private List<Class<? extends Exception>> 
            parse(List<String> excecoes) {

        log.info("Parse da lista de exceções {}", excecoes);

        return 
            requireNonNull(excecoes)
                .stream()
                .map(className -> {
                    try{
                        return Class.forName(className);

                    }catch(ClassNotFoundException e){
                        throw new RuntimeException(e);
                    }
                })
                .map(e -> (Class<? extends Exception>)e)
                .collect(Collectors.toList());
    }

    public void setRecuperaveis(List<String> excecoes) {
        this.recuperaveis = parse(excecoes);
    }

    public void setNaoRecuperaveis(List<String> excecoes) {
        this.naoRecuperaveis = parse(excecoes);
    }

    /**
     * Exceções classificadas como recuperáveis.
     * <br>
     * A configuração é feita no {@code application.properties}, através
     * das propriedades {@code app.kafka.dlt.excecoes.recuperaveis[]}
     */
    public List<Class<? extends Exception>> getRecuperaveis() {
        return recuperaveis;
    }

    /**
     * Exceções classificadas como não-recuperáveis.
     * <br>
     * A configuração é feita no {@code application.properties}, através
     * das propriedades {@code app.kafka.dlt.excecoes.naoRecuperaveis[]}
     */
    public List<Class<? extends Exception>> getNaoRecuperavies() {
        return naoRecuperaveis;
    }
}