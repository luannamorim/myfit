# MyFit — Especificação

> App Android pessoal e offline para montar treinos, executá-los com cronômetro confiável e acompanhar a evolução do corpo.

## Problema e por que importa

Quem treina musculação com regularidade precisa de três coisas ao mesmo tempo: saber o que fazer (sequência, séries, repetições, carga, descanso), executar sem atrito no meio do treino (marcar a série, descansar o tempo certo, ajustar a carga) e enxergar a evolução ao longo de meses (frequência, cargas, medidas, fotos). Hoje isso fica espalhado entre bloco de notas, cronômetro do celular, planilhas e a galeria de fotos. O resultado é histórico perdido, descanso no "olhômetro" e nenhuma visão de progresso.

Os apps de fitness do mercado resolvem parte disso, mas exigem conta, mandam os dados para servidores de terceiros, empurram assinatura e gamificação, e têm interfaces carregadas. Para uso pessoal diário, o custo é atrito em cada treino e dados que não são realmente do usuário.

O MyFit é um app de um usuário só: rápido durante o treino, 100% local, com backup criptografado sob controle do próprio usuário.

## Objetivos

- Executar um treino completo (séries, descansos, ajustes) sem tirar o foco do exercício, com cronômetro correto mesmo com tela bloqueada ou o app em segundo plano.
- Nunca perder uma sessão em andamento: fechar o app, matar o processo ou reiniciar o celular não apaga o progresso do treino.
- Guardar o histórico completo de treinos e medidas, consultável por calendário, resumos e gráficos.
- Proteger os dados contra perda do aparelho com backup automático e criptografado, sem backend próprio.
- Ter identidade visual própria, minimalista e escura, que não pareça template de app fitness.

## Não objetivos

- Não é rede social: sem compartilhamento, ranking, amigos ou feed.
- Sem gamificação: nada de medalhas, pontos ou sequências com pressão para não "quebrar".
- Sem conta, login ou servidor: os dados vivem no aparelho e nos arquivos de backup do usuário.
- Não é coach: não gera treinos, não prescreve cargas nem dá recomendações de saúde ou nutrição.
- Não integra com wearables, Health Connect ou sensores de frequência cardíaca.

## Critérios de sucesso

| Critério | Medição | Meta |
|---|---|---|
| Precisão do descanso | Desvio entre o fim real do descanso e o alerta, com tela bloqueada | ≤ 2 s |
| Recuperação de sessão | Treino retomado no mesmo exercício/série após matar o processo ou reiniciar o celular | 100% dos casos do roteiro de teste |
| Atrito na execução | Toques para concluir uma série e iniciar o descanso | 1 toque |
| Abertura do app | Tempo até o Início ficar interativo (cold start, celular intermediário) | < 1,5 s |
| Integridade do backup | Restaurar um backup recria banco e imagens idênticos (contagens e hashes) | 100% nos testes de ida e volta |
| Detecção de corrupção | Arquivo adulterado ou senha errada rejeitados antes de tocar nos dados locais | 100% nos testes |
| Tamanho do APK | APK de release minificado | < 15 MB |
| Critérios de aceite do usuário | Roteiro manual em `docs/TESTING.md` no celular do usuário | Todos os itens passam |

## Usuário e casos de uso

**Usuário:** uma pessoa (o dono do app) que treina musculação de 3 a 6 vezes por semana com treinos divididos (ex.: Costas, Peito, Pernas), usa um Samsung com Android recente e quer o histórico sob controle próprio.

**Casos de uso principais:**

1. "Chego na academia, abro o app, toco em Iniciar no treino de Costas e vou marcando as séries; o descanso conta sozinho e o celular vibra/toca quando acaba, mesmo bloqueado no banco."
2. "No meio do treino a máquina está ocupada: pulo o exercício, faço o próximo e volto depois; subo a carga de 40 para 42,5 kg numa série."
3. "O app fechou (ou o celular reiniciou) no meio do treino: reabro e continuo de onde parei, com o tempo total certo."
4. "No fim do mês, olho no calendário quantas vezes treinei, quanto tempo no total e quais exercícios fiz."
5. "A cada 4 semanas registro peso e medidas, comparo com a avaliação inicial e vejo o gráfico do braço e da cintura subindo e descendo."
6. "Troquei de celular: instalo o app, aponto para a pasta de backup sincronizada com o Drive, digito a senha e está tudo de volta, com as fotos."

## Requisitos funcionais

### Início (dashboard)

1. DEVE mostrar saudação e data atual.
2. DEVE mostrar, com prioridade máxima, um treino em andamento com ação "Retomar".
3. DEVE mostrar o treino realizado hoje, se houver.
4. DEVE oferecer atalho para iniciar um treino, sugerindo o treino feito há mais tempo.
5. DEVE listar os treinos disponíveis, o resumo da última atividade, os indicadores corporais mais recentes (com variação) e o resumo da semana.

