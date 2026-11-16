# MealTrack - Gestão de Restaurantes V1.0

Sistema full stack para monitoramento de desperdício de alimentos em restaurantes — MVP pronto para apresentação de banca.

---

## Sobre o projeto

O MealTrack simula um sistema real de controle operacional de buffet, com dados persistentes no banco. A arquitetura está preparada para futura integração com sensores Arduino (balanças/sensores de bandeja).

Funcionalidades principais:

- Autenticação com JWT e controle de acesso por perfil (Admin, Supervisor, Funcionário)
- Cadastro de usuários, restaurantes e alimentos
- Registro de produção
- Central de monitoramento operacional, com reposição de bandeja e histórico de eventos
- Dashboard gerencial com gráficos
- Relatórios de desperdício e eficiência, com exportação
- Simulação semanal com alteração real dos dados
- Tema claro/escuro e documentação interativa da API (Swagger)

---

## Tecnologias

| Camada | Stack |
|--------|-------|
| Frontend | React 19, Vite, Tailwind CSS, componentes estilo shadcn/ui (Radix UI), React Router 7, Axios, Recharts |
| Backend | Java 24, Spring Boot 3.5, Spring Data JPA, Lombok |
| Banco | MySQL |
| Segurança | Spring Security + JWT (stateless) |
| Documentação | OpenAPI / Swagger UI (springdoc) |

---

## Pré-requisitos

- **JDK 24** (versão definida no `pom.xml`)
- **Node.js** 20.19+ (ou 22+) e npm — exigência do Vite 8
- **MySQL** 8+ em execução na porta 3306

> O Maven não precisa ser instalado — o projeto usa o Maven Wrapper (`mvnw`).

---

## Como rodar

### 1. Banco de dados

```sql
CREATE DATABASE mealtrack;
```

