# Advanced Inscriber Backport

Port independente e focado do **Advanced Inscriber** do AE2 Things 1.0.7 para Minecraft Forge 1.16.5. Este projeto contém somente essa máquina: não inclui DISKs, Crystal Growth Chamber ou qualquer outro conteúdo do AE2 Things.

## Compatibilidade exata

| Componente | Versão usada para compilar |
|---|---:|
| Minecraft | 1.16.5 |
| Forge | 36.2.34 |
| ForgeGradle | 5.1.77 |
| Mappings MCP | 20210309-1.16.5 |
| Applied Energistics 2 | 8.4.7 |
| Java | 8 (bytecode 52) |

O arquivo `mods.toml` aceita Forge 36.2.34 ou mais recente dentro da linha 1.16.5 e exige AE2 de 8.4.7 até, mas sem incluir, 9.0.0. A configuração validada é Forge 36.2.34 + AE2 8.4.7.

## Instalação

1. Use um perfil Minecraft 1.16.5 com Forge 36.2.34.
2. Instale `appliedenergistics2-8.4.7.jar`.
3. Coloque `AdvancedInscriber-1.16.5-1.0.0.jar` na mesma pasta `mods`.
4. Não coloque o JAR original do AE2 Things 1.18.2 nesse perfil.

## Receita

```text
F H F
P I P
F H F
```

- `F`: lingote de ferro
- `H`: funil
- `P`: processador de engenharia do AE2
- `I`: Inscriber normal do AE2

## Funcionamento portado

- Executa as receitas registradas do Inscriber do AE2 8.4.7, inclusive a fabricação das peças impressas e dos processadores.
- Aceita até 64 itens em cada entrada e na saída.
- Aceita até cinco Speed Cards do AE2.
- Usa o mesmo fator do AE2 Things 1.0.7: `1 + 3 × número de Speed Cards`, chegando a 16×.
- Consome `20 × fator` AE por atualização de processamento, como no Advanced Inscriber original.
- Conecta energia e rede ME por qualquer face.
- Expõe um inventário Forge por qualquer face para funis, tubos e outros transportes; a inserção é filtrada pelas receitas e somente a saída permite extração.
- Tenta inserir diretamente o resultado no armazenamento ME conectado. Se a rede não aceitar o item, ele permanece na saída para coleta externa.
- Mantém inventário, upgrades e progresso no NBT ao salvar o mundo.
- Reutiliza a aparência, a animação e a disposição da interface do Advanced Inscriber original.

Para autocrafting, trate a máquina como um processador externo: uma ME Interface envia os ingredientes por um transporte de itens para o bloco e o resultado retorna diretamente à rede ME ou pode ser extraído da saída. A máquina não altera receitas nem classes do AE2.

## Compilação

Use um JDK 8 disponível para a toolchain e execute:

```bash
./gradlew build
```

O JAR reobfuscado é criado em `build/libs/AdvancedInscriber-1.16.5-1.0.0.jar`.

## Escopo e licença

O port foi implementado contra a API e o código do AE2 8.4.7, sem usar nomes ou APIs de 1.18.2 em tempo de execução. O código deste projeto é distribuído sob LGPL-3.0-or-later. O design e as quatro texturas reutilizadas do AE2 Things permanecem cobertos pela licença MIT e pelos créditos registrados em `THIRD_PARTY_NOTICES.md` e `THIRD_PARTY_LICENSES/AE2THINGS-MIT.txt`.

Veja `PORT_REPORT.md` para a análise técnica e `TEST_REPORT.md` para o que foi — e o que não foi — executado neste ambiente.