### Treinos e exercícios

6. DEVE permitir criar, editar, duplicar e excluir treinos (nome, descrição opcional, lista ordenada de exercícios), sem limite de quantidade.
7. DEVE permitir adicionar, remover e reordenar exercícios num treino.
8. Cada exercício no treino DEVE ter: séries, repetições por série, descanso entre séries (s), carga (kg, opcional) e observações.
9. DEVE oferecer uma biblioteca de exercícios pré-definidos (nome, grupo muscular, tipo de movimento push/pull/legs/outros) com busca e filtro por grupo.
10. DEVE permitir cadastrar exercícios personalizados com imagem demonstrativa opcional escolhida da galeria.

### Execução do treino

11. DEVE iniciar uma sessão a partir de um treino e mostrar: nome do treino, exercício atual, imagem (se houver), série atual/total, repetições planejadas, carga, cronômetro de descanso e tempo total.
12. DEVE seguir o fluxo: concluir série (1 toque) → descanso automático → alerta visual e sonoro ao fim → próxima série → próximo exercício → finalizar.
13. DEVE permitir pausar/retomar, pular série, pular exercício, ajustar carga e repetições da série, alterar o descanso (inclusive ±15 s durante o descanso) e encerrar antes.
14. DEVE manter os cronômetros corretos com o app em segundo plano, a tela bloqueada, o processo encerrado pelo sistema ou após reinício do aparelho. O tempo DEVE ser derivado de marcas temporais persistidas, nunca de contadores na interface.
15. DEVE alertar o fim do descanso por notificação com som e vibração mesmo com o app fechado, e mostrar uma notificação contínua do treino com o cronômetro e ações rápidas.
16. DEVE persistir cada passo da sessão imediatamente e oferecer a retomada exata ao reabrir o app.
17. Ao finalizar, DEVE salvar automaticamente: data, horário de início e fim, duração (sem pausas), treino, exercícios, séries concluídas/puladas, repetições e cargas realizadas.
18. DEVERIA oferecer, ao finalizar, atualizar as cargas do treino com as usadas na sessão.

### Histórico e calendário

19. DEVE mostrar um calendário mensal com os dias treinados destacados.
20. Ao tocar num dia, DEVE listar as sessões com duração e permitir abrir o detalhe (exercícios, séries, repetições, cargas).
21. DEVE mostrar resumos semanal e mensal: quantidade de treinos, tempo total, frequência semanal média e exercícios realizados.
22. O histórico DEVE se manter íntegro mesmo que o treino ou o exercício de origem sejam editados ou excluídos depois.

### Evolução corporal

23. DEVE permitir criar, editar e excluir avaliações com data, peso (kg), % de gordura (opcional) e 14 circunferências em cm (pescoço, ombros, peitoral, cintura, abdômen, quadril, braços D/E, antebraços D/E, coxas D/E, panturrilhas D/E), todas opcionais exceto a data.
24. DEVE listar o histórico de avaliações.
25. DEVE comparar duas avaliações escolhidas, mostrando valores e diferença absoluta com sinal por indicador.
26. DEVE mostrar gráficos de evolução no tempo para cada indicador, com seleção dos indicadores exibidos.
27. DEVE permitir registrar fotos corporais por data (frente/lado/costas), navegar por data e comparar duas fotos lado a lado.

### Backup e dados

28. DEVE gerar backup único e compactado com todos os dados (treinos, biblioteca personalizada, sessões completas, medidas, fotos, imagens de exercícios, configurações), incluindo versão do formato e metadados.
29. DEVE criptografar todo backup com uma senha de recuperação definida pelo usuário.
30. DEVE gravar backups numa pasta escolhida pelo usuário no aparelho (que ele pode sincronizar com o Google Drive por conta própria), mantendo os 7 mais recentes.
31. DEVE fazer backup automático diário e após alterações importantes, com intervalo mínimo para evitar gravações excessivas, sem atrapalhar o uso offline.
32. DEVE permitir backup manual e mostrar data/hora do último backup, status e o último erro de forma clara.
33. DEVE permitir restaurar um backup da pasta ou de um arquivo avulso, mostrando a data do backup e pedindo confirmação antes de substituir os dados locais.
34. DEVE validar a integridade (senha, autenticidade, hashes, versão do formato) antes de restaurar e de novo depois, sem alterar os dados locais se a validação falhar.

### Configurações

