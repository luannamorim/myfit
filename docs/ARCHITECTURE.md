# MyFit — Arquitetura

## Visão geral

App Android nativo, um módulo (`:app`), MVVM, offline. Room é a fonte da verdade; nada sai do aparelho além dos arquivos de backup que o usuário manda gravar numa pasta.

```
ui/        Compose: telas + ViewModels (Hilt), design system, navegação
  │  observa StateFlow, chama ações
domain/    Kotlin puro: motor da sessão, cálculos de histórico/medidas, formato de backup
  ▲  usado por
data/      Room, DataStore, ImageStore, repositórios, BackupManager, destino SAF
platform/  foreground service, alarme, receivers, notificações, WorkManager
```

Regra de dependência: `domain` não conhece Android, exceto a lib Tink. `ui` não fala com Room diretamente. `platform` orquestra `data` e `domain`.

| Pacote | Conteúdo |
|---|---|
| `core/time` | `Mark` (relógio real + monotônico + contador de boots), `TimeSource` |
| `core/format` | números PT-BR (`DecimalInput`), durações, datas |
| `domain/session` | `SessionEngine`, `ActiveSession` |
| `domain/history` | `HistoryCalculator` (resumos, semanas seg–dom) |
| `domain/body` | `Measure`, `BodyComparison` (diferenças, séries, snapshot) |
| `domain/backup` | DTOs v1, cabeçalho e criptografia, zip + manifest, retenção |
| `data/db` | entidades, DAOs, `MyFitDatabase` (v1), seed da biblioteca |
| `data/repository` | treinos, exercícios, sessão, histórico, corpo |
| `data/backup` | `BackupManager`, `BackupKeyStore`, destinos (SAF/arquivo), `FolderBackupWriter` |
| `platform/session` | `SessionController`, `WorkoutService`, alarme, receivers, notificações |
| `platform/backup` | `BackupScheduler`, workers, notificação de falha, limpeza de imagens |

## Dados

```
exercise ─┐                       workout_session ── session_exercise ── session_set
          └── routine_exercise ── routine        (snapshots: nome, grupo, imagem, planejado/realizado)
body_assessment   body_photo
```

- `routine_exercise.exerciseId` usa RESTRICT. Exercício em uso é arquivado em vez de excluído.
- `workout_session.routineId` usa SET NULL, e as tabelas da sessão guardam cópias (nome do treino, nome/grupo/imagem do exercício, metas). Editar ou excluir um treino nunca altera o histórico.
- `startDateEpochDay` é o dia local em que a sessão começou: um treino que atravessa a meia-noite conta no dia de início.
- Imagens ficam em `filesDir/images/{exercises,body}/<uuid>.jpg`, reduzidas a 1600 px e sem EXIF. O banco guarda só o caminho relativo. Caminhos que escapariam de `images/` não resolvem para nenhum arquivo.
- Schema exportado em `app/schemas`. Qualquer mudança exige migração testada, porque já existe APK instalado.

## Sessão de treino

**Motor puro** (`SessionEngine`): cada ação (`completeSet`, `skipSet`, `skipExercise`, `goToExercise`, `adjustRest`, `pause`, `resume`…) devolve uma nova `ActiveSession`. Regras principais:

- O cursor avança para a próxima série pendente do exercício, depois para os exercícios seguintes e, por fim, volta aos anteriores que ficaram pendentes.
- O descanso começa depois de concluir uma série, exceto na última série do treino. Pular não gera descanso.
- A carga usada numa série é levada para as séries pendentes do mesmo exercício.
- Pausar congela o tempo total e o descanso. Ao retomar, o descanso recomeça com o tempo que faltava.

**Tempo** (`Mark`): cada marca guarda `wallMs`, `elapsedMs` e `bootCount`. No mesmo boot vale o relógio monotônico, imune a ajuste de hora; depois de um reboot vale o relógio real. O resultado nunca é negativo. A UI só redesenha a partir das marcas: um ticker de 250 ms no ViewModel roda apenas enquanto a tela coleta o estado.

**Persistência**: o `SessionController` serializa as ações com um mutex. Cada ação roda o motor, grava a sessão inteira numa transação, reagenda ou cancela o alarme e atualiza as notificações.

**Segundo plano**:
- **Notificação contínua**: um `WorkoutService` em foreground, do tipo `health` (o requisito do API 34+ é atendido por `HIGH_SAMPLING_RATE_SENSORS`, permissão normal), mostra o cronômetro do sistema em contagem regressiva no descanso e progressiva no resto do treino, com as ações "Concluir série" e "Pular descanso". Se o usuário arrastar a notificação, ela volta.
- **Alarme de fim do descanso**: usa `AlarmManager.setAlarmClock`, o único alarme exato imune ao Doze e às otimizações da One UI. O `RestEndHandler` ignora alarmes velhos e posta a notificação com som. Há dois canais: um segue o modo do celular e o outro usa áudio de alarme e toca sempre.
- **Recuperação**: se o processo morrer, a sessão continua no banco. Ao voltar ao primeiro plano, o app chama `resync()`, que rearma o alarme e o service, e mostra a barra "Retomar".

## Backup

**Arquivo** (`.myfit`):

