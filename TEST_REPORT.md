# Relatório de testes

Data: 2026-09-23 (UTC)

## Resultado

O projeto compila e gera um JAR Forge reobfuscado. Não foi possível concluir uma sessão jogável do Minecraft neste ambiente; portanto, este relatório **não afirma teste manual dentro do jogo**.

## Verificações concluídas

| Verificação | Resultado |
|---|---|
| Resolução de Forge 1.16.5-36.2.34 | Aprovada |
| Resolução de AE2 8.4.7 | Aprovada |
| `clean build` com ForgeGradle | Aprovada |
| Compilação Java | Aprovada, sem erros |
| Reobfuscação Forge (`reobfJar`) | Aprovada |
| Inicialização Forge/AE2 em `runData` | Aprovada |
| JSON de modelos, blockstate, tela, receita e loot | Aprovada |
| Conteúdo obrigatório dentro do JAR | Aprovada |
| Ausência de classes/recursos de DISKs e Crystal Growth Chamber | Aprovada |
| Bytecode Java 8 | Aprovada (class major 52) |
| Dependências declaradas no `mods.toml` | Forge ≥36.2.34, MC 1.16.5, AE2 ≥8.4.7 e <9.0.0 |

O JAR final usa bytecode Java 8 e inclui as licenças LGPL-3.0 e MIT. O checksum SHA-256 é registrado em `SHA256SUMS.txt`, entregue junto ao projeto.

Comando de compilação final:

```bash
../research/tooling/gradle-7.4/bin/gradle clean build --no-daemon
```

Resultado: `BUILD SUCCESSFUL`; tarefas `compileJava`, `processResources`, `jar`, `reobfJar`, `assemble`, `check` e `build` concluídas. Não há testes JUnit no projeto.

## Tentativa de inicialização

`runServer --args='--nogui'` foi tentado após baixar e validar os assets faltantes por SHA-1. O ModLauncher iniciou com Forge 36.2.34, Minecraft 1.16.5 e Java 8; o processo então parou na tela de aceite do EULA, como esperado. O EULA não foi aceito automaticamente, portanto não houve criação de mundo nem sessão de servidor completa.

Para validar o carregamento sem aceitar o EULA, foi executado `runData`. Na primeira passagem, o mixin do AE2 externo tentou usar o refmap de produção contra classes de desenvolvimento já deobfuscadas. As configurações de desenvolvimento passaram a usar `mixin.env.disableRefMap=true`, a mesma solução usada pelo próprio projeto AE2 em seus runs de desenvolvimento. A repetição carregou Forge 36.2.34, AE2 8.4.7 e `advancedinscriber`, inicializou os registros e terminou com `BUILD SUCCESSFUL`. Os avisos de configuração sem comentário emitidos pelo AE2 são não fatais e não pertencem a este mod.

Esse teste detecta falhas de classloading, mixins, dependências e registro comum. Ele não substitui a interação em um mundo.

O ambiente não possui servidor gráfico/X virtual, então o cliente não foi aberto até o menu principal.

## O que ainda deve ser validado em um modpack

1. Abrir a GUI e conferir os cinco slots de Speed Card.
2. Produzir cada circuito impresso e cada processador do AE2.
3. Verificar Name Press.
4. Alimentar entradas e extrair a saída com funil e com o transporte usado no modpack.
5. Conectar à rede ME, medir energia e confirmar inserção direta da saída.
6. Executar um padrão de processamento por ME Interface/autocrafting.
7. Salvar, fechar e reabrir o mundo durante um processo e com itens nos quatro slots.
8. Validar conflitos com os demais mods do modpack.

Esses testes precisam ser feitos numa instância real Forge 1.16.5 com AE2 8.4.7. O código preserva as interfaces do AE2/Forge correspondentes, mas compatibilidade com tubos específicos de terceiros depende da implementação desses mods.
