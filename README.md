# SRS — Sistema de Reserva SIURB (Backend)

API REST para gerenciamento de **reservas de recursos corporativos** — auditório, veículos da frota e salas de reunião — com fluxo de aprovação, controle de acesso por grupos e anexos de arquivos.

Construído com **Spring Boot 4 / Java 21**, autenticação via **AWS Cognito** (OAuth2 / JWT), persistência em **PostgreSQL** com migrações **Flyway**, e implantação em **AWS ECS (Fargate)**.

---

## Sumário

- [Visão geral](#visão-geral)
- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Modelo de domínio](#modelo-de-domínio)
- [API](#api)
- [Segurança e perfis de acesso](#segurança-e-perfis-de-acesso)
- [Como rodar localmente](#como-rodar-localmente)
- [Configuração](#configuração)
- [Banco de dados e migrações](#banco-de-dados-e-migrações)
- [Testes](#testes)
- [Build e Docker](#build-e-docker)
- [Deploy (CI/CD)](#deploy-cicd)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Visão geral

O SRS permite que colaboradores solicitem a reserva de três tipos de recurso. Cada solicitação nasce com o status `ENVIADA_PARA_ANALISE` e segue um fluxo de aprovação conduzido por divisões responsáveis:

| Recurso | Divisão aprovadora |
|---|---|
| Auditório | `siurb-divisao-administrativa` |
| Sala de reunião | `siurb-divisao-administrativa` |
| Veículo da frota | `siurb-divisao-frotas` |

O grupo `siurb-administrador` tem acesso total, incluindo gestão de usuários e grupos no Cognito.

Cada reserva passa por **validações de negócio** (conflito de horário, intervalo mínimo entre reservas, capacidade, horário de início antes do fim, endereço de saída diferente do destino, etc.) antes de ser persistida.

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation, Actuator) |
| Segurança | Spring Security + OAuth2 Resource Server (JWT), AWS Cognito |
| Persistência | PostgreSQL + Hibernate/JPA |
| Migrações | Flyway |
| Mapeamento DTO ↔ Entidade | MapStruct + Lombok |
| Armazenamento de anexos | AWS EFS (sistema de arquivos montado) |
| SDK AWS | AWS SDK v2 (Cognito Identity Provider) |
| Build | Maven (wrapper incluído) |
| Runtime/Deploy | Docker → AWS ECR → AWS ECS (Fargate) |

## Arquitetura

Organização em camadas clássica do Spring:

```
Controller  →  Service  →  Repository  →  PostgreSQL
                  ↓
              Validators   (regras de negócio)
                  ↓
              Mapper (MapStruct)  DTO ↔ Entity
```

- **Controllers** expõem os endpoints REST e aplicam autorização por grupo (`@PreAuthorize`).
- **Services** concentram a lógica de negócio e orquestram validadores.
- **Validators** (`service/reservation/validators`) encapsulam as regras de cada tipo de reserva.
- **Mappers** convertem entre entidades JPA e DTOs de request/response.
- Integrações externas: **Cognito** (usuários/grupos) e **EFS** (upload/leitura de anexos).

## Modelo de domínio

Entidades principais (tabelas com prefixo `tbl_`, schema `db_srs`):

- `tbl_auditorium` — reservas de auditório
- `tbl_meeting_room` — reservas de sala de reunião (referencia `tbl_room`)
- `tbl_fleet_vehicles` — reservas de veículo (referencia `tbl_driver`)
- `tbl_room` — salas físicas
- `tbl_driver` — motoristas da frota
- `tbl_user` — usuários (e-mail / imagem)

**Enums:**

- `ReserveStatus`: `ENVIADA_PARA_ANALISE`, `APROVADA`, `REPROVADA`, `CANCELADA`, `FINALIZADA`
- `ReserveType`: `FLEET_VEHICLE`, `AUDITORIUM`, `MEETING_ROOM`
- `VehicleType`: `VAN`, `CARRO`

## API

Todos os endpoints têm o prefixo do host, ex.: `http://localhost:8080`.

### Reservas — Auditório · `/api/reservation/auditorium`
| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| POST | `/` | Cria reserva | Autenticado |
| GET | `/` | Lista reservas | Autenticado |
| GET | `/{id}` | Detalha reserva | Autenticado |
| GET | `/?year=&month=` | Lista por ano/mês | Autenticado |
| PUT | `/{id}` | Atualiza reserva | Autenticado |
| POST | `/{id}/approve` | Aprova | administrador · divisão administrativa |
| POST | `/{id}/reject` | Reprova | administrador · divisão administrativa |
| DELETE | `/{id}` | Remove | administrador · divisão administrativa |

### Reservas — Sala de reunião · `/api/reservation/meeting-room`
Mesmo conjunto de rotas do auditório. Aprovação/reprovação/remoção: **administrador · divisão administrativa**.

### Reservas — Veículo da frota · `/api/reservation/fleet-vehicle`
Mesmo conjunto de rotas. Aprovação/reprovação/remoção: **administrador · divisão de frotas**.

### Salas · `/api/room`
| Método | Rota | Descrição |
|---|---|---|
| GET | `/` | Lista salas |
| GET | `/{id}` | Detalha sala |

### Motoristas · `/api/driver`
| Método | Rota | Acesso |
|---|---|---|
| GET | `/` · `/{id}` | Autenticado |
| POST · PUT `/{id}` · DELETE `/{id}` | | administrador · divisão de frotas |

### Anexos (EFS) · `/api/anexos`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/` | Upload de arquivo (`multipart/form-data`, até 50 MB) |
| GET | `/img` | Recupera imagem |

### Usuários (Cognito) · `/api`
| Método | Rota | Acesso |
|---|---|---|
| GET | `/user` | Autenticado (dados do usuário logado) |
| GET | `/admin/list-users` | administrador |
| POST · DELETE · PATCH | `/admin/user` | administrador |

### Grupos (Cognito) · `/api/admin`
`GET /list-group`, `POST /group`, `PATCH /group`, `DELETE /group`, `POST /user/groups`, `POST /groups/users`, `POST /user/add-group`, `POST /user/remove-group` — todos restritos ao grupo **administrador**.

### Health check
`GET /actuator/health` (Spring Boot Actuator).

## Segurança e perfis de acesso

- Autenticação por **JWT emitido pelo AWS Cognito** (OAuth2 Resource Server). O `issuer-uri` aponta para o User Pool na região `sa-east-1`.
- Os grupos do Cognito são convertidos em *roles* do Spring (`CognitoGroupsConverter`) e verificados via `@PreAuthorize("hasAnyRole(...)")`.
- Grupos usados: `siurb-administrador`, `siurb-divisao-administrativa`, `siurb-divisao-frotas`.
- A configuração de segurança é ativada por **perfil**:
  - `SecurityConfig` + `CorsProdConfig` → perfil **`prod`** (validação de JWT ativa).
  - `DevSecurityConfig` + `CorsDevConfig` → perfil **`dev`** (segurança relaxada para desenvolvimento local).

## Como rodar localmente

**Pré-requisitos:** Java 21, PostgreSQL em execução. (Maven não é necessário — use o wrapper `./mvnw`.)

1. Crie um banco PostgreSQL e ajuste a URL/credenciais (veja [Configuração](#configuração)).
2. Rode com o perfil de desenvolvimento:

```bash
# Linux/macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Windows (PowerShell)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

A aplicação sobe em `http://localhost:8080`. O Flyway aplica as migrações automaticamente no start.

> O perfil padrão em `application.properties` é `prod`. Para desenvolvimento local, ative explicitamente o perfil `dev`, que relaxa a segurança e o CORS.

## Configuração

As propriedades ficam em `src/main/resources/`:

- `application.yaml` — configuração comum (nome da app, `issuer-uri` do Cognito, limites de upload, `app.efs.base-path`).
- `application.properties` — define o perfil ativo padrão (`prod`).
- `application-prod.properties` — configuração de produção.

Principais propriedades a ajustar para produção (hoje com placeholders `<host>` / `<dbname>`):

```properties
spring.datasource.url=jdbc:postgresql://<host>:5432/<dbname>?currentSchema=db_srs&ApplicationName=srs_app
spring.datasource.username=<usuario>
spring.datasource.password=<senha>
```

Outros pontos relevantes:

- `spring.jpa.hibernate.ddl-auto=validate` — o schema é gerido exclusivamente pelo Flyway; o Hibernate apenas valida.
- `app.efs.base-path=/app/anexos` — diretório (montagem EFS) onde os anexos são salvos.
- `spring.security.oauth2.resourceserver.jwt.issuer-uri` — User Pool do Cognito.

> ⚠️ **Segurança:** evite versionar segredos (senha do banco, `spring.temporary-password`). Prefira variáveis de ambiente ou AWS Secrets Manager em produção.

## Banco de dados e migrações

As migrações Flyway ficam em `src/main/resources/db/migration/` e são aplicadas automaticamente no schema `db_srs`:

| Versão | Descrição |
|---|---|
| V1 | Schema inicial (auditório, sala, veículo, motorista, usuário) |
| V2 | Seed de salas |
| V3 | Remove status `EM_ATENDIMENTO` |
| V4 | Remove salas 2 e 3 |
| V5 | Adiciona coluna em `tbl_user` |
| V6 | Corrige tipo do id em `tbl_room` |

## Testes

```bash
./mvnw test
```

Os testes usam **H2** em memória e cobrem controllers (incluindo segurança) e services (reservas, driver, EFS, Cognito).

> No build da imagem Docker os testes são pulados (`-DskipTests`) por dependerem de um PostgreSQL interno. Rode os testes no **CI ou localmente**, não no build de produção.

## Build e Docker

Gerar o JAR:

```bash
./mvnw clean package        # gera target/srs-backend.jar
```

Imagem Docker (multi-stage: build com Maven+JDK 21, runtime só JRE, usuário não-root):

```bash
docker build -t srs-backend .
docker run -p 8080:8080 srs-backend
```

A JVM respeita o limite de memória do container (`-XX:MaxRAMPercentage=75.0`) e recebe `SIGTERM` para shutdown gracioso no ECS.

## Deploy (CI/CD)

Deploy automatizado via **GitHub Actions** (`.github/workflows/deploy.yml`) a cada push na branch `main`:

1. Autentica na AWS via **OIDC** (sem chaves de longa duração).
2. Faz `build` e `push` da imagem para o **Amazon ECR**.
3. Renderiza a Task Definition com a nova imagem e faz **deploy no ECS**, aguardando estabilidade do serviço.

| Recurso | Valor |
|---|---|
| Região | `sa-east-1` |
| Cluster ECS | `exemplo` |
| Serviço | `srs-backend-service` |
| Repositório ECR | `srs-backend` |

A Task Definition de referência está em `deploy/task-definition.json`.

## Estrutura do projeto

```
src/main/java/com/siurbinfo/srs/
├── config/         # CORS (dev/prod)
├── controller/     # endpoints REST (reservation, room, driver, efs, cognito)
├── dto/            # objetos de request/response
├── entity/         # entidades JPA
├── enums/          # ReserveStatus, ReserveType, VehicleType
├── exception/      # exceções de domínio + GlobalExceptionHandler
├── mapper/         # mappers MapStruct
├── repository/     # repositórios Spring Data JPA
├── security/       # config de segurança, conversão de grupos Cognito
├── service/        # regras de negócio + validators + integrações (cognito, efs)
└── SrsApplication.java

src/main/resources/
├── application*.properties / .yaml
└── db/migration/   # migrações Flyway (V1..V6)
```

---

<sub>Projeto interno — SIURB · `com.siurbinfo:srs`</sub>
