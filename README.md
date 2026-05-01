**Descrição**

API REST desenvolvida em **Java com Spring Boot** para gerenciamento de **corretoras** e **ações financeiras**, com integração a APIs externas para validação e enriquecimento de dados.

O sistema permite cadastrar e consultar informações de corretoras a partir do CNPJ e obter dados de ações com base no ticker informado.


 **Tecnologias Utilizadas**

* Java
* Spring Boot
* Spring Data JPA
* Hibernate
* Banco de dados H2
* Maven
*Lombok

**APIs Externas Utilizadas**

* BrasilAPI → consulta de CNPJ
* ViaCEP → consulta de CEP
* Brapi → cotação de ações brasileiras

## 🗄️ Banco de Dados

O projeto utiliza o banco **H2 em memória**.

Console disponível em:

```
http://localhost:8080/h2-console
```

**Configurações padrão**:

* JDBC URL: `jdbc:h2:mem:testdb`
* User: `sa`
* Password: (vazio)

**Endpoints da API**

**Corretoras**

* `POST /corretoras`
* `GET /corretoras`
* `GET /corretoras/{id}`
* `GET /corretoras/cnpj/{cnpj}`

---

**Ações**

* `POST /acoes`
* `GET /acoes`
* `GET /acoes/{id}`
* `GET /acoes/ticker/{ticker}`
* `PUT /acoes/{id}/atualizar-cotacao`


## 📥 Exemplos de Requisição

### ➤ Criar Corretora

```json
{
  "cnpj": "00000000000191",
  "cep": "01001000"
}
```

---

### ➤ Criar Ação

```json
{
  "ticker": "PETR4"
}
```

---

**Regras de Negócio Implementadas**

* Validação de CNPJ via API externa
* Validação de CEP via API externa
* Impede cadastro duplicado de corretoras (CNPJ)
* Impede cadastro duplicado de ações (ticker)
* Validação de existência do ticker na API
* Identificação de mercado (BR/EUA)
* Atualização de cotação de ações
* Preenchimento automático dos dados via APIs

**Tratamento de Erros**

A aplicação trata os seguintes cenários:

* CNPJ inválido
* CEP inválido
* Ticker inexistente
* APIs externas indisponíveis
* Tentativa de cadastro duplicado

**Arquitetura do Projeto**

O sistema segue arquitetura em camadas:

* **Controller (Resource)** → entrada das requisições
* **Service** → regras de negócio
* **Repository** → acesso ao banco de dados
* **DTO** → transferência de dados
* **Entity** → representação das tabelas

---

## 📊 Diagrama

O diagrama da estrutura do projeto está disponível no arquivo:

```
gestaodeacao.png
```

---

**Justificativa das APIs**

Foram utilizadas APIs externas conforme os requisitos do trabalho:

* BrasilAPI para validação de CNPJ
* ViaCEP para obtenção de dados de endereço
* Brapi para consulta de ações

A escolha foi baseada na necessidade de integrar dados reais ao sistema, garantindo validação e consistência das informações.

---

**Observações**

* O sistema depende de APIs externas, podendo sofrer instabilidade em caso de indisponibilidade dos serviços
* Alguns CEPs podem não ser encontrados dependendo da base da API
* Para testes, recomenda-se utilizar dados válidos conhecidos

---
