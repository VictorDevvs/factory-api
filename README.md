# factory-api

API REST para gerenciamento de insumos e otimização de produção industrial.

Construída com **Spring Boot 4**, **Java 21** e **PostgreSQL** (H2 para desenvolvimento).

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4 |
| Persistência | Spring Data JPA + Hibernate |
| Banco (dev) | H2 (em memória) |
| Banco (prod) | PostgreSQL |
| Validação | Jakarta Bean Validation |
| Boilerplate | Lombok |
| Testes | JUnit 5 + Mockito + AssertJ |
| Importação | OpenCSV |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- (Opcional, apenas para prod) PostgreSQL 14+

---

## Como rodar

### Desenvolvimento (H2 em memória)

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/factory-api.git
cd factory-api

# Sobe com perfil dev — banco H2, sem configuração extra
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`.

O console do banco H2 estará disponível em `http://localhost:8080/h2-console`:
- JDBC URL: `jdbc:h2:mem:factorydb`
- Username: `sa`
- Password: *(deixar vazio)*

### Produção (PostgreSQL)

Crie o banco no PostgreSQL e exporte as variáveis de ambiente:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/factory
export DB_USERNAME=seu_usuario
export DB_PASSWORD=sua_senha

./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## Rodar os testes

```bash
./mvnw test
```

Os testes utilizam H2 em memória — nenhuma configuração adicional é necessária.

Para ver o relatório de cobertura:

```bash
./mvnw verify
# Relatório gerado em: target/site/jacoco/index.html
```

---

## Estrutura do Projeto

```
src/
├── main/java/com/factory/
│   ├── config/
│   │   └── CorsConfig.java               # Liberação de CORS para o frontend Vue
│   ├── controller/
│   │   ├── RawMaterialController.java    # CRUD de matérias-primas
│   │   ├── ProductController.java        # CRUD de produtos
│   │   └── ProductionController.java     # Endpoint de otimização
│   ├── dto/
│   │   ├── ProductCompositionRequest     # Objeto de entrada da API
│   │   └── ProductionSuggestionResponse  # Objeto de saída da API
|   |   └── ProductRequest                # Objeto de entrada da API
|   |   └── RawMaterialRequest            # Objeto de entrada da API
|   |   └── CsvImportResponse             # Objeto de saída da API
│   ├── entity/
│   │   ├── RawMaterial.java              # Entidade: matéria-prima
│   │   ├── Product.java                  # Entidade: produto
│   │   └── ProductComposition.java       # Entidade: receita (relação produto ↔ insumo)
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java   # Tratamento centralizado de erros
│   │   └── ResourceNotFoundException.java
│   ├── repository/
│   │   ├── RawMaterialRepository.java
│   │   ├── ProductRepository.java        # Contém query com JOIN FETCH para evitar N+1
│   │   └── ProductCompositionRepository.java
│   └── service/
|       ├── CsvImportService.java
│       ├── RawMaterialService.java
│       ├── ProductService.java
│       └── ProductionOptimizationService.java  # Algoritmo greedy de otimização
│
└── test/java/com/factory/
    └── service/
        └── ProductionOptimizationServiceTest.java
        └── CsvImportServiceTest.java
```

---

## Endpoints

### Matérias-Primas

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/raw-materials` | Lista todas |
| `GET` | `/api/v1/raw-materials/{id}` | Busca por ID |
| `POST` | `/api/v1/raw-materials` | Cria nova |
| `POST` | `api/v1/raw-materials/import/simple` | Importa csv |
| `PUT` | `/api/v1/raw-materials/{id}` | Atualiza |
| `DELETE` | `/api/v1/raw-materials/{id}` | Remove |

**Exemplo de body (POST/PUT):**
```json
{
  "code": "FL001",
  "name": "Farinha de Trigo",
  "stockQuantity": 5000,
  "unit": "g"
}
```

### Produtos

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/products` | Lista todos com composições |
| `GET` | `/api/v1/products/{id}` | Busca por ID |
| `POST` | `/api/v1/products` | Cria novo |
| `PUT` | `/api/v1/products/{id}` | Atualiza |
| `DELETE` | `/api/v1/products/{id}` | Remove |

