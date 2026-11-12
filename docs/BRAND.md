# MyFit — Identidade visual

## Conceito

O MyFit existe para mostrar evolução. O símbolo é um **"M" traçado como uma única linha de progresso**. A linha sobe, recua e volta a subir acima do primeiro pico, e termina num **ponto verde destacado**, como o dado mais recente de um gráfico. Ele funciona como inicial e como gráfico ao mesmo tempo, sem recorrer a halteres ou silhuetas.

A interface segue o mesmo princípio: neutros grafite, tipografia firme e **uma única cor de destaque** usada só onde existe progresso ou ação positiva.

## Símbolo e wordmark

- Grade de 24 unidades. Linha `M3,19 L8,9 L12,14.5 L16.74,8.18`, traço 2,6 com pontas e junções arredondadas.
- Ponto: centro (19,5; 4,5), raio 2,3, separado da linha por um respiro de 1 unidade.
- Fonte única da geometria: `ui/components/MyFitMark.kt` (`MarkGeometry`). Os vetores `ic_launcher_foreground`, `ic_launcher_monochrome` e `ic_stat_myfit` (ícone das notificações) usam os mesmos números.
- Wordmark: `myfit` em minúsculas, Geist SemiBold, espaçamento −0,04 em, à direita do símbolo.
- Ícone do app: fundo `#0B0B0C`, linha `#F5F5F4`, ponto `#47F55E`; versão monocromática para ícones temáticos (Android 13+).
- Tamanho mínimo do símbolo: 16 dp.

## Cores

| Token | Escuro | Claro | Uso |
|---|---|---|---|
| `background` | `#0B0B0C` | `#FAFAF9` | fundo das telas |
| `surface` | `#141416` | `#FFFFFF` | cards |
| `surfaceRaised` | `#1C1C1F` | `#F2F2F0` | controles, diálogos, indicador da navegação |
| `outline` | `#2A2A2E` | `#E2E2DF` | bordas finas (1 dp) |
| `outlineStrong` | `#3A3A40` | `#CBCBC7` | bordas de botões secundários |
| `textPrimary` | `#F5F5F4` | `#0B0B0C` | texto principal |
| `textSecondary` | `#A1A1A8` | `#5A5A62` | rótulos, metadados |
| `textTertiary` | `#6B6B73` | `#8E8E95` | desativado, ícones inativos |
| `accent` | `#47F55E` | `#15803D` | ação primária, progresso, séries concluídas |
| `onAccent` | `#0B0B0C` | `#FFFFFF` | texto sobre o verde |
| `danger` | `#F2555A` | `#CD2B31` | ações destrutivas, erros |
| `warning` | `#F5A524` | `#B45309` | avisos |

Contraste garantido por `ColorContrastTest`: texto principal ≥ 7:1, secundário ≥ 4,5:1, texto sobre o verde ≥ 4,5:1, verde sobre fundo ≥ 3:1.

**Regra do verde:** só para progresso (anel de descanso, série concluída, variação positiva de carga), ação primária da tela e links de ação. Diferenças de medidas corporais usam cor neutra, porque o app não julga se subir ou descer é bom.

## Tipografia

Geist (SIL OFL 1.1, embutida em `res/font`, licença em `docs/licenses/GEIST_OFL.txt`), pesos Regular, Medium, SemiBold e Bold.

| Estilo | Tamanho / peso | Uso |
|---|---|---|
| `display` | 32 SemiBold, −0,03 em | título das abas |
| `headline` | 24 SemiBold | títulos de tela cheia |
| `title` | 17 SemiBold | títulos de cards e listas |
| `body` / `bodyStrong` | 15 Regular / Medium | texto corrido, botões |
| `label` | 13 Medium | rótulos, chips |
| `caption` | 12 Regular | metadados |
| `overline` | 11 SemiBold, maiúsculas, +0,1 em | cabeçalhos de seção |
| `numericHero` | 72 Medium, algarismos tabulares | cronômetro de descanso |
| `numericLarge` | 30 SemiBold, algarismos tabulares | valores de destaque, steppers |
| `numeric` | 15 Medium, algarismos tabulares | números em linha |

Números sempre com algarismos tabulares (`tnum`), para não "pular" enquanto o cronômetro corre. A Geist Mono foi testada e descartada: vírgula decimal larga demais e zero cortado.

## Forma e espaço

- Grade de 4 dp; margem lateral das telas 20 dp; conteúdo limitado a 640 dp em telas largas.
- Cantos: 10 dp (pequeno), 16 dp (cards), 24 dp (diálogos); botões e chips em pílula.
- Cards planos: borda de 1 dp, sem sombra.
- Alvos de toque: mínimo 48 dp, e 56 dp nos controles de treino (steppers).

## Movimento

150–250 ms, easing padrão, sem bounce. Animar só o que comunica estado: progresso do descanso, entrada e saída de conteúdo, seleção.

## Componentes

`ui/components`: `MyFitMark`, `MyFitWordmark`, `MyFitCard`, `PrimaryButton`, `SecondaryButton`, `TextAction`, `StatTile`, `NumberStepper`, `RestRing`, `SectionHeader`, `EmptyState`, `FilterChipRow` / `MyFitChip`, `ConfirmDialog`, `MyFitTopBar`, `TabHeader` / `TabScreen`.
Catálogo visual: `ComponentCatalogScreenshotTest` (os PNGs saem em `app/build/outputs/roborazzi/`).

## Fazer / evitar

- Fazer: muito espaço negativo, uma ação primária por tela, números grandes e legíveis à distância.
- Evitar: gradientes, sombras, ilustrações de pessoas ou halteres, mais de uma cor de destaque, ícones coloridos.
