package com.av2.gestaodeacao.e2e;

import com.av2.gestaodeacao.GestaoDeAcaoApplication;
import com.av2.gestaodeacao.domains.Acao;
import com.av2.gestaodeacao.domains.Corretora;
import com.av2.gestaodeacao.domains.Usuario;
import com.av2.gestaodeacao.domains.enums.Papel;
import com.av2.gestaodeacao.repositories.AcaoRepository;
import com.av2.gestaodeacao.repositories.CorretoraRepository;
import com.av2.gestaodeacao.repositories.UsuarioRepository;
import com.sun.net.httpserver.HttpServer;
import org.springframework.boot.SpringApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Launcher de teste: não faz parte do artefato de produção e nunca usa PostgreSQL/.env. */
public final class E2eApplication {
    public static void main(String[] args) throws Exception {
        Map<String, String> properties = Map.ofEntries(
                Map.entry("spring.config.import", ""),
                Map.entry("spring.profiles.active", "h2"),
                Map.entry("spring.devtools.restart.enabled", "false"),
                Map.entry("spring.datasource.url", "jdbc:h2:mem:e2e_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1"),
                Map.entry("spring.datasource.username", "sa"),
                Map.entry("spring.datasource.password", ""),
                Map.entry("spring.jpa.hibernate.ddl-auto", "validate"),
                Map.entry("spring.h2.console.enabled", "false"),
                Map.entry("server.address", "127.0.0.1"),
                Map.entry("server.port", "18080"),
                Map.entry("app.jwt.secret", UUID.randomUUID().toString() + UUID.randomUUID()),
                Map.entry("app.jwt.issuer", "gestao-e2e"),
                Map.entry("app.jwt.expiration", "30m"),
                Map.entry("integracoes.brapi.token", "e2e-provider-only"),
                Map.entry("twelvedata.api.key", "e2e-provider-only"),
                Map.entry("integracoes.brapi.base-url", "http://127.0.0.1:19090"),
                Map.entry("integracoes.twelvedata.base-url", "http://127.0.0.1:19090"),
                Map.entry("integracoes.brasilapi.base-url", "http://127.0.0.1:19090"),
                Map.entry("integracoes.viacep.base-url", "http://127.0.0.1:19090"),
                Map.entry("integracoes.cvm.base-url", "http://127.0.0.1:19090"),
                Map.entry("logging.level.root", "WARN"),
                Map.entry("spring.main.banner-mode", "off")
        );
        properties.forEach(System::setProperty);
        AtomicBoolean ready = new AtomicBoolean();
        Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 19090), 0);
        provider.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.computeIfAbsent(path, ignored -> new AtomicInteger()).incrementAndGet();
            int status = 501;
            String body = "{\"error\":true,\"message\":\"Endpoint não previsto no teste\"}";
            if (path.equals("/__e2e/ready")) {
                status = ready.get() ? 200 : 503;
                body = "{\"ready\":" + ready.get() + "}";
            } else if (path.equals("/__e2e/stats")) {
                status = 200;
                body = requests.entrySet().stream().map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                        .collect(java.util.stream.Collectors.joining(",", "{", "}"));
            } else if (path.startsWith("/api/quote/")) {
                String ticker = path.substring("/api/quote/".length());
                if (ticker.equals("ERRO3")) {
                    status = 401;
                    body = "{\"error\":true,\"message\":\"Autenticação recusada no provedor simulado\"}";
                } else if (java.util.Set.of("PETR4", "VALE3", "TAEE11", "ROXO34", "AUAU3").contains(ticker)) {
                    status = 200;
                    body = "{\"results\":[{\"symbol\":\"" + ticker + "\",\"longName\":\"Empresa E2E "
                            + ticker + "\",\"regularMarketPrice\":30,\"currency\":\"BRL\"}]}";
                } else {
                    status = 404;
                    body = "{\"error\":true,\"message\":\"Ticker não encontrado\"}";
                }
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        provider.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> provider.stop(0)));
        var context = SpringApplication.run(GestaoDeAcaoApplication.class);
        Usuario admin = new Usuario();
        admin.setUsername("e2e_admin");
        admin.setSenha(context.getBean(PasswordEncoder.class).encode("E2e-only-admin-123!"));
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);
        context.getBean(UsuarioRepository.class).save(admin);
        Corretora corretora = new Corretora();
        corretora.setCnpj("11222333000181");
        corretora.setNomeFantasia("Corretora E2E");
        corretora.setRazaoSocial("Corretora exclusiva dos testes");
        corretora.setValidadaNaCvm(true);
        corretora.setSituacaoCadastral("ATIVA");
        context.getBean(CorretoraRepository.class).save(corretora);
        for (String ticker : java.util.List.of("PETR4", "VALE3", "ERRO3", "TAEE11")) {
            Acao acao = new Acao();
            acao.setTicker(ticker);
            acao.setNomeEmpresa("Empresa E2E " + ticker);
            acao.setCotacaoAtual(10.0);
            acao.setMoeda("BRL");
            acao.setMercado("BR");
            acao.setCorretoraRelacionada(corretora);
            context.getBean(AcaoRepository.class).save(acao);
        }
        ready.set(true);
        System.out.println("E2E_BACKEND_READY: banco temporário e provedores locais");
    }
}
