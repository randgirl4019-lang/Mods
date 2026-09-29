# Relatório do port

## Fontes analisadas

- JAR fornecido: `AE2-Things-1.0.7.jar`, SHA-256 `9dd028d7b2ff06825b0b645f4746a4098b6bafa689f2af0a9be31538d22c9fd3`.
- AE2 Things Forge, tag `1.0.7`, commit `5f9ee85a6bd50bfba2d759eb3c0770bdd7fb2aa4`.
- Applied Energistics 2, tag `v8.4.7`, commit `b45c590639a0ed0e6dd2cd60a3263b4546cd53c5`.
- Forge 1.16.5 `36.2.34`, ForgeGradle `5.1.77` e mappings MCP `20210309-1.16.5`.
- AE Additions na branch 1.16.5, como referência de addon externo compilado contra `appeng:appliedenergistics2:8.4.7`.

O conteúdo e os metadados do JAR fornecido foram comparados com a tag 1.0.7. O original declara Minecraft 1.18.2, Forge 40.1.60, AE2 11.6.4 e Java 17; por isso suas classes não foram copiadas para o resultado 1.16.5.

## Elementos identificados no original

Classes relevantes:

- `BlockAdvancedInscriber`
- `BEAdvancedInscriber`
- `AdvancedInscriberMenu`
- `AdvancedInscriberRootPanel`

Recursos relevantes:

- dois modelos de bloco (ocioso e ativo), modelo de item e blockstate `working`;
- `top.png`, `side_off.png`, `side_on.png` e metadados da animação;
- definição de tela `advanced_inscriber.json`;
- receita de fabricação e loot table.

Comportamento observado no original:

- quatro slots lógicos: placa superior, placa inferior, entrada central e saída;
- pilhas de até 64 itens;
- cinco slots de Speed Card;
- fator de velocidade `1 + 3 × cards` e consumo `20 × fator` AE;
- busca nas receitas de Inscriber do AE2, incluindo Name Press;
- energia, rede e inventário disponíveis em todas as faces;
- inserção externa validada contra receitas e extração limitada à saída;
- resultado enviado diretamente ao armazenamento ME quando possível.

## Adaptação 1.18.2 → 1.16.5

| Área | AE2 Things / AE2 11.6.4 | Port / AE2 8.4.7 |
|---|---|---|
| Bloco | `AEBaseEntityBlock` | `AEBaseTileBlock` |
| Entidade | `AENetworkPowerBlockEntity` | `AENetworkPowerTileEntity` |
| Inventário | `InternalInventory` | Forge `IItemHandler` + `AppEngInternalInventory` |
| Combinação/filtro | `CombinedInternalInventory` / `FilteredInternalInventory` | `WrapperChainedItemHandler` / `WrapperFilteredItemHandler` |
| Energia ME | `IEnergyService` | `IEnergyGrid` |
| Storage ME | chave `AEItemKey` | canal `IItemStorageChannel` + `IAEItemStack` |
| Upgrade | `IUpgradeInventory` | `UpgradeInventory` + `Upgrades.SPEED` |
| Menu | `MenuTypeBuilder` | Forge `IForgeContainerType` + `NetworkHooks` |
| Registro | registro moderno 1.18 | `DeferredRegister` do Forge 1.16.5 |

A lógica de receita usa diretamente `InscriberRecipes.findRecipe` e `InscriberRecipe` do AE2 8.4.7. A saída para a rede usa `IMEInventory.injectItems`, e a alimentação usa `IEnergyGrid.extractAEPower`, ambos existentes nessa versão. Nenhuma classe compilada do JAR 1.18.2 foi incorporada.

## Decisões de compatibilidade

- O inventário externo combinado foi preservado em todas as faces, reproduzindo o comportamento do Advanced Inscriber 1.0.7, em vez das restrições laterais do Inscriber comum.
- A saída retida é reavaliada enquanto a máquina permanece conectada, permitindo enviá-la quando houver espaço na rede.
- O progresso foi persistido no NBT. Isso evita perder trabalho parcial após recarregar o mundo e não modifica o AE2.
- A janela é registrada pelo próprio mod e usa os componentes visuais públicos do AE2 8.4.7.
- Não existe código, registro ou recurso para DISKs ou Crystal Growth Chamber.

## Licenças

O AE2 Things 1.0.7 está sob MIT e permite reutilização e modificação com preservação do aviso de copyright e da licença. O AE2 8.4.7 está sob LGPL v3-or-later. Como a implementação adapta lógica das duas bases e vincula-se ao AE2, o projeto é distribuído sob LGPL-3.0-or-later, acompanhado da licença MIT do material reutilizado.
