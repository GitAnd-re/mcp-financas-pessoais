## 🚀 MCP de Finanças Pessoais 🚀

Projeto final de Serviços Web: servidor e cliente MCP implementados em Java com Apache Maven. O servidor usa transporte STDIO e oferece ferramentas e recursos para controle de finanças pessoais, além de prompts que orientam análises financeiras.

Os esquemas JSON das ferramentas ficam em `src/main/resources/json-schemas` e são incluídos no JAR do servidor.

### 🧰 Tools 🧰
**Registrar gasto**: registra e salva um gasto em `lancamentos.json`. A resposta confirma os dados do gasto; o identificador continua salvo no arquivo. Argumentos:
* **valor** (`number`, obrigatório): valor positivo do gasto.
* **categoria** (`string`, obrigatório): categoria do gasto; não pode ser vazia ou conter apenas espaços.
* **data** (`string`, obrigatório): data válida no formato `DD/MM/AAAA`.
* **descricao** (`string`, opcional): descrição do gasto; se omitida, fica vazia.

Resposta de sucesso (texto):

```text
tipo:GASTO, valor:42.75, categoria:Transporte, data:20/11/2026, descricao:Ônibus
```

Erros possíveis:

* Valor ausente ou que não seja número: `O valor deve ser informado como número.`
* Valor inválido: `O valor informado é inválido.`
* Valor menor ou igual a zero: `O valor deve ser maior que zero.`
* Categoria ausente: `O campo categoria é obrigatório.`
* Categoria que não seja texto: `O campo categoria deve ser texto.`
* Categoria vazia ou só com espaços: `O campo categoria não pode ser vazio.`
* Data ausente: `O campo data é obrigatório.`
* Data que não seja texto: `O campo data deve ser texto.`
* Data vazia: `O campo data não pode ser vazio.`
* Data inválida ou fora do formato `DD/MM/AAAA`: `A data informada é inválida. Use uma data válida no formato DD/MM/AAAA.`
* Descrição que não seja texto: `O campo descrição deve ser texto.`
* Falha ao salvar: `Não foi possível salvar o gasto: <mensagem da falha>.`

**Registrar receita**: registra e salva uma receita em `lancamentos.json`. A resposta confirma os dados da receita; o identificador continua salvo no arquivo. Argumentos:
* **valor** (`number`, obrigatório): valor positivo da receita.
* **fonte** (`string`, obrigatório): origem da receita; não pode ser vazia ou conter apenas espaços.
* **data** (`string`, obrigatório): data válida no formato `DD/MM/AAAA`.

Resposta de sucesso (texto):

```text
tipo:RECEITA, valor:2500.00, fonte:Salário, data:20/11/2026
```

Erros possíveis:

* Valor ausente ou que não seja número: `O valor deve ser informado como número.`
* Valor inválido: `O valor informado é inválido.`
* Valor menor ou igual a zero: `O valor deve ser maior que zero.`
* Fonte ausente: `O campo fonte é obrigatório.`
* Fonte que não seja texto: `O campo fonte deve ser texto.`
* Fonte vazia ou só com espaços: `O campo fonte não pode ser vazio.`
* Data ausente: `O campo data é obrigatório.`
* Data que não seja texto: `O campo data deve ser texto.`
* Data vazia: `O campo data não pode ser vazio.`
* Data inválida ou fora do formato `DD/MM/AAAA`: `A data informada é inválida. Use uma data válida no formato DD/MM/AAAA.`
* Falha ao salvar: `Não foi possível salvar a receita: <mensagem da falha>.`

**Listar lançamentos**: lista os lançamentos de um mês específico ou de um ano inteiro, opcionalmente filtrando pela categoria. Argumentos:
* **mes** (`integer`, opcional): mês dos lançamentos, de 1 a 12. Se omitido, lista todos os meses do ano informado.
* **ano** (`integer`, obrigatório): ano dos lançamentos, maior que zero.
* **categoria** (`string`, opcional): mostra somente os gastos da categoria informada. Sem este parâmetro, lista gastos e receitas.

Resposta de sucesso (texto):