As tabelas são criadas automaticamente na primeira execução (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Backend

Configure `mealtrack/src/main/resources/application.properties` com o usuário e a senha do seu MySQL:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mealtrack
spring.datasource.username=root
spring.datasource.password=SUA_SENHA
```

```bash
cd mealtrack
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

O backend inicia na porta **9090**.

### 3. Frontend

```bash
cd Front/mealtrack-frontend
npm install
cp .env.example .env.local    # opcional: só necessário se o backend não estiver em localhost:9090
npm run dev
```

O frontend inicia em **http://localhost:9091**.

### Endereços úteis

| Serviço | URL |
|---------|-----|
| Aplicação (frontend) | http://localhost:9091 |
| API (backend) | http://localhost:9090 |
| Swagger UI | http://localhost:9090/swagger-ui.html |

### Variáveis de ambiente (frontend)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `VITE_API_URL` | `http://localhost:9090` | URL base da API |

> O CORS do backend aceita as origens `http://localhost:5173`, `http://localhost:9090` e `http://localhost:9091` (definidas em `CorsOrigins.java`). Se o frontend rodar em outra porta, adicione-a nessa lista.

---

## Dados iniciais

Na primeira execução (banco vazio), o sistema carrega apenas o **mínimo necessário** para entrar e operar:

| Recurso | Quantidade |
|---------|------------|
| Restaurante | 1 (Restaurante Sabor Caseiro) |
| Usuários | 3 (admin, supervisor, 1 funcionário) |

Os **dados de demonstração** (alimentos, produções, consumo, descartes e eventos) são gerados pela **Simulação** — veja a seção abaixo. Antes de executar a primeira simulação, Dashboard, Alimentos, Monitoramento e Relatórios aparecem vazios.

### Login padrão

| Perfil | Email | Senha |
|--------|-------|-------|
| Administrador | `admin@mealtrack.com` | `123456` |
| Supervisor | `supervisor@mealtrack.com` | `123456` |
| Funcionário | `carlos@mealtrack.com` | `123456` |

---

## Perfis de acesso

| Funcionalidade | Admin | Supervisor | Funcionário |
|----------------|:-----:|:----------:|:-----------:|
| Dashboard | ✅ | ✅ | ✅ |
| Produção | ✅ | ✅ | ✅ |
| Monitoramento | ✅ | ✅ | ✅ |
| Simulação | ✅ | ✅ | ✅ |
| Alimentos (cadastro) | ✅ | ✅ | — |
| Relatórios | ✅ | ✅ | — |
| Usuários | ✅ | — | — |
| Restaurantes | ✅ | — | — |

As rotas são protegidas tanto no frontend (`ProtectedRoute`) quanto no backend (`SecurityConfig`).

---

## Simulação semanal

Na página **Simulação**, clique em **Executar Simulação**. O sistema evolui 7 dias de operação de forma real, gravando no banco:

- Funcionários produzem alimentos
- Clientes consomem
- Reposições acontecem
- Sobras são descartadas (única fonte de desperdício)
- Alimentos podem vencer e ser descartados
- Eventos são registrados no histórico

Se ainda não houver alimentos cadastrados, a simulação cria automaticamente um catálogo básico (Arroz Branco, Feijão Carioca, Frango Grelhado, Salada Verde e Macarrão ao Molho).

Após a simulação, Dashboard, Monitoramento, Alimentos e Relatórios refletem os novos dados.

> **Atenção:** os valores têm componente aleatório e **cada execução acumula** sobre os dados existentes (não substitui). Para voltar ao estado inicial, recrie o banco (`DROP DATABASE mealtrack; CREATE DATABASE mealtrack;`) e reinicie o backend.

---

## Regra de desperdício

```
Produção → Consumo → Reposições → Quantidade restante → Descarte = Desperdício
```

**Exemplo:** 50 kg produzidos, 44 kg consumidos, 6 kg restantes, 2 kg descartados → **Desperdício = 2 kg** → **Eficiência = 96%**

O que resta na bandeja **não** é desperdício. Apenas a sobra descartada conta.

- **Eficiência** = consumido ÷ (consumido + descartado)
- **Valor perdido** = kg descartados × valor por kg do alimento

### Status do alimento na bandeja

O status é calculado pelo percentual restante em relação à quantidade inicial:

| Status | Condição |
|--------|----------|
| `DISPONIVEL` | mais de 40% restante |
| `ATENCAO` | entre 10% e 40% restante |
| `CRITICO` | 10% ou menos restante |
| `ESGOTADO` | nada restante |
| `VENCIDO` | alimento vencido (descartado integralmente) |

A lógica fica centralizada em `CalculadoraDesperdicio.java`.

---

## Relatórios e exportação

A página **Relatórios** (Admin e Supervisor) mostra desperdício, eficiência e perda financeira por alimento e permite exportar os dados:

- **CSV** — `GET /relatorios/export/csv`
- **PDF** — `GET /relatorios/export/pdf` *(ver limitações conhecidas)*

---

## API

Documentação completa e testável em **Swagger UI** (`/swagger-ui.html`). Para chamar endpoints protegidos, faça login em `POST /usuarios/login` e informe o token no botão **Authorize**.

| Recurso | Endpoints |
|---------|-----------|
| Autenticação | `POST /usuarios/login` |
| Usuários | `GET/POST /usuarios`, `GET/PUT/DELETE /usuarios/{id}` |
| Restaurantes | `GET/POST /restaurantes`, `GET/PUT/DELETE /restaurantes/{id}` |
| Alimentos | `GET/POST /alimentos`, `GET/PUT/DELETE /alimentos/{id}` |
| Produções | `GET/POST /producoes`, `GET/PUT/DELETE /producoes/{id}` |
| Monitoramento | `GET /monitoramento`, `GET /monitoramento/summary`, `GET /monitoramento/history`, `POST /monitoramento/repor/{id}` |
| Dashboard | `GET /dashboard` |
| Relatórios | `GET /relatorios`, `GET /relatorios/export/csv`, `GET /relatorios/export/pdf` |
| Simulação | `POST /simulacao/executar` |

Erros seguem um formato padronizado (`ApiError`), inclusive os de autenticação (401) e permissão (403).

---

## Estrutura do repositório

```
Mealtrack/
├── mealtrack/                     Backend (Spring Boot)
│   └── src/main/java/com/mealtrack/
│       ├── config/                DataLoader, CORS, OpenAPI
│       ├── controller/            Endpoints REST
│       ├── service/               Regras de negócio e simulação
│       ├── repository/            Spring Data JPA
│       ├── model/                 Entidades e enums
│       ├── dto/ · mapper/         Objetos de transferência
│       ├── security/              JWT, filtro e SecurityConfig
│       ├── exception/             Tratamento global de erros
│       └── util/                  CalculadoraDesperdicio
└── Front/mealtrack-frontend/      Frontend (React + Vite)
    └── src/
        ├── pages/                 Telas (Dashboard, Produção, Alimentos, ...)
        ├── components/            ui/, layout/ e shared/
        ├── services/              Um service por recurso da API (Axios)
        ├── contexts/ · hooks/     Autenticação, tema, CRUD, paginação
        └── routes/                Rotas e proteção por perfil
```

Detalhes adicionais do frontend em [`Front/mealtrack-frontend/README.md`](Front/mealtrack-frontend/README.md).

---

## Limitações conhecidas

- **Exportação em PDF:** o endpoint `/relatorios/export/pdf` ainda retorna o conteúdo do relatório em texto/CSV com extensão `.pdf`; a geração de um PDF formatado ainda não foi implementada.
- **Monitoramento e Simulação** estão liberados no backend sem exigir token (`permitAll`), por conveniência na demonstração. Em produção, devem exigir autenticação.
- O segredo do JWT está fixo no código (`JwtService.java`) e as credenciais do banco ficam no `application.properties`; em produção, mova-os para variáveis de ambiente.
- As senhas padrão (`123456`) servem apenas para demonstração.
- Não há testes automatizados além do teste de carregamento de contexto do Spring.

---

## Próximos passos

- Integração com sensores/balanças Arduino para alimentar o consumo e o descarte em tempo real
- Geração real de relatórios em PDF
- Proteger os endpoints de monitoramento e simulação com autenticação
- Testes automatizados (backend e frontend)

---

## Equipe

- João Vitor Moreira
- Esdras
- Caique
- Felipe Borges
