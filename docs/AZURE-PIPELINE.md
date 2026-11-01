# Pipeline CI/CD no Azure DevOps — kura-tutor-api

Guia para criar, executar e operar a pipeline definida em [`azure-pipelines.yml`](../azure-pipelines.yml) (raiz do
repositório). Tudo roda no **Windows, sem WSL**: o agente é um programa Windows e os passos usam cmd e PowerShell.

## Índice

- [Visão geral](#visão-geral)
- [Roteiro em 8 etapas](#roteiro-em-8-etapas)
- [Etapa 1: Preparar o PC](#etapa-1-preparar-o-pc)
- [Etapa 2: Criar a organização e o projeto](#etapa-2-criar-a-organização-e-o-projeto)
- [Etapa 3: Convidar o professor](#etapa-3-convidar-o-professor)
- [Etapa 4: Instalar o agente self-hosted](#etapa-4-instalar-o-agente-self-hosted)
- [Etapa 5: Criar a service connection](#etapa-5-criar-a-service-connection)
- [Etapa 6: Publicar o YAML e criar a pipeline](#etapa-6-publicar-o-yaml-e-criar-a-pipeline)
- [Etapa 7: Executar e conferir](#etapa-7-executar-e-conferir)
- [Etapa 8: Confirmar o disparo automático](#etapa-8-confirmar-o-disparo-automático)
- [Checklist da entrega e roteiro do vídeo](#checklist-da-entrega-e-roteiro-do-vídeo)
- [Testes no Azure DevOps](#testes-no-azure-devops)
- [Variáveis e segredos](#variáveis-e-segredos)
- [Operação do dia a dia](#operação-do-dia-a-dia)
- [Convivência com o deploy.sh e o GitHub Actions](#convivência-com-o-deploysh-e-o-github-actions)
- [Solução de problemas](#solução-de-problemas)
- [Plano B se o deploy pela task falhar](#plano-b-se-o-deploy-pela-task-falhar)

## Visão geral

Um único arquivo, dois estágios:

```
push na main ─► CI: Build ──────────────────────────────► CD: Deploy
                 verifica JDK 21                           baixa o artefato app.jar
                 mvnw.cmd verify (testes, perfil dev/H2)   AzureWebApp@1 → kura-tutor-api
                 publica resultados dos testes             health check (/api/actuator/health)
                 publica o artefato (app.jar)
```

| Regra da entrega | Onde está atendida |
|---|---|
| I. Conectada ao repositório da aplicação | O arquivo vive neste repositório e a pipeline é criada a partir dele ([Etapa 6](#etapa-6-publicar-o-yaml-e-criar-a-pipeline)) |
| II. CI a cada alteração na branch principal | `trigger` com `branches: include: [main]`. O slide diz `master`; o repositório do tutor usa `main`, então avise isso no vídeo |
| III. CD após novo artefato | O estágio `Deploy` tem `dependsOn: Build` e só roda com o CI verde, depois do artefato publicado |
| IV. Dados sensíveis protegidos | A pipeline não guarda segredo nenhum ([Variáveis e segredos](#variáveis-e-segredos)) |
| V. Artefato publicado no Azure DevOps | `PublishPipelineArtifact@1` publica `tutor-jar` (contém `app.jar`) |
| VI. Etapa de testes | `mvnw.cmd verify` + `PublishTestResults@2` (aba **Tests** da execução) |
| VII. Deploy em Azure Web App | `AzureWebApp@1` em `kura-tutor-api` |

## Roteiro em 8 etapas

| # | Etapa | Onde | Você sabe que terminou quando |
|---|---|---|---|
| 1 | Preparar o PC | PowerShell | `java -version` mostra 21 e `az version` responde |
| 2 | Organização e projeto | dev.azure.com | o projeto `Sprint 4 – Azure DevOps` abre |
| 3 | Convidar o professor | Azure DevOps | o professor aparece como **Basic** e como administrador do projeto |
| 4 | Agente self-hosted | Azure DevOps + PowerShell (administrador) | o agente aparece **Online** no pool `Default` |
| 5 | Service connection | Azure DevOps | a conexão `kura-azure` aparece na lista |
| 6 | YAML e pipeline | Git Bash + Azure DevOps | a pipeline existe e a primeira execução começou |
| 7 | Executar e conferir | Azure DevOps | os dois estágios verdes, testes e artefato visíveis, health check 200 |
| 8 | Disparo automático | Git Bash | um push novo na `main` inicia uma execução sozinho |

Depois da etapa 8, use o [checklist da entrega e o roteiro do vídeo](#checklist-da-entrega-e-roteiro-do-vídeo).

Antes de começar: o ambiente do Azure já precisa existir (Web App `kura-tutor-api` no grupo `rg-kura`, criados pelo
`azure/deploy.sh infra` do repositório DevOps-Cloud). Se mudou `PREFIX` ou `RG` lá, ajuste `webAppName` e `resourceGroup`
nas `variables` do YAML.

## Etapa 1: Preparar o PC

O agente usa as ferramentas instaladas nesta máquina. Em um PowerShell, confirme:

```powershell
java -version      # precisa mostrar 21 ou superior
az version         # Azure CLI instalado (a task AzureCLI@2 usa o az desta máquina)
git --version
```

- O `java` precisa estar no **PATH do sistema** (não só do seu usuário), para o agente enxergá-lo mesmo se rodar como serviço.
- A máquina precisa acessar `repo.maven.apache.org` (o `mvnw.cmd` baixa o Maven e as dependências de lá).
- Alterou PATH ou instalou algo depois de iniciar o agente? Pare e inicie o agente de novo: ele lê o ambiente ao subir.

## Etapa 2: Criar a organização e o projeto

O slide 2/8 da entrega fixa a configuração do projeto; copie os valores exatamente:

1. Entre em `dev.azure.com` com sua conta Microsoft e crie (ou abra) uma organização.
2. *New project* e preencha:

   | Campo | Valor |
   |---|---|
   | Project name | `Sprint 4 – Azure DevOps` (com o travessão do slide; se o Azure DevOps recusar o caractere, use o hífen comum e avise o professor) |
   | Description | `Projeto para entrega da Sprint 4 do professor <nome do professor>`, e abaixo uma linha por integrante: `<RM> - <nome do aluno> - <turma 2TDS..>` |
   | Visibility | **Private** |
   | Version control (em *Advanced*) | **Git** |
   | Work item process (em *Advanced*) | **Scrum** ou **Agile** |

3. O plano gratuito inclui 1 job paralelo em agente **self-hosted**, suficiente aqui (só existe esta pipeline).

O código da aplicação continua no GitHub (o PDF da entrega pede o link do repositório do GitHub). O Azure Repos que o projeto
cria por causa do *Version control: Git* pode ficar vazio.

## Etapa 3: Convidar o professor

Sem acesso do professor ao projeto a avaliação é zerada (slide 6/8). O slide 3/8 pede o nível **Basic**, e o print do slide
8/8 mostra o professor também como administrador do projeto. Faça os dois:

1. *Organization settings* (canto inferior esquerdo) → *Users* → *Add users*.
2. Informe o **e-mail institucional do professor**, *Access level* **Basic**, e em *Add to projects* escolha
   `Sprint 4 – Azure DevOps`.
3. *Project settings* → *Overview* → *Project administrators* → *Add administrator* → selecione o professor.
4. Confira como no print do slide 8/8: em *Users* o professor aparece com acesso **Basic**, e em *Overview* ele aparece
   em *Project administrators*.
5. Avise o professor: o convite chega por e-mail e ele precisa aceitar para acessar.

## Etapa 4: Instalar o agente self-hosted

1. **Criar o PAT** (usado só para registrar o agente): ícone de usuário → *Personal access tokens* → *New Token* →
   *Custom defined* → escopo **Agent Pools: Read & manage** → validade curta. Copie o valor (ele só aparece uma vez).
2. **Baixar o agente**: *Project settings* → *Agent pools* → **Default** → aba *Agents* → *New agent* → **Windows** →
   *Download*.
3. **Instalar**: abra o PowerShell **como administrador** (obrigatório se for rodar como serviço) e extraia o zip em um
   caminho curto:
   ```powershell
   mkdir C:\agent; cd C:\agent
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   [System.IO.Compression.ZipFile]::ExtractToDirectory("$HOME\Downloads\vsts-agent-win-x64-<versão>.zip", "$PWD")
   .\config.cmd
   ```
4. **Responder ao assistente** do `config.cmd`:

   | Pergunta | Resposta |
   |---|---|
   | Server URL | `https://dev.azure.com/<sua-organização>` |
   | Authentication type | Enter (PAT) |
   | Personal access token | o PAT do passo 1 |
   | Agent pool | Enter (`Default`) |
   | Agent name | um nome livre, por exemplo `kura-pc` |
   | Work folder | Enter (`_work`) |
   | Run agent as service? (Y/N) | **N** na primeira vez (modo interativo, mais fácil de depurar) |

5. **Iniciar** e deixar a janela aberta:
   ```powershell
   .\run.cmd
   ```
   A janela deve mostrar `Listening for Jobs`.
6. **Conferir**: *Agent pools* → *Default* → *Agents*: o agente deve estar **Online**; em *Capabilities* aparece
   `Agent.OS = Windows_NT`.
7. Revogue o PAT (*Personal access tokens* → *Revoke*): depois do registro ele não é mais necessário.

Interativo ou serviço: no modo interativo o agente só roda enquanto a janela estiver aberta e você logado. Para deixá-lo
sempre ativo, rode `.\config.cmd remove` e configure de novo respondendo **Y** a *Run agent as service*. Nesse modo ele
roda como `NT AUTHORITY\NETWORK SERVICE` e enxerga só variáveis e PATH do **sistema**: confira a Etapa 1 antes.

## Etapa 5: Criar a service connection

1. *Project settings* → *Service connections* → *New service connection* → **Azure Resource Manager** → *Next*.
2. Método **Workload identity federation (automatic)** (não guarda senha nem segredo).
3. Escopo **Resource group**: escolha a subscription e o grupo `rg-kura`.
4. Nome: **`kura-azure`** (o mesmo de `serviceConnection` no YAML). Pode marcar *Grant access permission to all pipelines*
   ou autorizar na primeira execução ([Etapa 7](#etapa-7-executar-e-conferir)).
5. *Save*. A conexão `kura-azure` deve aparecer na lista.

A criação automática precisa de permissão para registrar aplicativos no Entra ID e atribuir papéis no grupo; ela concede
ao aplicativo o papel *Contributor* no `rg-kura`, suficiente para o deploy.

## Etapa 6: Publicar o YAML e criar a pipeline

1. **Copiar os arquivos** para o repositório `backend-tutor-java` e enviar para a `main` (Git Bash):
   ```bash
   cd /caminho/para/backend-tutor-java
   cp /caminho/dos/arquivos/azure-pipelines.yml .
   cp /caminho/dos/arquivos/docs/AZURE-PIPELINE.md docs/
   git add azure-pipelines.yml docs/AZURE-PIPELINE.md
   git commit -m "ci: pipeline Azure DevOps (CI + CD) para o tutor-api"
   git push origin main
   ```
2. **Criar a pipeline**: *Pipelines* → *New pipeline* → **GitHub** → selecione `KURA-Clyvo/backend-tutor-java`.
   Na primeira vez, o Azure DevOps pede para instalar/autorizar o app **Azure Pipelines** no GitHub; em uma organização do
   GitHub isso pode exigir um *owner* da organização `KURA-Clyvo`.
3. Escolha **Existing Azure Pipelines YAML file** → branch `main` → caminho `/azure-pipelines.yml` → *Continue*.
4. Clique em **Run**. (Opcional: *⋮* → *Rename/move* para chamar a pipeline de `kura-tutor-api`.)

## Etapa 7: Executar e conferir

1. A execução pausa pedindo permissão (*This pipeline needs permission to access a resource*): clique em **View** →
   **Permit** para a service connection `kura-azure` e para o ambiente `kura-producao` (pode pedir duas vezes).
2. Acompanhe o estágio **Build**: *Verificar JDK 21*, *mvnw verify*, *Publicar resultados dos testes*, *Separar o
   artefato*, *Publicar artefato*. Depois o estágio **Deploy**: *Deploy no Web App* e *Verificar health check*.
3. **Checklist de sucesso:**
   - [ ] Build e Deploy verdes.
   - [ ] Aba **Tests** da execução com os testes listados.
   - [ ] Link de artefatos (*1 published*) com `tutor-jar` contendo `app.jar`.
   - [ ] No log de *Verificar health check*, uma linha `HTTP 200`.
   - [ ] Conferência manual: `az webapp show -g rg-kura -n kura-tutor-api --query defaultHostName -o tsv` e abrir
     `https://<host>/api/actuator/health` no navegador (resposta com `"status":"UP"`).

## Etapa 8: Confirmar o disparo automático

Faça qualquer commit na `main` (por exemplo, um ajuste de documentação) e envie. Em até um minuto a pipeline deve aparecer
em *Pipelines → Runs* com o motivo *Continuous integration* e o commit novo. Se não aparecer, veja
[Solução de problemas](#solução-de-problemas).

## Checklist da entrega e roteiro do vídeo

Baseado nos slides 1/8 a 8/8 da entrega da Sprint 4 (DevOps Tools & Cloud Computing). O cronograma do PDF marca a entrega em
**04/11/2026**; confira o horário no portal.

### Antes de gravar (zero de nota se faltar)

- [ ] Projeto criado exatamente como na [Etapa 2](#etapa-2-criar-a-organização-e-o-projeto) e professor convidado
  ([Etapa 3](#etapa-3-convidar-o-professor)).
- [ ] Pipeline de CI e de CD funcionando: a última execução está verde e o agente está **online** (se o professor rodar a
  pipeline com o agente desligado, ela trava).
- [ ] A aplicação roda no Web App, não em localhost.
- [ ] CI e CD disparam sozinhos quando o **código** muda na `main`. Alterar só o README não vale (-20).
- [ ] PDF só com: nome completo, RM e turma de cada integrante, link do GitHub, link do vídeo no YouTube e link do projeto no
  Azure DevOps. O resto vai no README.

### Roteiro do vídeo (slides 4/8 e 5/8)

Qualidade mínima 720p, áudio claro, sem legendas e com explicação por voz. Deixe tudo aberto antes de gravar: IDE, Azure DevOps,
Pipelines, Portal do Azure e o terminal para o banco.

1. **Ferramentas:** mostre a IDE, o Azure DevOps, o Portal do Azure e o terminal do banco.
2. **Pipeline:** abra o `azure-pipelines.yml` e explique cada job e task: *Verificar JDK 21*, *mvnw verify*, *Publicar
   resultados dos testes*, *Separar o artefato*, *Publicar artefato*, e no CD *Deploy no Web App* e *Verificar health check*.
3. **Antes da alteração:** abra o Swagger do app no Web App (`https://<host>/api/swagger-ui.html`; o host sai de
   `az webapp show -g rg-kura -n kura-tutor-api --query defaultHostName -o tsv`).
4. **Alteração no código:** em `src/main/java/br/com/clyvo/kura/tutor/shared/config/OpenApiConfig.java`, troque o texto de
   `.title("KURA — Backend Tutor API")` por algo visível, por exemplo `"KURA — Backend Tutor API (Sprint 4)"`.
5. **Commit e push na `main`** (mostre o commit).
6. **Início automático:** em *Pipelines → Runs*, mostre a execução nova disparada pelo commit (*Individual CI*).
7. **Explique cada etapa durante a execução**, abrindo o log de cada passo (não explicar custa -20).
8. **Artefato e testes:** mostre a aba **Tests** e o artefato `tutor-jar` (*1 published*).
9. **Portal do Azure:** mostre o grupo `rg-kura`, o Web App `kura-tutor-api` com o deploy recente e as *Environment variables*
   com referências do Key Vault.
10. **Aplicação com a alteração:** recarregue o Swagger e mostre o novo título.
11. **CRUD completo com SELECT depois de cada operação** (-30 se faltar evidência): faça a operação pela aplicação (REST pelo
    Swagger ou Postman) e comprove no banco com o `scripts/db-select.sh` do repositório DevOps-Cloud (a senha nunca aparece
    na tela). Exemplo, ajustando as tabelas para as do seu CRUD (`bash scripts/db-select.sh --tabelas` e
    `--colunas <TABELA>` ajudam):
    ```bash
    bash scripts/db-select.sh --tabelas                      # tabelas do schema
    bash scripts/db-select.sh --colunas PET                  # colunas da tabela
    bash scripts/db-select.sh "select * from tutor"          # depois do INSERT, do UPDATE e do DELETE
    bash scripts/db-select.sh "select * from pet"            # tabela relacionada
    ```
    Cada consulta leva alguns segundos. Deixe as consultas prontas (ou use `-f arquivo.sql`) para não perder tempo digitando.

## Testes no Azure DevOps

- O comando é o mesmo do GitHub Actions (`-Dspring.profiles.active=dev`): H2 em memória, sem Oracle, Mongo ou Docker.
  Para reproduzir na sua máquina: `mvnw.cmd -Dspring.profiles.active=dev verify`.
- `PublishTestResults@2` lê `target/surefire-reports/TEST-*.xml` e roda **mesmo quando um teste falha**, então a aba
  **Tests** mostra o que quebrou, a duração e o histórico/tendência entre execuções.
- Falhou teste: o estágio **Build** fica vermelho e o **Deploy** não começa (o código não chega ao Web App).
- Cobertura (opcional): o `pom.xml` ainda não tem JaCoCo. Para exibir cobertura na execução, adicione o plugin
  `org.jacoco:jacoco-maven-plugin` (goals `prepare-agent` e `report`, fase `verify`; confira a versão atual) e um passo
  `PublishCodeCoverageResults@2` com `summaryFileLocation: '$(System.DefaultWorkingDirectory)/target/site/jacoco/jacoco.xml'`
  depois do `PublishTestResults@2`.

## Variáveis e segredos

A pipeline **não usa nenhum segredo**:

| Item | Como é protegido |
|---|---|
| Acesso ao Azure | Service connection por federação de identidade: não existe senha nem token guardado |
| Segredos da aplicação (banco, JWT, Mongo…) | Ficam no Key Vault e chegam ao Web App por *Key Vault references* (configurado pelo `deploy.sh`); a pipeline nunca os lê |
| PAT do agente | Só na configuração do agente; validade curta e revogado depois (Etapa 4) |

As variáveis do YAML (`serviceConnection`, `webAppName`, `resourceGroup`, `artifactName`) são nomes, não credenciais.
No vídeo, mostre a regra IV na prática: no portal, o Web App → *Environment variables* exibe referências `@Microsoft.KeyVault(...)` no
lugar de senhas, e a service connection não guarda segredo.
Se um dia a pipeline precisar de um segredo: *Pipelines* → *Library* → *Variable group* → ligue ao Key Vault
(*Link secrets from an Azure key vault*, com a service connection e o papel *Key Vault Secrets User* no cofre) ou crie
uma variável com o cadeado (*Keep this value secret*). Variáveis secretas não chegam aos scripts automaticamente:
mapeie-as em `env:` do passo.

## Operação do dia a dia

| Quero… | Como |
|---|---|
| Rodar sem push | *Pipelines* → a pipeline → **Run pipeline** (branch `main`) |
| Ver testes e artefato de uma execução | Abra a execução → aba **Tests** / link de artefatos |
| Exigir aprovação antes do deploy | *Pipelines* → *Environments* → `kura-producao` → *Approvals and checks* → *Approvals* |
| Voltar para uma versão anterior | Abra uma execução antiga que passou → estágio **Deploy** → *Rerun stage* (usa o artefato daquela execução, enquanto a retenção, padrão de 30 dias, não o apagar). Ou reverta o commit na `main` |
| Pausar o CD | *Edit* → *⋮* → *Settings* → *Disabled*, ou comente o `trigger` |
| Mudar nome do Web App ou da service connection | Edite as `variables` no topo do YAML |
| Agente offline | Rode `.\run.cmd` em `C:\agent` (ou inicie o serviço em `services.msc`) e confira em *Agent pools* |
| Ver o log do app após o deploy | `az webapp log tail -g rg-kura -n kura-tutor-api` |

Atenção: não há ambiente de staging. Cada push na `main` com testes verdes é implantado e o Flyway aplica as
migrations no Oracle de produção ao subir: migrations precisam ser compatíveis com a versão anterior do app.

## Convivência com o deploy.sh e o GitHub Actions

- O **`azure/deploy.sh`** (repositório DevOps-Cloud) continua dono da infraestrutura e dos *app settings* (Key Vault
  references, URLs, CORS). A pipeline só envia o código. Se rodar `bash azure/deploy.sh apps`, ele reimplanta as 3 APIs
  a partir do build local e pode sobrescrever a versão que a pipeline publicou: use-o só para mudar configuração ou refazer
  o ambiente.
- O pin do `java-backend` no DevOps-Cloud passa a ficar atrasado em relação ao `main` do Java depois dos deploys da
  pipeline; o job *Check submodule pins* vai avisar. Atualize o pin quando quiser registrar a versão de referência.
- O **GitHub Actions** (`.github/workflows/ci.yml`) continua validando PRs e o build da imagem Docker. O `pr: none`
  evita que PRs rodem no agente da sua máquina.

## Solução de problemas

| Sintoma | O que fazer |
|---|---|
| Execução parada em *Queued* / "No agent found in pool" | Agente offline ou sem `Agent.OS = Windows_NT`: inicie `.\run.cmd` e confira as *Capabilities* |
| Pipeline não dispara no push | Confira se o app Azure Pipelines está instalado no repositório do GitHub e se o push foi na `main`; em *Pipelines* → *Edit* → *Triggers* veja se o CI aparece habilitado |
| "This pipeline needs permission to access a resource" | *View* → *Permit* (service connection, ambiente) |
| `JDK 21 ou superior necessario no agente` | Instale o JDK 21, coloque no PATH do sistema e reinicie o agente |
| `mvnw.cmd` falha ao baixar o Maven | A máquina precisa de acesso a `repo.maven.apache.org` (antivírus/proxy podem bloquear) |
| `az` não reconhecido no *Verificar health check* | Azure CLI ausente ou fora do PATH do sistema: instale para todos os usuários e reinicie o agente |
| Service connection não consegue ser criada | Falta permissão no Entra ID/Azure; peça a um *Owner* ou crie com um service principal manual |
| `AzureWebApp@1` falha com 401/403 | Papel *Contributor* no `rg-kura` ausente para o aplicativo da service connection |
| Deploy passa mas o health check dá 503 | O app não subiu: `az webapp log tail -g rg-kura -n kura-tutor-api` (Oracle fora, Key Vault reference sem resolver, migration falhando) |
| Jar implantado mas não executa | O artefato precisa se chamar `app.jar` (o YAML já faz isso); confira o log do passo *Separar o artefato* |
| Caminhos muito longos no build | Instale o agente em um caminho curto (`C:\agent`) |

## Plano B se o deploy pela task falhar

Se o `AzureWebApp@1` não funcionar com o jar neste Web App, troque o passo *Deploy no Web App* pelo mesmo comando que o
`deploy.sh` usa (já validado neste ambiente):

```yaml
- task: AzureCLI@2
  displayName: 'Deploy no Web App (az webapp deploy)'
  inputs:
    azureSubscription: '$(serviceConnection)'
    scriptType: 'ps'
    scriptLocation: 'inlineScript'
    inlineScript: |
      az webapp deploy -g "$(resourceGroup)" -n "$(webAppName)" --src-path "$(Pipeline.Workspace)/$(artifactName)/app.jar" --type jar --restart true
```