```text
Lançamentos de 2040:
tipo:GASTO, valor:30.00, categoria:Curso, data:20/04/2040, descricao:
```

Quando não houver resultados, a mensagem indica o período consultado, por exemplo: `Nenhum lançamento encontrado para 04/2040.` ou `Nenhum lançamento encontrado para 2040.`

Erros possíveis:

* Mês informado que não seja inteiro: `O campo mês deve ser inteiro.`
* Mês menor que 1 (na coleta pelo cliente): `Valor inválido: O parâmetro 'mes' deve ser maior ou igual a 1.`
* Mês maior que 12 (na coleta pelo cliente): `Valor inválido: O parâmetro 'mes' deve ser menor ou igual a 12.`
* Ano ausente: `O campo ano é obrigatório.`
* Ano que não seja inteiro: `O campo ano deve ser inteiro.`
* Ano menor que 1: `O ano deve ser maior que zero.`
* Categoria que não seja texto: `O campo categoria deve ser texto.`
* Categoria vazia: `O campo categoria não pode ser vazio.`
* Categoria composta só por espaços: `A categoria não pode ser vazia.`
* Falha ao ler os lançamentos: `Não foi possível listar os lançamentos: <mensagem da falha>.`

**Resumo financeiro**: calcula o total de receitas, o total de gastos, o saldo (receitas menos gastos) e os gastos agrupados por categoria. Com mês, resume aquele mês; sem mês, resume o ano informado. Argumentos:
* **mes** (`integer`, opcional): mês do resumo, de 1 a 12. Se omitido, considera o ano inteiro.
* **ano** (`integer`, obrigatório): ano do resumo, maior que zero.

Resposta de sucesso (texto):

```text
Resumo financeiro de 04/2040:
total_receitas:2500.00
total_gastos:130.00
saldo:2370.00

gastos_por_categoria:
Curso:30.00
Mercado:100.00
```

Quando não houver gastos no período, a resposta inclui `Nenhum gasto no período.` Os totais sem lançamentos são `0`.

Erros possíveis:

* Mês informado que não seja inteiro: `O campo mês deve ser informado como número inteiro.`
* Mês menor que 1 (na coleta pelo cliente): `Valor inválido: O parâmetro 'mes' deve ser maior ou igual a 1.`
* Mês maior que 12 (na coleta pelo cliente): `Valor inválido: O parâmetro 'mes' deve ser menor ou igual a 12.`
* Ano ausente ou que não seja inteiro: `O campo ano deve ser informado como número inteiro.`
* Ano menor que 1: `O ano deve ser maior que zero.`
* Falha ao ler os lançamentos: `Não foi possível gerar o resumo financeiro: <mensagem da falha>.`

**Definir orçamento**: salva ou atualiza o limite mensal de gastos de uma categoria em `orcamentos.json`. Argumentos:
* **categoria** (`string`, obrigatório): categoria a orçar; não pode ser vazia ou conter apenas espaços.
* **mes** (`integer`, obrigatório): mês ao qual o orçamento se aplica, de 1 a 12.
* **ano** (`integer`, obrigatório): ano ao qual o orçamento se aplica, maior que zero.
* **limite_mensal** (`number`, obrigatório): limite positivo de gastos mensais.

Resposta de sucesso (texto):

```text
categoria:Alimentação, mes:11, ano:2026, limite_mensal:500.00
```

Erros possíveis:

* Categoria ausente: `O campo categoria é obrigatório.`
* Categoria que não seja texto: `O campo categoria deve ser texto.`
* Categoria vazia ou só com espaços: `A categoria não pode ser vazia.`
* Categoria composta apenas por número: `A categoria não pode ser um número.`
* Mês menor que 1 ou maior que 12 (rejeitado pela validação do schema MCP).
* Ano menor que 1 (rejeitado pela validação do schema MCP).
* Limite mensal ausente: `O campo limite_mensal é obrigatório.`
* Limite mensal que não seja número: `O limite mensal deve ser informado como número.`
* Limite mensal inválido: `O limite mensal informado é inválido.`
* Limite mensal menor ou igual a zero (rejeitado pela validação do schema MCP): `Validation failed: JSON schema validation errors: [/limite_mensal: deve ter um valor mínimo exclusivo de 0]`
* Falha ao salvar: `Não foi possível salvar o orçamento: <mensagem da falha>.`