35. DEVE permitir escolher tema (Escuro, padrão; Claro; Sistema), descanso padrão, comportamento do som do descanso e manter a tela ligada durante o treino.
36. DEVE reunir gerenciamento de backup (pasta, senha, automático, manual, restaurar) e da biblioteca de exercícios.
37. DEVERIA orientar a remover a otimização de bateria do app (relevante em aparelhos Samsung).

## Requisitos não funcionais

- **Offline-first:** toda funcionalidade opera sem internet; não há chamada de rede no app.
- **Confiabilidade:** dados preservados após reinício do aparelho; banco com migrações testadas a partir da primeira versão instalada; restauração atômica (ou tudo ou nada).
- **Privacidade e segurança:** sem coleta de dados, sem analytics, sem permissões de mídia (seletor de fotos do sistema); backups criptografados com autenticação (AES-256-GCM em streaming, chave derivada da senha); backup automático do Android desligado para não copiar chaves que não funcionam em outro aparelho.
- **Desempenho:** interface a 60 fps na execução do treino; consultas do histórico e gráficos fluidos com 5 anos de dados (~1.500 sessões, ~60 avaliações).
- **Experiência:** tema escuro priorizado e tema claro completo; layouts adaptados a celulares de tamanhos diferentes e telas largas; animações discretas (150–250 ms); alvos de toque grandes na execução; suporte a fonte grande do sistema.
- **Compatibilidade:** Android 8.0 (API 26) ou superior; alvo API 37.
- **Idioma:** interface em PT-BR, unidades métricas (kg, cm).

## Identidade visual

- Marca MyFit com símbolo e logotipo próprios, ícone adaptativo e versão monocromática.
- Predominância de preto, branco e cinzas; verde como única cor de destaque, reservada para progresso e ações positivas.
- Referências estéticas: Linear, Apple Fitness, Whoop. Evitar aparência de template de app fitness.
- Sistema de componentes consistente em todas as telas, documentado em `docs/BRAND.md`.

## Visão geral da arquitetura

App Android nativo (Kotlin, Jetpack Compose, Material 3), MVVM com camadas de UI, domínio e dados:

```
ui/       telas Compose + ViewModels por feature; design system compartilhado
domain/   modelos e regras puras (motor da sessão de treino, resumos, comparações, formato de backup)
data/     Room (fonte da verdade), DataStore (configurações), armazenamento de imagens, pasta de backup (SAF)
```

Componentes de plataforma: foreground service da sessão de treino, alarme exato para o fim do descanso, WorkManager para backups. Detalhes em `docs/ARCHITECTURE.md`.

## Restrições e premissas

- Distribuição por APK assinado instalado manualmente (sem Play Store). Premissa: o usuário consegue instalar apps fora da loja no seu aparelho, seja pelo fluxo de desenvolvedor não verificado, seja via ADB.
- Sem backend, autenticação ou serviços pagos.
- A sincronização da pasta de backup com a nuvem é responsabilidade do usuário (app de sincronização de sua escolha). Premissa: o provedor de documentos da pasta permite criar, listar e apagar arquivos.
- Desenvolvimento e validação sem dispositivo Android: testes na JVM (unitários, Robolectric, screenshots). Comportamentos de plataforma (alarmes, service, som, pasta real) são validados pelo usuário com roteiro manual.
- A perda simultânea do aparelho e da senha de recuperação torna os backups irrecuperáveis, por design.

## Fora do escopo (v1)

- Sincronização em tempo real entre aparelhos.
- Integração direta com a API do Google Drive (substituída pela pasta escolhida pelo usuário).
- Exportação para CSV/planilha.
- Supersets, drop-sets e séries com metas diferentes por série no planejamento (o realizado é registrado por série).
- Cálculo automático de 1RM, volume por grupo muscular e recordes pessoais.
- Widgets de tela inicial, Wear OS e Live Updates do Android 16.
- Imagens demonstrativas na biblioteca pré-definida (só exercícios com imagem escolhida pelo usuário).
- Outros idiomas e unidades imperiais.

## Questões em aberto

1. **Instalação fora da Play Store no Brasil:** a exigência de desenvolvedor verificado estava prevista para 30/09/2026. Confirmar antes do release se está vigente e documentar o caminho (fluxo avançado, ADB ou conta de distribuição limitada).
2. **Provedores de documentos:** apps de sincronização expõem pastas com comportamentos diferentes (renomear, listar). O fallback é gravar direto no nome final; validar no aparelho do usuário.
3. **Atualizar cargas do treino ao finalizar:** padrão marcado (opção visível na tela de resumo). Revisar depois de algumas semanas de uso.
4. **Sugestão de próximo treino:** "o feito há mais tempo" é a regra inicial; pode virar ordem fixa de rotação se não refletir a divisão do usuário.
