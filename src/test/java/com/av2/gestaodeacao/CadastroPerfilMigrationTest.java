package com.av2.gestaodeacao;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;

class CadastroPerfilMigrationTest {
    @Test
    void preservaContasLegadasEImpedeCpfDuplicadoNoBanco() throws Exception {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:cadastro-perfil-migration;DB_CLOSE_DELAY=-1", "sa", "");
        var anterior = new SpringLiquibase();
        anterior.setDataSource(dataSource);
        anterior.setChangeLog("classpath:/db/changelog/pre-cpf.xml");
        anterior.afterPropertiesSet();
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO usuario(id,username,senha,papel,ativo) VALUES(1,'admin_legado','hash_teste','ADMIN',true)");
        jdbc.update("INSERT INTO usuario(id,username,senha,papel,ativo) VALUES(2,'user_legado','hash_teste','USER',true)");

        var atual = new SpringLiquibase();
        atual.setDataSource(dataSource);
        atual.setChangeLog("classpath:/db/changelog/db.changelog-master.xml");
        atual.afterPropertiesSet();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE cpf IS NULL AND telefone IS NULL AND ativo=true", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT senha FROM usuario WHERE id=1", String.class)).isEqualTo("hash_teste");
        jdbc.update("UPDATE usuario SET cpf='52998224725',telefone='11999999999' WHERE id=1");
        assertThatThrownBy(() -> jdbc.update("UPDATE usuario SET cpf='52998224725' WHERE id=2"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT cpf FROM usuario WHERE id=2", String.class)).isNull();
    }
}