**Verificar orçamento**: compara os gastos de cada categoria com os limites salvos para o mês e ano informados. Considera apenas orçamentos definidos para esse período. O restante é calculado como limite menos gasto; quando negativo, o orçamento foi ultrapassado. Argumentos:
* **mes** (`integer`, obrigatório): mês da verificação, de 1 a 12.
* **ano** (`integer`, obrigatório): ano da verificação, maior que zero.

Resposta de sucesso (texto):

```text
Orçamentos de 11/2026:
Lazer: gasto:0.00, limite:200.00, restante:200.00, estourou:false
Mercado: gasto:125.50, limite:100.00, restante:-25.50, estourou:true
```

Erros possíveis:

* Mês ou ano ausente: `O campo mês/ano é obrigatório.`
* Mês que não seja inteiro: `O campo mês deve ser informado como número inteiro.`
* Mês menor que 1 ou maior que 12 (na validação do schema MCP): mensagem de validação do schema indicando o limite permitido.
* Ano que não seja inteiro: `O campo ano deve ser informado como número inteiro.`
* Ano menor que 1: `O ano deve ser maior que zero.`
* Nenhum orçamento definido: `Nenhum orçamento foi definido.`
* Orçamento não definido para o mês e ano informados: `Nenhum orçamento foi definido para 11/2026.`
* Falha ao ler orçamentos ou lançamentos: `Não foi possível verificar os orçamentos: <mensagem da falha>.`

**Simular parcelamento**: calcula parcelas mensais iguais pelo sistema Price e compara o custo total com o pagamento à vista. Valores monetários são exibidos com duas casas decimais; total e juros são calculados antes do arredondamento da parcela. Argumentos:
* **valor** (`number`, obrigatório): valor positivo da compra.
* **parcelas** (`integer`, obrigatório): quantidade de parcelas, no mínimo 1.
* **taxa_juros_mensal** (`number`, obrigatório): percentual de juros mensal, não negativo. Por exemplo, `2.5` representa 2,5% ao mês; zero indica parcelamento sem juros.

Resposta de sucesso (texto):

```text
Simulação de parcelamento:
parcelas:12
valor_parcela:94.56
total_pago:1134.72
juros_pagos:134.72
pagamento_a_vista:1000.00
economia_pagando_a_vista:134.72
```

Erros possíveis:

* Valor ausente: `O campo valor é obrigatório.`
* Valor que não seja número: `O campo valor deve ser informado como número.`
* Valor inválido: `O campo valor informado é inválido.`
* Valor menor ou igual a zero: `Validation failed: JSON schema validation errors: [/valor: deve ter um valor mínimo exclusivo de 0]`
* Parcelas ausentes: `O campo parcelas é obrigatório.`
* Parcelas que não sejam inteiras: `O campo parcelas deve ser informado como número inteiro.`
* Parcelas menores que 1: `Validation failed: JSON schema validation errors: [/parcelas: deve ter um valor mínimo de 1]`
* Taxa ausente: `O campo taxa de juros mensal é obrigatório.`
* Taxa que não seja número: `O campo taxa de juros mensal deve ser informado como número.`
* Taxa inválida: `O campo taxa de juros mensal informado é inválido.`
* Taxa negativa: `Validation failed: JSON schema validation errors: [/taxa_juros_mensal: deve ter um valor mínimo de 0]`
* Falha no cálculo: `Não foi possível simular o parcelamento: <mensagem da falha>.`

### 🪄 Prompts 🪄
**Analisar gastos** (`analisar_gastos`): orienta a análise dos gastos de um mês ou ano usando as ferramentas `resumo_financeiro` e `listar_lancamentos`. Argumentos:
* **ano**: ano a analisar, inteiro maior que zero (obrigatório).
* **mes**: mês de 1 a 12; se omitido ou vazio, analisa o ano todo (opcional).
* **contexto**: pergunta ou foco adicional da análise (opcional).