```
"MYFITBAK" | u16 versão | u8 kdf | u32 iterações | 16 B salt | 16 B verificador | u64 criadoEm
AES-256-GCM-HKDF em streaming (Tink, segmentos de 1 MiB, cabeçalho inteiro como dado associado)
  └─ zip: manifest.json (formatVersion, app, contagens, SHA-256 por entrada)
          data.json (BackupDataV1, independente do schema do Room)
          settings.json
          images/<tipo>/<uuid>.jpg (sem recompressão)
```

**Chave**:
- **Derivação**: a chave sai de PBKDF2-HMAC-SHA256 (600k iterações, senha normalizada em NFKC) aplicado sobre a senha de recuperação. O verificador é um HKDF da chave, e não a própria chave, e permite distinguir "senha errada" de "arquivo corrompido".
- **Armazenamento**: a chave derivada fica guardada embrulhada por uma chave AES do Android Keystore, sem exigir desbloqueio, para o worker rodar com a tela bloqueada. Se essa chave sumir (reinstalação ou dados limpos), o estado vira LOCKED e o app pede a senha de novo.
- **Arquivos de terceiros**: parâmetros fracos de um arquivo importado nunca são adotados.

**Restauração** (`BackupManager.restore`):
1. Recusa se houver um treino em andamento. Lê o cabeçalho, obtém a chave (a do aparelho ou a da senha digitada) e confere o verificador.
2. Decripta tudo para o cache, porque truncamento e adulteração só aparecem no fim do stream. Valida nomes (sem zip-slip), limites, versão do formato, hashes, contagens e caminhos de imagem.
3. Copia as imagens novas para `images/` antes do commit. Os nomes são UUIDs, então as antigas continuam lá.
4. Substitui o banco numa única transação, importa as configurações e só então apaga as imagens que nenhuma linha referencia.
5. Revalida contagens e imagens.

Se o app morrer em qualquer ponto, o banco fica inteiro (o antigo ou o novo) e com todas as suas imagens. Na inicialização, `recoverInterruptedRestore` limpa as sobras.

**Pasta e agendamento**:
- **Destino**: `SafBackupDestination` grava, via SAF, na pasta escolhida (permissão persistente). Escreve `<nome>.partial`, renomeia e relê o cabeçalho; só então apaga os excedentes, mantendo 7 e nunca o arquivo recém-gravado. Arquivos que não são do MyFit são ignorados.
- **Sincronização**: fica por conta de um app de sincronização com o Drive. O MyFit não fala com a rede.
- **Quando roda**: o `BackupScheduler` agenda uma execução diária (bateria e armazenamento não baixos), um debounce de 10 min após mudanças importantes e o "Fazer backup agora".
- **Contador de alterações**: `changeCount` sobe a cada mudança e `savedChangeCount` registra o que o último backup cobriu. Uma mudança feita durante um backup continua pendente.
- **Falhas**: viram estado (pasta indisponível, sem senha, sem espaço, desconhecida) e uma notificação.

## Navegação e UI

- Navigation Compose com rotas type-safe, num `NavigationSuiteScaffold`: barra inferior no celular e rail em telas largas, escondido nas telas cheias. O conteúdo fica limitado a 640 dp.
- A execução do treino é imersiva, sempre no tema escuro, com tela ligada.
- O tema vem das configurações (escuro por padrão). Tokens de cor, tipografia (Geist com algarismos tabulares) e componentes estão em `docs/BRAND.md`.

## Testes

| Camada | Ferramenta | Exemplos |
|---|---|---|
| Domínio puro | JUnit | motor da sessão (17 casos), `Mark`, resumos, comparação corporal, cripto, zip, retenção |
| Dados | Robolectric + Room em memória | DAOs, cascata e restrições, repositórios, snapshot ida e volta, backup criar/restaurar/corromper |
| Plataforma | Robolectric shadows | alarme agendado/cancelado/reagendado, notificações, service, workers (`work-testing`) |
| UI | Compose test + Robolectric | fluxo completo de treino, navegação, recuperação de sessão |
| Visual | Roborazzi | screenshots de todas as telas em tema escuro e claro, fonte 1,5× e tablet |

```bash
./gradlew testDebugUnitTest          # todos os testes
./gradlew recordRoborazziDebug       # gera PNGs em app/build/outputs/roborazzi/
./gradlew lintDebug
```

## Decisões e trade-offs

- **Pasta em vez da API do Drive**: pedido do usuário. Elimina OAuth e Google Cloud. A sincronização fica com outro app, e o seletor do Drive oficial não oferece acesso a uma pasta inteira.
- **`setAlarmClock`**: confiabilidade acima de tudo no descanso. O efeito colateral é o ícone de alarme na barra de status enquanto o descanso corre.
- **Um módulo**: o app é pessoal e pequeno, então as fronteiras ficam nos pacotes e na regra de dependência.
- **Navigation 2 e Room 2.8**: estáveis e bem documentados. Navigation 3 e Room 3 são recentes e não trariam ganho aqui.
- **Sem dispositivo no desenvolvimento**: a validação de plataforma é feita com shadows do Robolectric mais o roteiro manual de `docs/TESTING.md`.
