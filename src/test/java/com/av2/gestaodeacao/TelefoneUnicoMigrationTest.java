package com.av2.gestaodeacao;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TelefoneUnicoMigrationTest {
    private DriverManagerDataSource bancoAnterior() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:telefone-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        migrar(ds, "classpath:/db/changelog/pre-telefone-unico.xml");
        return ds;
    }
    private void migrar(DriverManagerDataSource ds, String changelog) throws Exception {
        var liquibase = new SpringLiquibase();
        liquibase.setDataSource(ds);
        liquibase.setChangeLog(changelog);
        liquibase.afterPropertiesSet();
    }
    @Test
    void preservaNumerosEContasLegadasERejeitaInsertEUpdateDuplicados() throws Exception {
        var ds = bancoAnterior();
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO usuario(id,username,senha,papel,ativo,telefone) VALUES(1,'um','hash','USER',true,'17999998888')");
        jdbc.update("INSERT INTO usuario(id,username,senha,papel,ativo) VALUES(2,'dois','hash','USER',true),(3,'tres','hash','USER',true)");
        migrar(ds, "classpath:/db/changelog/db.changelog-master.xml");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE telefone IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT telefone FROM usuario WHERE id=1", String.class)).isEqualTo("17999998888");
        assertThatThrownBy(() -> jdbc.update("UPDATE usuario SET telefone='17999998888' WHERE id=2"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO usuario(id,username,senha,papel,ativo,telefone) VALUES(4,'quatro','hash','USER',true,'17999998888')"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE usuario SET telefone='17999998888' WHERE id=1");
        jdbc.update("UPDATE usuario SET telefone='2133334444' WHERE id=2");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario", Integer.class)).isEqualTo(3);
    }
    @Test
    void duplicidadePreExistenteInterrompeMigracaoSemExcluirOuModificarContas() throws Exception {
        var ds = bancoAnterior();
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO usuario(id,username,senha,papel,ativo,telefone) VALUES(1,'um','hash','USER',true,'17999998888'),(2,'dois','hash','USER',false,'17999998888')");
        assertThatThrownBy(() -> migrar(ds, "classpath:/db/changelog/db.changelog-master.xml"))
                .hasStackTraceContaining("Existem telefones duplicados");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE telefone='17999998888'", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM databasechangelog WHERE id='008-telefone-unico'", Integer.class)).isZero();
    }
}