**Analisar lançamentos** (`analisar_lancamentos`): orienta a análise dos lançamentos individuais de um mês ou ano usando a ferramenta `listar_lancamentos`. Argumentos:
* **ano**: ano a consultar, inteiro maior que zero (obrigatório).
* **mes**: mês de 1 a 12; se omitido ou vazio, consulta o ano todo (opcional).
* **categoria**: categoria usada para filtrar os lançamentos (opcional).
* **contexto**: pergunta ou foco adicional da análise (opcional).

### 📚 Resources 📚
**Categorias de gastos** (`financas://categorias`): lista em ordem alfabética as categorias usadas em gastos ou configuradas em orçamentos, sem duplicatas. Retorna `Nenhuma categoria encontrada.` quando ainda não houver categorias.

**Resumo financeiro do mês atual** (`financas://resumo-mensal-atual`): retorna receitas, gastos, saldo e gastos por categoria do mês corrente. O conteúdo é atualizado quando o resource é lido. Se não houver dados, os totais aparecem como `0.00` e a lista informa `Nenhum gasto no período.`.

---

### 📌 Executar a aplicação 📌
Este projeto entrega duas aplicações: servidor e cliente. O servidor é executado a partir da classe `br.edu.ifpr.mcp_financas_pessoais.main.McpServer01SyncStdio`; o cliente, pela classe `br.edu.ifpr.mcp_financas_pessoais.main.McpClient01SyncStdio`.

