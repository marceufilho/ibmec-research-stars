# IBMEC Research Stars — Requisitos da API

> API REST de back-end para gerenciar as publicações de pesquisa dos professores, a validação pelo administrador e os relatórios de conformidade com o MEC/CAPES.
> **Stack:** Java + Spring Boot · **Testes:** JUnit 5 (incluindo testes funcionais)

---

## 1. Visão Geral do Projeto

O **IBMEC Research Stars** permite que professores se cadastrem e *mantenham* suas publicações de pesquisa. Um administrador *mantém* os professores (consultar, aprovar, editar, excluir), valida as publicações, consulta a produção científica consolidada e consulta rankings/dashboards — acompanhando quantos professores atingem a meta do MEC de **no mínimo 9 publicações validadas nos últimos 3 anos**, segmentada por curso.

Este documento cobre **apenas a API** (sem front-end). O modelo de casos de uso tem dois atores — **Professor** e **Admin** — com os fluxos descritos a seguir.

---

## 2. Atores e Casos de Uso

Extraídos do diagrama de casos de uso. Observação: *Manter* significa CRUD completo — criar, ler, atualizar e excluir.

| Ator | Casos de uso |
|------|--------------|
| **Professor** | **Efetuar Cadastro** (autocadastro), **Manter Publicações** (CRUD das próprias publicações), **Exibir Ranking** (ver *somente a sua própria* posição no ranking). |
| **Admin** | **Manter Professores** (consultar, aprovar, editar, excluir professores), **Consultar Produção Científica** (consultar toda a produção científica / publicações), **Consultar Ranking — Dashboards** (ranking completo + dashboards de conformidade por curso). |

O administrador deve conseguir **consultar e pesquisar tudo**: cada recurso expõe um endpoint de listagem ("get all") com filtros para o administrador (ver a Seção 5.6 e a tabela de endpoints).

---

## 3. Premissas e Decisões a Confirmar

Estes pontos não foram totalmente especificados — escolhi um padrão sensato para cada um. Por favor, confirme ou corrija.

1. **O que conta para os "9"**: somente publicações com status `VALIDATED` contam. `PENDING` e `REJECTED` não contam.
2. **Janela dos "últimos 3 anos"**: janela móvel baseada na `publicationDate` de cada publicação, medida a partir de *hoje* (ex.: em 28/05/2026, a janela é 28/05/2023 → 28/05/2026). **Não** é por ano-calendário.
3. **O link da publicação é obrigatório** — a API rejeita a criação sem uma URL válida.
4. **Métrica do ranking**: os professores são ordenados pela **quantidade de publicações validadas nos últimos 3 anos** (assim o ranking e o relatório de conformidade usam o mesmo número). Confirme se prefere a contagem de todo o histórico.
5. **A conta de administrador** é *criada na implantação* (seed), não por autocadastro. Apenas professores se autocadastram.
6. **Autenticação** usa tokens JWT (stateless). Confirme se prefere sessões.
7. Um professor precisa estar `APPROVED` antes que suas publicações contem em qualquer relatório. (Convém confirmar se um professor `PENDING` ainda pode cadastrar publicações — padrão adotado: sim, ele pode cadastrar, mas as publicações ficam invisíveis nos relatórios até o professor ser aprovado.)
8. **A lista de cursos** é gerenciada pelo administrador; o professor escolhe entre os cursos existentes no cadastro. Ele não pode criar cursos.

---

## 4. Modelo de Domínio

### Entidades

**User** (base da autenticação)
- `id`, `email` (único), `passwordHash`, `role` (`ADMIN` | `PROFESSOR`)

**Professor** (estende/relaciona-se com User)
- `id`
- `name`
- `email` (único)
- `lattesNumber` (único — identificador do currículo Lattes)
- `status` (`PENDING` | `APPROVED`)
- `courses` (muitos-para-muitos com Course)
- `createdAt`

**Course**
- `id`
- `name`
- `code` (único)

**Publication**
- `id`
- `title`
- `link` (obrigatório, URL válida)
- `publicationDate`
- `professor` (dono, muitos-para-um)
- `status` (`PENDING` | `VALIDATED` | `REJECTED`)
- `validatedBy` (usuário admin, anulável)
- `validatedAt` (anulável)
- `createdAt`

