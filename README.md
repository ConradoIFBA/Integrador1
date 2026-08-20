# Integrador — Sistema de Gestão para Restaurante

Sistema web para gestão completa de um restaurante: cardápio, mesas, pedidos, fila de preparo, pagamentos e relatórios gerenciais — construído em **Java puro (Servlets + JSP)** sobre o padrão **MVC**, sem frameworks como Spring ou Struts.

> Projeto acadêmico desenvolvido para a disciplina **Projeto Integrador I** — Curso Superior em Computação, IFBA Campus Camaçari.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-Servlets%20%2B%20JSP-blue)](https://jakarta.ee/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/license-Acad%C3%AAmico-lightgrey)](#licença)

**Demonstração em vídeo:** [assista no YouTube](https://youtu.be/Yw9ps0nlBXg)
**Repositório:** [ConradoIFBA/Integrador1 — pasta `integrador v4`](https://github.com/ConradoIFBA/Integrador1/tree/main/integrador%20v4)

---

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Perfis de acesso](#perfis-de-acesso)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Como executar localmente](#como-executar-localmente)
- [Vídeo de demonstração](#vídeo-de-demonstração)
- [Documentação](#documentação)
- [Licença](#licença)

---

## Sobre o projeto

O **Integrador** digitaliza o fluxo completo de atendimento de um restaurante: do pedido — feito na mesa pelo cliente ou pelo atendente, ou lançado como delivery — até a entrega, passando pela gestão do cardápio, das mesas do salão e da fila de preparo por setor (cozinha, bebida e sobremesa), com relatórios gerenciais de faturamento para o gestor.

A aplicação segue o padrão **MVC** montado manualmente:

```
View (JSP)  →  Controller (Servlet)  →  DAO (JDBC)  →  Model  →  MySQL
```

## Perfis de acesso

| Perfil | Descrição | Principais telas |
|---|---|---|
| **GERENTE** | Acesso total ao sistema. | Dashboard, Cardápio, Mesas, Pedidos, Fila, Relatórios, Funcionários |
| **FUNCIONARIO** | Atendente ou cozinha (unificados). | Mesas, Cardápio, Pedidos, Fila |
| **USUARIO** (cliente) | Cliente final do restaurante. | Cardápio, Pedido na mesa, Delivery, Reserva, Meus Pedidos |

## Funcionalidades

- Autenticação com sessão e senhas protegidas por **hash BCrypt**
- CRUD de cardápio (itens + categorias por setor) com upload de imagem e exclusão lógica
- Gestão de mesas: abrir, fechar, reservar e chamar garçom
- Pedidos com máquina de estados `aberto → em_preparo → pronto → entregue`, cancelamento em cascata e pagamento (inclusive parcial/split)
- Fila de preparo segmentada por setor, com prioridade calculada
- Área do cliente: pedido na mesa, delivery, reserva e histórico "Meus Pedidos"
- Dashboard gerencial com gráficos de receita e vendas por categoria
- Relatórios de faturamento em **PDF** por período (via iText)
- Gestão de contas internas (funcionários e gerentes)
- Tema claro/escuro

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Back-end | Jakarta Servlets 6.0 + JSP 3.1 + JSTL |
| Front-end | JSP, CSS puro, JavaScript vanilla |
| Banco de dados | MySQL 8.0 (JDBC, `mysql-connector-j`) |
| Segurança | BCrypt (`jBCrypt`) |
| Relatórios | iText 5 (PDF) |
| Build | Maven |
| Servidor | Apache Tomcat 10+ |

## Arquitetura

```
br.com.restaurante
├── controller   → Servlets (rotas fixas via @WebServlet)
├── dao          → Acesso a dados (JDBC / PreparedStatement)
├── model        → Entidades do domínio (POJOs)
├── filter       → AuthFilter (sessão) e CharsetFilter (UTF-8)
└── utils        → Conexao, UploadImagemUtil, RelatorioPDF
```

**Banco de dados** — 8 tabelas principais: `usuario`, `mesa`, `categoria_item`, `cardapio`, `pedido`, `item_pedido`, `fila_preparo`, `pagamento`, com integridade referencial via chaves estrangeiras e exclusão lógica (`ativo`) na maior parte das entidades.

## Estrutura de pastas

```
integrador/
├── src/main/java/br/com/restaurante/
│   ├── controller/
│   ├── dao/
│   ├── model/
│   ├── filter/
│   └── utils/
├── src/main/resources/
│   └── db.properties
├── src/main/webapp/
│   ├── WEB-INF/views/       # JSPs (auth, dashboard, cardapio, mesa, pedido, fila, cliente, relatorio, staff)
│   ├── assets/              # CSS / JS
│   └── index.jsp
├── integrador_v3.sql        # script de criação do banco
└── pom.xml
```

## Como executar localmente

### Pré-requisitos

- JDK 21+
- Apache Maven
- Apache Tomcat 10+
- MySQL 8.0

### Passo a passo

1. **Clone o repositório**
   ```bash
   git clone https://github.com/ConradoIFBA/Integrador1.git
   cd "Integrador1/integrador v4"
   ```

2. **Crie o banco de dados**
   ```bash
   mysql -u root -p < integrador_v3.sql
   ```

3. **Configure a conexão com o banco**

   Edite `src/main/resources/db.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/integrador?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
   db.usuario=root
   db.senha=SUA_SENHA_AQUI
   ```

4. **Compile o projeto**
   ```bash
   mvn clean package
   ```

5. **Faça o deploy**

   Copie o `.war` gerado em `target/integrador.war` para a pasta `webapps` do Tomcat, ou publique o projeto diretamente pela sua IDE (Eclipse/IntelliJ) com o Tomcat configurado como servidor.

6. **Acesse o sistema**
   ```
   http://localhost:8080/integrador/
   ```

## Vídeo de demonstração

Confira o sistema em funcionamento: [youtu.be/Yw9ps0nlBXg](https://youtu.be/Yw9ps0nlBXg)

## Documentação

Documentação técnica completa (arquitetura, modelo de dados, design de componentes e telas) disponível na pasta `/docs` do repositório.

## Licença

Projeto acadêmico desenvolvido para fins educacionais na disciplina Projeto Integrador I — IFBA Campus Camaçari.