Inicialmente, é preciso gerar a aplicação servidor e, em seguida, executar o cliente, tanto no [Eclipse IDE](https://www.eclipse.org/) quanto em linha de comando (CLI - *Command Line Interface*).

Além disto, é possível executar o servidor MCP Java no assistente de IA da Anthropic, o [Claude Desktop](https://claude.com/product/overview).

Também é possível verificar e testar o servidor MCP Java com o [MCP Inspector](https://github.com/modelcontextprotocol/inspector), ferramenta interativa para testar e depurar servidores MCP. Mais adiante, este documento mostra os passos necessários para executar o MCP Inspector em um contêiner Docker no Docker Desktop, juntamente com os arquivos `Dockerfile` e `compose.yaml`, disponíveis na raiz do projeto.

#### ⚙️ Eclipse IDE ⚙️
1. Clicar com o botão direito do mouse no nome do projeto;
2. Selecionar **Run As** > **Maven build...**
3. Na janela **Edit configuration and launch**:
    * Na aba **Main** digitar **clean package -Pserver -DskipTests** no campo **Goals**
    * Na aba **JRE** digitar **--enable-native-access=ALL-UNNAMED** no campo **VM arguments**
4. Clicar no botão **Run**

Os passos anteriores devem ser executados somente na primeira vez. A partir da segunda vez clicar na "pontinha de flecha" ao lado do botão *play*, na barra de botões do Eclipse IDE.

Se o processo de construção da aplicação servidor ocorreu sem erros, ele produzirá o diretório (pasta) **server** na raiz do projeto. Este diretório contém a aplicação executável do servidor e o sub-diretório **libs** contendo as bibliotecas de código necessárias ao servidor.

**A aplicação servidor é executada pela aplicação cliente e não precisa de intervenção do programador.**

Para gerar a aplicação cliente, deve-se executar os quatro passos mostrados anteriormente, com a seguinte diferença, na aba **Main** digitar **clean package -Pclient -DskipTests**, e este procedimento, assim como o anterior, deve ser realizado apenas na primeira vez.

Se o processo de construção da aplicação cliente ocorreu sem erros, ele produzirá o diretório **dist-client** como subdiretório de **target**. O diretório **dist-client** contém a aplicação cliente e o subdiretório **libs**, com as bibliotecas necessárias à sua execução.

#### ⚙️ Linha de Comando (CLI) ⚙️
Abrir uma janela do terminal, Prompt de Comando ou PowerShell no Windows, por exemplo, mudar para o diretório raiz do projeto e gerar as aplicações:

```
# gerar a aplicação servidor
mvn clean package -Pserver -DskipTests
```
```
# gerar a aplicação cliente
mvn clean package -Pclient -DskipTests

# executar o cliente MCP Java, que executará o servidor MCP Java
# DIRETÓRIO_AUTORIZADO é passado como parâmetro ao cliente, que
# repassará ao servidor

chcp 65001 >nul
cd target\dist-client
java -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar mcp_financas_pessoais_client.jar DIRETÓRIO_AUTORIZADO
```

ou

```
# executar o cliente MCP Java, que executará o servidor MCP Java
# DIRETÓRIO_AUTORIZADO é disponibilizado por meio da variável
# de ambiente MCP_FILES_ROOT e será repassado ao servidor

chcp 65001 >nul
cd target\dist-client
set MCP_FILES_ROOT=DIRETÓRIO_AUTORIZADO
java -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar mcp_financas_pessoais_client.jar
set MCP_FILES_ROOT=
```

ou

```
# executar o cliente MCP Java, que executará o servidor MCP Java
# DIRETÓRIO_AUTORIZADO é disponibilizado por meio de um parâmetro
# à JVM (propriedade) e será repassado ao servidor

chcp 65001 >nul
cd target\dist-client
java -Dmcp.files.root=DIRETÓRIO_AUTORIZADO  -jar mcp_financas_pessoais_client.jar
```

Independentemente do modo de construção (*build*), pelo Eclipse IDE ou pela linha de comando, depois de gerar as duas aplicações, abra um terminal e entre no diretório **target/dist-client**. Execute um dos comandos acima e pressione ENTER <img width="20" height="20" src="https://img.icons8.com/color/48/enter-key.png" alt="enter-key"/>. O cliente iniciará o servidor e exibirá um menu para interagir com as ferramentas disponíveis.

O argumento **DIRETÓRIO_AUTORIZADO** indica a pasta local onde o servidor criará e manterá `lancamentos.json` e `orcamentos.json`. Se o argumento, a variável `MCP_FILES_ROOT` e a propriedade `mcp.files.root` não forem informados, os arquivos serão salvos em `.mcp-financas-pessoais` dentro do diretório pessoal do usuário.

A segunda e terceira opções são mostradas na sequência para configuração do Claude Desktop.

---

### ⚙️ MCP Inspector no [Docker Desktop](https://www.docker.com/products/docker-desktop/?at_exp=DO112.A) ⚙️
Certificar-se de que o Docker Desktop esteja em execução antes de proceder com os passos à seguir:.

1. Editar o arquivo `compose.yaml` e localizar a entrada `source` para inserir um caminho válido no sistema de arquivos

2. Abrir uma janela do terminal, Prompt de Comando ou PowerShell no Windows, por exemplo
3. Mudar para o diretório raiz do projeto
4. Executar o comando `docker compose up -d --build mcp-inspector` (veja a seguir)
5. Aguardar até que a imagem tenha sido criada e executada no Docker Desktop
6. Na barra de endereços do navegador, digitar `http://127.0.0.1:6274` e pressionar ENTER <img width="20" height="20" src="https://img.icons8.com/color/48/enter-key.png" alt="enter-key"/>

O passo **4** é uma composição, ou seja, o comando `compose` processa o arquivo `compose.yaml`, que, por sua vez, processa o arquivo `Dockerfile` para criar, se necessário, a imagem Docker. Após a criação da imagem Docker, o `compose` continua sua execução e implanta um conjunto de serviços, o MCP Inspector + o MCP Java Server, no Docker Desktop. Ambos os arquivos encontram-se no diretório raiz do projeto.

Se os passos anteriores ocorreram com sucesso, o MCP Inspector exibirá o servidor MCP Java, ferramenta(s), recurso(s) e prompt(s) para testes.

O arquivo `compose.yaml` foi preparado para a construção de duas imagens:
1. A imagem contendo o MCP Inspector com o MCP Java Server: adicionar a opção `--build mcp-inspector` na linha de comando
2. A imagem do MCP Java Server para ser usada com um assistente de IA. Neste caso, quem executará o `compose` é a aplicação de assistente de IA (ver abaixo).

O diretório onde o servidor grava `lancamentos.json` e `orcamentos.json` **deve** ser configurado no arquivo `.env`, na raiz do projeto.

---

### 📌 Instalar o servidor MCP Java Local no [Claude Desktop](https://claude.com/) 📌 ###
- Localizar o diretório `C:\Users\<USUÁRIO_LOGADO>\AppData\Local\Packages\Claude_pzs8sxrjxfjjc\LocalCache\Roaming\Claude` ou `C:\Users\<USUÁRIO_LOGADO>\AppData\Local\Claude-Data\claude_pzs8sxrjxfjjc`
- Editar o arquivo `claude_desktop_config.json`
- Se não existir, criar a entrada `mcpServers` com um dos conteúdos abaixo

```
  "mcpServers": {
    "mcp_financas_pessoais": {
      "command": "java",
      "args": [
        "-jar",
        "DRIVE:\\caminho\\absoluto\\para\\servidor.jar"
      ],
      "env": { "MCP_FILES_ROOT": "DIRETÓRIO_AUTORIZADO"}
    }
  }
```
ou

```
  "mcpServers": {
    "mcp_financas_pessoais": {
      "command": "java",
      "args": [
         "-Dmcp.files.root=DIRETÓRIO_AUTORIZADO",
         "-jar",
         "DRIVE:\\caminho\\absoluto\\para\\servidor.jar"
      ]
    }
  }
```

Também é possível configurar o Claude Desktop para iniciar o MCP Java Server pelo Docker. O Docker Desktop deve estar em execução. Gere primeiro o servidor com o Maven para que o arquivo `server/mcp_financas_pessoais_server.jar` exista. Substitua os caminhos abaixo pelos caminhos absolutos do seu projeto e do arquivo `.env`:

```
  "mcpServers": {
    "mcp_financas_pessoais": {
      "command": "docker",
      "args": [
        "compose",
        "--env-file",
        "DRIVE:\\caminho\\absoluto\\do\\projeto\\.env",
        "-f",
        "DRIVE:\\caminho\\absoluto\\do\\projeto\\compose.yaml",
        "run",
        "--build",
        "--rm",
        "-T",
        "--no-deps",
        "mcp-server"
      ]
    }
  }
```
- Salvar o arquivo e iniciar ou reiniciar o Claude Desktop (se o Claude Desktop estiver em execução deve ser completamente encerrado e não apenas sua janela fechada).

Na interface atual do Claude Desktop, as ferramentas ficam disponíveis no servidor conectado. Para localizar e usar os resources e prompts, clique no botão **+** ao lado da caixa de mensagem, selecione **Plugins** e depois **mcp-financas-pessoais**.

O diretório `Users` também pode ser escrito como `Usuários`.

`USUÁRIO_LOGADO` representa o nome do diretório do perfil da conta usada para entrar no Windows.
 
O diretório `Claude_pzs8sxrjxfjjc` pode possuir um nome diferente do apresentado, mas o prefixo `Claude_` será o mesmo.

`DRIVE:` é a indicação do *drive* no sistema de arquivos do Windows.

No Windows, o separador de diretórios é a barra invertida (`\`); em Linux, Unix e macOS, geralmente é a barra (`/`).

O primeiro exemplo é genérico e pode ser usado com servidores MCP de diferentes tecnologias, desde que consigam ler variáveis de ambiente.

O segundo exemplo é específico para a linguagem Java, pois usa parâmetros que são passados à Máquina Virtual Java (JVM - *Java Virtual Machine*) através de propriedades.

Para o primeiro e segundo exemplos, o Java (JDK - *Java Development Kit* ou JRE - *Java Runtime Environment*) **deve** estar instalado e adequadamente configurado.

O terceiro exemplo usará o `compose.yaml` do projeto para que o Claude Desktop inicie a execução do `compose`, que, por sua vez, criará uma imagem e a executará (tornando-se um contêiner) no Docker Desktop.

O diretório onde o servidor grava `lancamentos.json` e `orcamentos.json` **deve** ser configurado no arquivo `.env`, na raiz do projeto.

---

## 🗣️ **OBSERVAÇÃO IMPORTANTE** 🗣️
> Sempre que o artefato SERVIDOR é gerado, o artefato CLIENTE é apagado do diretório "target", devido ao objetivo (*goal*) `clean`, e deve ser gerado novamente (mesmo que não tenha havido mudança de código nele).

---

## 🕵️‍♂️ TESTES DE SOFTWARE 🕵️‍♂️
Algumas características e dados importantes acerca de testes de software.

* **Projeto orientado pela demanda**: ao invés de escrever o código e testá-lo depois, escreve-se um teste que requeira uma determinado comportamento. Isto conduz ao raciocínio sobre a interface, limites e perspectiva do consumidor (programador e **testers**, entre outros) antes que detalhes da implementação tornem-se caóticos;
* **Pequenos e seguros passos**: a carga cognitiva é minimizada ao dividir o problema em partes menores e gerenciáveis. Desta forma, pensa-se na implementação necessária ao teste e cria-se um mecanismo contínuo de **feedback**;
* **Documentação ativa**: o conjunto de testes atua como uma documentação atualizada de como a aplicação deve se comportar. Se um teste é difícil de ler ou escrever, provavelmente, há uma falha na implementação do código.

**Ciclo Red-Green-Refactor**

* **Red**: escrever um teste que falha (a funcionalidade ainda não existe), isto garante que o teste existe;
* **Green**: implementar minimamente a funcionalidade para que o teste passe sem se importar com a qualidade da implementação;
* **Refactor**: realizar refatorações e refinamentos sem quebrar os testes.-

O diretório `src/test/java` é o local onde se localizam as classes para testes, tanto de integração, classes com o sufixo **IT**, quanto unitários, classes com o sufixo **Test**.

#### 📌 Executar Testes Unitários 📌
Os testes unitários usam o sufixo `Test` e cobrem a lógica dos serviços, ferramentas, resources e prompts.

Seguir os passos acima para criar um *build*, se for a primeira execução. No campo `Goals` digitar `clean test`.

Para executar os testes unitários em linha de comando, digitar `mvn clean test`.

Para testar apenas uma das várias classes de testes disponíveis, o campo `Goals` deve informar `clean test -Dtest=NomeDaClasse`.


Para testar apenas um método de uma determinada classe, o campo `Goals` deve informar `clean test -Dtest=NomeDaClasse#NomeDoMétodo`.


#### 📌 Executar Testes de Integração (IT - *Integration Test*) 📌
Os testes de integração usam o sufixo `IT` e verificam ferramentas, resources e prompts pela comunicação real com o servidor MCP via STDIO.

Seguir os passos acima para criar um *build*, se for a primeira execução. No campo `Goals` digitar `clean verify -Pserver`.

Para executar os testes de integração em linha de comando, digitar `mvn clean verify -Pserver`.

Para testar apenas uma das várias classes de testes disponíveis, o campo `Goals` deve informar `clean verify -Pserver -Dit.test=NomeDaClasse`.


Para testar apenas um método de uma determinada classe, o campo `Goals` deve informar `clean verify -Pserver -Dit.test=NomeDaClasse#NomeDoMétodo`.

---

#### 📘 REFERÊNCIAS 📘 ####
[What is the Model Context Protocol (MCP)? - Documentação Oficial](https://modelcontextprotocol.io/docs/2026-07-28/getting-started/intro)

[MCP Java SDK 1](https://github.com/modelcontextprotocol/java-sdk)

[MCP Java SDK 2](https://java.sdk.modelcontextprotocol.io/latest/)

[Building a JDBC-based MCP Server: A Developer Guide](https://www.cdata.com/kb/articles/jdbc-mcp-server-dev-guide.rst)

[Learn MCP Java Tutorial - Microsoft](https://github.com/microsoft/lets-learn-mcp-java/tree/main)

[Tutorial 1 no GitHub](https://github.com/eugenp/tutorials/tree/master/java-mcp)

[Tutorial 2 no GitHub](https://github.com/CDataSoftware/cdata-jdbc-mcp-server)

---

[Test Driven Development (Martin Fowler)](https://martinfowler.com/bliki/TestDrivenDevelopment.html)

[GeeksforGeeks TDD Guide](https://www.geeksforgeeks.org/software-testing/the-importance-of-test-driven-development-tdd/)