### Relacionamentos
- `Professor` ↔ `Course`: muitos-para-muitos (um professor leciona vários cursos; um curso tem vários professores).
- `Professor` → `Publication`: um-para-muitos.

---

## 5. Requisitos Funcionais

### Autenticação e Cadastro
- **RF-01** O professor pode se autocadastrar informando **nome, e-mail, número do Lattes e os cursos que leciona**. O status inicial é `PENDING`.
- **RF-02** E-mail e número do Lattes devem ser únicos; o cadastro com duplicidade é rejeitado.
- **RF-03** Um usuário (professor ou administrador) pode se autenticar e receber um token de acesso.

### Gestão de Professores — *Manter Professores* (Admin)
- **RF-04** O administrador pode listar todos os professores, com filtro por status (ex.: apenas `PENDING`) e busca textual por nome/e-mail/Lattes.
- **RF-05** O administrador pode consultar (visualizar) um professor específico e seus dados.
- **RF-06** O administrador pode aprovar um professor `PENDING` (status → `APPROVED`).
- **RF-07** O administrador pode editar os dados de um professor (nome, cursos; e-mail/Lattes conforme RN-05).
- **RF-08** O administrador pode excluir um professor (o que também remove/trata suas publicações).
- **RF-09** O professor pode visualizar o próprio perfil.

### Gestão de Publicações — *Manter Publicações* (Professor) + validação (Admin)
- **RF-10** O professor pode cadastrar uma publicação para si, informando **título, link (obrigatório) e data de publicação**.
- **RF-11** Uma nova publicação começa como `PENDING`.
- **RF-12** O professor pode listar e visualizar **somente as suas próprias** publicações.
- **RF-13** O professor pode editar e excluir **somente as suas próprias** publicações. (Confirmar se editar uma publicação já `VALIDATED` a retorna para `PENDING` — padrão recomendado: sim, a edição reabre a validação.)
- **RF-14** O administrador pode listar e visualizar **todas** as publicações, com filtro por status, por professor e busca textual por título.
- **RF-15** O administrador pode validar uma publicação (status → `VALIDATED`, registrando quem/quando).
- **RF-16** O administrador pode rejeitar uma publicação (status → `REJECTED`).
- **RF-17** O administrador pode excluir qualquer publicação.
- **RF-18** Uma publicação sem link válido não pode ser criada nem validada.

### Produção Científica — *Consultar Produção Científica* (Admin)
- **RF-19** O administrador pode consultar a produção científica consolidada de qualquer professor — ou seja, recuperar todas as publicações de um determinado professor em uma única chamada.
- **RF-20** O administrador pode consultar/pesquisar em todos os recursos. Cada coleção (professores, publicações, cursos) expõe um endpoint de listagem ("get all") restrito ao administrador, com filtro, busca textual e paginação.

### Dashboard de Conformidade — parte de *Consultar Ranking / Dashboards* (Admin)
- **RF-21** O administrador pode obter, **para cada curso**, o **percentual de professores** que possuem **≥ 9 publicações validadas nos últimos 3 anos**.
- **RF-22** O relatório expõe os números subjacentes por curso (quantidade de conformes / total de professores) para que o percentual seja auditável.

### Rankings — *Exibir Ranking* (Professor) / *Consultar Ranking* (Admin)
- **RF-23** O administrador pode ver o **ranking completo** dos professores, ordenado pela quantidade de publicações validadas (últimos 3 anos), de forma decrescente.
- **RF-24** O professor pode ver **somente a sua própria** posição no ranking (seu número de classificação e sua contagem de publicações) — não a lista completa.

### Cursos
- **RF-25** O administrador pode criar, listar, editar e excluir cursos.
- **RF-26** Qualquer usuário autenticado pode listar cursos (necessário para o autocadastro do professor).

---

## 6. Regras de Negócio

- **RN-01** Uma publicação conta para a conformidade com o MEC apenas se `status = VALIDATED` **e** a `publicationDate` estiver dentro dos últimos 3 anos a partir da data atual.
- **RN-02** Um professor conta nos números de conformidade de um curso apenas se `status = APPROVED`.
- **RN-03** Percentual de conformidade do curso = (professores do curso com ≥ 9 publicações qualificadas) ÷ (total de professores aprovados no curso) × 100. Se um curso tiver zero professores, o percentual é reportado como 0 (ou `N/A` — confirmar).
- **RN-04** Um professor pertence a vários cursos; uma única publicação qualificada pode fazê-lo contar em *todos* os cursos que ele leciona.
- **RN-05** Número do Lattes e e-mail são identificadores imutáveis após o cadastro (ou exigem ação do administrador para alteração — confirmar).

