# ArtixCloud Plugin

Plugin Minecraft profissional para criação e gerenciamento de servidores diretamente dentro do jogo, integrado à infraestrutura da **ArtixCloud**.

## Requisitos

| Componente | Versão mínima |
|---|---|
| Java | 21 |
| Maven | 3.8+ |
| Paper | 1.21.1 |
| MySQL | 8.0+ |

## Como compilar

```bash
# Clone ou extraia o projeto
cd artixcloud-plugin

# Compile com Maven
mvn clean package

# O JAR ficará em:
# target/ArtixCloud-1.0.0.jar
```

Ou use o script auxiliar:
```bash
chmod +x build.sh && ./build.sh
```

## Instalação

1. Copie `ArtixCloud-1.0.0.jar` para a pasta `plugins/` do servidor Paper.
2. Inicie o servidor uma vez para gerar os arquivos de configuração.
3. Edite `plugins/ArtixCloud/config.yml`:
   - Defina `api.token` com seu token da API ArtixCloud.
   - Configure as credenciais do banco de dados MySQL (`database.*`).
4. Reinicie o servidor.

## Configuração rápida (config.yml)

```yaml
api:
  base-url: "https://api.artixcloud.com/v1"
  token: "SEU_TOKEN_AQUI"

database:
  host: "localhost"
  port: 3306
  name: "artixcloud"
  username: "root"
  password: "senha"
```

## Comandos

| Comando | Descrição | Permissão |
|---|---|---|
| `/cloud` | Abre o painel principal | `artixcloud.use` |
| `/cloud criar` | Atalho para criar servidor | `artixcloud.use` |
| `/cloud status <id>` | Status de um servidor | `artixcloud.use` |
| `/cloud reload` | Recarrega configurações | `artixcloud.admin` |
| `/cloud help` | Lista de comandos | `artixcloud.use` |

Aliases: `/artix`, `/hospedagem`

## Permissões

| Permissão | Descrição | Padrão |
|---|---|---|
| `artixcloud.use` | Usar o plugin | `true` |
| `artixcloud.admin` | Acesso administrativo | `op` |
| `artixcloud.bypass.cooldown` | Ignorar cooldown | `op` |
| `artixcloud.bypass.limit` | Ignorar limite de servidores | `op` |

## Estrutura do projeto

```
com.artixcloud
├── ArtixCloud.java              ← Classe principal
├── api/
│   ├── APIClient.java           ← Cliente HTTP assíncrono (OkHttp)
│   └── APIResponse.java         ← Wrapper de resposta da API
├── commands/
│   ├── CloudCommand.java        ← Executor do /cloud
│   └── CloudTabCompleter.java   ← Tab-complete
├── menus/
│   ├── ArtixMenu.java           ← Classe base de menus GUI
│   ├── MenuManager.java         ← Gerenciador de estado dos menus
│   ├── MainMenu.java            ← Painel principal
│   ├── PlansMenu.java           ← Seleção de planos
│   ├── ServerMenu.java          ← Gerenciamento de servidor
│   ├── CreateServerMenu.java    ← Configuração de criação
│   ├── VersionMenu.java         ← Seleção de versão
│   └── ConfirmMenu.java         ← Confirmação de criação
├── listeners/
│   ├── MenuListener.java        ← Roteamento de cliques
│   └── PlayerListener.java      ← Join/quit/chat input
├── services/
│   ├── ServerService.java       ← Lógica de negócio de servidores
│   ├── PlayerService.java       ← Lógica de jogadores
│   └── PlanService.java         ← Gerenciamento de planos
├── repositories/
│   ├── PlayerRepository.java    ← CRUD jogadores (MySQL)
│   └── ServerRepository.java    ← CRUD servidores (MySQL)
├── database/
│   └── DatabaseManager.java     ← Pool HikariCP + DDL
├── models/
│   ├── CloudServer.java         ← Entidade servidor
│   ├── CloudPlayer.java         ← Entidade jogador
│   ├── Plan.java                ← Entidade plano
│   └── ServerStatus.java        ← Enum de status
├── cache/
│   └── CacheManager.java        ← Cache Caffeine
├── schedulers/
│   └── StatusScheduler.java     ← Atualização periódica de status
├── integrations/
│   └── ArtixCloudIntegration.java ← Interface de extensão
└── utils/
    ├── MessageUtil.java         ← Mensagens configuráveis
    ├── ItemBuilder.java         ← Builder fluente de ItemStack
    └── ColorUtil.java           ← Utilitários de cor
```

## Fluxo de criação de servidor

```
Jogador digita /cloud
       ↓
  MainMenu (painel)
       ↓
  PlansMenu (escolhe plano)
       ↓
  CreateServerMenu (versão + nome)
       ↓
  ConfirmMenu (resumo)
       ↓
  ServerService.createServer()
       ↓
  APIClient → POST /servers
       ↓
  API ArtixCloud cria o servidor
       ↓
  StatusScheduler atualiza em tempo real
```

## Dependências (bundled no JAR)

- **OkHttp 4.12** — requisições HTTP assíncronas
- **HikariCP 5.1** — pool de conexões MySQL
- **Caffeine 3.1** — cache em memória de alto desempenho
- **Gson 2.10** — serialização/deserialização JSON
- **MySQL Connector/J 8.3** — driver JDBC

---

Desenvolvido para a infraestrutura **ArtixCloud** | https://artix.cloud