**Exemplo de body (POST/PUT):**
```json
{
  "code": "BRD001",
  "name": "Pão Artesanal",
  "saleValue": 8.50,
  "compositions": [
    { "rawMaterialId": 1, "requiredQuantity": 200 },
    { "rawMaterialId": 2, "requiredQuantity": 50 }
  ]
}
```

### Otimização de Produção

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/v1/production/optimize` | Retorna sugestão de produção |

**Exemplo de resposta:**
```json
{
  "suggestions": [
    {
      "productCode": "CK001",
      "productName": "Bolo",
      "quantityToProduce": 3,
      "unitValue": 15.00,
      "totalItemValue": 45.00
    },
    {
      "productCode": "BRD001",
      "productName": "Pão Artesanal",
      "quantityToProduce": 5,
      "unitValue": 8.50,
      "totalItemValue": 42.50
    }
  ],
  "totalValue": 87.50
}
```

---

## Algoritmo de Otimização

O endpoint `/api/v1/production/optimize` analisa o estoque atual e sugere quais produtos fabricar para maximizar o valor total de venda.

### Estratégia: Greedy (Algoritmo Guloso)

1. Busca todos os produtos com suas composições em uma única query (JOIN FETCH)
2. Ordena os produtos por `saleValue` de forma decrescente
3. Para cada produto, calcula quantas unidades é possível produzir dado o estoque disponível: `floor(estoque / quantidade_necessária)` para cada insumo — o limitante é o insumo mais escasso
4. Produz o máximo possível, consome o estoque virtualmente e avança para o próximo
5. Retorna a lista de sugestões e o valor total

> **Nenhuma alteração é feita no banco.** O cálculo opera sobre um snapshot do estoque em memória.

### Trade-off documentado

O algoritmo guloso **não garante o ótimo global** em todos os cenários. Exemplo: se um produto de alto valor e um de médio valor disputam o mesmo insumo escasso, pode ser mais rentável produzir dois do produto médio do que um do produto caro.

A solução ótima garantida exigiria **Programação Linear Inteira (ILP)**, com custo de implementação e dependência de um solver externo. Para o volume e complexidade esperados neste sistema, o greedy oferece resultados suficientemente bons com complexidade O(n log n).

---

## Decisões de Arquitetura

### Por que Spring Boot e não Quarkus?
Ecossistema mais maduro, melhor suporte a testes com MockMvc e Testcontainers, e curva de aprendizado mais acessível. Quarkus teria vantagem em startup time e footprint de memória para deploys nativos/GraalVM, cenário fora do escopo deste projeto.

### Por que H2 em dev e PostgreSQL em prod?
H2 em memória elimina dependência de infraestrutura para rodar o projeto localmente. O perfil de prod usa `ddl-auto=validate` — o Hibernate valida o schema existente sem alterá-lo, comportamento seguro para ambientes de produção.

### Por que `FetchType.LAZY` nas composições?
Evita o problema N+1: sem LAZY, o JPA carregaria automaticamente todas as composições a cada produto buscado, gerando uma query por produto. Com LAZY + `JOIN FETCH` explícito no repository, tudo é carregado em uma única query quando necessário.

### Por que DTOs separados das entidades?
Entidades JPA carregam anotações de banco e podem ter referências circulares que quebram a serialização JSON. DTOs são o contrato público da API — estáveis, sem vazamento de detalhes de persistência e sem risco de referência circular.

---

## Variáveis de Ambiente (Produção)

| Variável | Descrição |
|---|---|
| `DB_URL` | JDBC URL do PostgreSQL |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |

---

## Victor Santos

Desenvolvido como teste técnico para vaga de Desenvolvedor Fullstack Júnior.