---

## 7. Endpoints da API (Rascunho)

Caminho base: `/api/v1`. As funções entre colchetes indicam quem pode chamar.

| Método | Caminho | Descrição | Acesso |
|--------|---------|-----------|--------|
| POST | `/auth/register` | Autocadastro do professor | Público |
| POST | `/auth/login` | Autenticar, retornar token | Público |
| GET | `/professors` | **Listar todos** os professores (filtro `?status=`, busca `?q=`, paginado) | Admin |
| GET | `/professors/{id}` | Consultar um professor | Admin |
| GET | `/professors/{id}/publications` | Consultar a produção científica completa de um professor | Admin |
| GET | `/professors/me` | Próprio perfil | Professor |
| POST | `/professors/{id}/approve` | Aprovar um professor | Admin |
| PATCH | `/professors/{id}` | Editar um professor | Admin |
| DELETE | `/professors/{id}` | Excluir um professor | Admin |
| POST | `/publications` | Cadastrar a própria publicação | Professor |
| GET | `/publications` | **Listar todas** as publicações (filtro `?status=` `?professorId=`, busca `?q=`, paginado) | Admin |
| GET | `/publications/me` | Listar as próprias publicações | Professor |
| GET | `/publications/{id}` | Visualizar uma publicação | Admin / dono |
| PATCH | `/publications/{id}` | Editar uma publicação | Dono (própria) / Admin |
| POST | `/publications/{id}/validate` | Validar uma publicação | Admin |
| POST | `/publications/{id}/reject` | Rejeitar uma publicação | Admin |
| DELETE | `/publications/{id}` | Excluir uma publicação | Dono (própria) / Admin |
| GET | `/courses` | **Listar todos** os cursos | Autenticado |
| POST | `/courses` | Criar um curso | Admin |
| PATCH | `/courses/{id}` | Editar um curso | Admin |
| DELETE | `/courses/{id}` | Excluir um curso | Admin |
| GET | `/reports/course-compliance` | % de professores conformes por curso | Admin |
| GET | `/rankings` | Ranking completo dos professores | Admin |
| GET | `/rankings/me` | Própria posição no ranking | Professor |

Convenções:
- Substantivos no plural para coleções; mudanças de estado como sub-recursos verbais (`/approve`, `/validate`).
- Todo endpoint de listagem ("get all") do administrador suporta paginação `?page=` / `?size=`, ordenação e um parâmetro de busca textual `?q=`.
- Corpos de requisição/resposta usam DTOs, nunca entidades diretamente.
- Códigos de status padrão: `201` na criação, `200` na leitura/atualização, `204` na exclusão, `400` erro de validação, `401/403` autenticação, `404` não encontrado, `409` conflito (e-mail/Lattes duplicado).

---

## 8. Requisitos Não Funcionais

- **RNF-01** Autenticação stateless (JWT); senhas armazenadas com hash (BCrypt).
- **RNF-02** Autorização baseada em papéis (roles) aplicada no nível do endpoint (Spring Security).
- **RNF-03** Validação de entrada em todos os DTOs (Bean Validation / `jakarta.validation`).
- **RNF-04** Formato de resposta de erro consistente (ex.: um corpo `ApiError` padrão com timestamp, status, mensagem e erros de campo).
- **RNF-05** Documentação da API via OpenAPI/Swagger (springdoc-openapi).
- **RNF-06** Migrações de banco de dados versionadas (Flyway ou Liquibase).
- **RNF-07** Arquitetura em camadas: Controller → Service → Repository, com o mapeamento DTO ↔ entidade isolado (MapStruct ou mapeadores manuais).

---

## 9. Estratégia de Testes (JUnit)

- **JUnit 5 (Jupiter)** como framework de testes.
- **Mockito** para mockar dependências em testes unitários.
- **AssertJ** para asserções fluentes (mais legíveis que as asserções puras do JUnit).
- **Test slices do Spring Boot**:
    - `@WebMvcTest` + **MockMvc** para testes da camada de controllers.
    - `@DataJpaTest` para testes de repositórios.
    - `@SpringBootTest` para testes de integração selecionados.
- **Testcontainers** para testes de integração contra um banco real (recomendado se usar PostgreSQL/MySQL, para evitar que os testes passem no H2 mas falhem em produção).
- **Testes funcionais** (indicados como *Testes Funcionais* no diagrama de casos de uso): testes ponta a ponta que exercitam cada caso de uso através da API, verificando o resultado esperado e as regras de autorização. No mínimo cobrir: autocadastro do professor → aprovação pelo admin; cadastro de publicação → validação pelo admin; o percentual de conformidade por curso; o ranking completo (admin) vs. somente o próprio ranking (professor); e que os endpoints de "listar tudo"/busca do administrador retornam os dados esperados enquanto o professor recebe `403` (proibido) ao acessá-los. Implementar com `@SpringBootTest` + MockMvc (ou REST-assured) contra um banco Testcontainers.
- **JaCoCo** para medição de cobertura; definir um limite mínimo (ex.: 80% na camada de serviço) imposto no build.
- Concentrar os testes mais pesados nas **regras de negócio**: a janela de 3 anos, o limite de ≥ 9 e o cálculo do percentual por curso (é aí que os bugs prejudicam o resultado do MEC).
- Opcional: **ArchUnit** para garantir a arquitetura em camadas (ex.: controllers nunca chamam repositórios diretamente).

---

## 10. Qualidade de Código, Nomenclatura e Ferramentas

### Convenções de nomenclatura
Siga o **Google Java Style Guide** como base. Concretamente:

| Elemento | Estilo | Exemplo |
|----------|--------|---------|
| Classes / interfaces / enums | PascalCase | `PublicationService`, `ProfessorStatus` |
| Métodos e variáveis | camelCase | `validatePublication`, `lattesNumber` |
| Constantes (`static final`) | UPPER_SNAKE_CASE | `MIN_PUBLICATIONS = 9` |
| Pacotes | tudo minúsculo, sem underscores | `com.suaorg.publications.service` |
| Booleanos | prefixo `is` / `has` | `isValidated`, `hasMinimumPublications` |
| Componentes Spring | sufixo do papel | `...Controller`, `...Service`, `...Repository`, `...Dto` |
| Classes de teste | `<ClasseSobTeste>Test` | `PublicationServiceTest` |

Evite abreviações (`pub` → `publication`) e nomeie pela intenção, não pelo tipo (`approvedProfessors`, não `list1`).

### Analisadores estáticos e linters (Java)
Stack recomendada, do mais leve ao mais completo:

1. **Spotless** (com `google-java-format`) — formatador automático. Rode a cada build para que a formatação nunca seja discutida. Esta é a sua camada de "lint/format".
2. **Checkstyle** — aplica as regras de estilo Google/Sun (nomenclatura, imports, Javadoc, comprimento de linha). Use o `google_checks.xml` como ponto de partida.
3. **PMD** — detecta code smells, código morto, métodos excessivamente complexos e más práticas.
4. **SpotBugs** (sucessor do FindBugs) — detecção estática de bugs (desreferência de nulos, vazamento de recursos etc.). Adicione o plugin **FindSecBugs** para questões de segurança.
5. **Error Prone** (Google) — detecta erros comuns em tempo de compilação.
6. **SonarQube / SonarLint** — a opção tudo-em-um, que sobrepõe as anteriores e adiciona notas de manutenibilidade/segurança e um quality gate. O **SonarLint** roda na IDE; o **SonarQube** no CI. Se adotar o Sonar, pode manter o Spotless + Checkstyle para formatação e deixar o Sonar cobrir o resto, em vez de rodar PMD/SpotBugs separadamente.

Um setup mínimo e prático para um projeto acadêmico/MEC: **Spotless + Checkstyle + SpotBugs + JaCoCo**, todos integrados ao build Maven/Gradle de modo que o build falhe em caso de violações. Adicione o **SonarLint** na IDE de graça.

### Integração com o build
- Coloque Checkstyle/PMD/SpotBugs na fase `verify` (Maven) ou na task `check` (Gradle) para que rodem no CI.
- Faça o build falhar se a cobertura ficar abaixo do limite (JaCoCo) e diante de qualquer violação de estilo de nível "error".
- Gere a especificação OpenAPI como parte do build para documentação.